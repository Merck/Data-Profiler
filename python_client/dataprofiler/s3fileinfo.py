"""
  Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
 
 	Licensed to the Apache Software Foundation (ASF) under one
 	or more contributor license agreements. See the NOTICE file
 	distributed with this work for additional information
 	regarding copyright ownership. The ASF licenses this file
 	to you under the Apache License, Version 2.0 (the
 	"License"); you may not use this file except in compliance
 	with the License. You may obtain a copy of the License at
 
 	http://www.apache.org/licenses/LICENSE-2.0
 
 
 	Unless required by applicable law or agreed to in writing,
 	software distributed under the License is distributed on an
 	"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 	KIND, either express or implied. See the License for the
 	specific language governing permissions and limitations
 	under the License.
"""
import boto3
import os
import chardet
import csv
from logging import getLogger, DEBUG as LOGGING_DEBUG

logger = getLogger('dpc-s3fileinfo')
logger.setLevel(LOGGING_DEBUG)


def key_contains_files(bucket_name: str, key: str) -> bool:
    client = boto3.client('s3')
    response = client.list_objects_v2(
            Bucket=bucket_name, 
            Prefix=key)

    size = 0
    if response['KeyCount'] > 0:
        for o in response['Contents']:
            size += o['Size']

    if size > 0:
        return True
    else:
        return False


def split_bucket_and_key(path):
    directory = path[-1] == "/"
    splits = path.split("/")
    while splits[0] == '' or splits[0].startswith("s3"):
        splits = splits[1:]
    assert len(splits) >= 1
    bucket_name = splits[0]
    prefix = None
    if len(splits) > 1:
        prefix = os.path.normpath("/".join(splits[1:]))
        if directory:
            prefix = prefix + "/"

    return bucket_name, prefix


class S3FileInfo:
    WHITELIST = [",", "\t", "|", ";", ":"]
    SCHEMA_SUFFIX = "-schema"
    CSV = 1
    ORC = 2
    PARQUET = 3
    AVRO = 4

    def __init__(self, s3_path):
        self.s3 = boto3.resource('s3')
        self.bucket_name, self.key = split_bucket_and_key(s3_path)
        self.object = self.s3.Object(self.bucket_name, self.key)
        self.fname = s3_path
        self.size = self.object.content_length
        self.format = self.CSV
        # Use RFC 4180 defaults
        # https://tools.ietf.org/html/rfc4180#page-2
        self.delimiter = ","
        self.escape = '"'
        self.quote = '"'
        self.charset = "utf-8"
        self.schema_fname = None
        self.dialect = None
        self.quote_params = False

    def __repr__(self):
        return "<DataSetFileInfo: {}({})>".format(self.fname, self.size)

    def is_orc(self, data):
        return data[:3] == b"ORC"

    def is_parquet(self, data):
        return data[:4] == b"PAR1"

    def is_avro(self, data):
        return data[:3] == b"Obj"

    def sniff_format(self):
        data = self.object.get(Bucket=self.bucket_name, Key=self.key, Range='bytes{}-{}'.format(0, 2048))['Body'].read()
        # Check for an ORC formation
        if self.is_orc(data):
            self.format = self.ORC
            logger.debug('Found ORC format')
            return
        elif self.is_parquet(data):
            self.format = self.PARQUET
            logger.debug('Found Parquet format')
            return
        elif self.is_avro(data):
            self.format = self.AVRO
            logger.debug('Found Avro format')
            return

        self.charset = chardet.detect(data)["encoding"]

        # If chardet fails to determine encoding, default to UTF-8
        if self.charset is None:
            self.charset = "utf-8"

        data = data.decode(self.charset)

        self.sniff_csv_format_from_data(self.fname, data)

    def __ext_to_delim(self, fname):
        ext_to_delim = {".tsv": "\t", ".csv": ","}
        ext = os.path.splitext(fname)[1]
        if ext in ext_to_delim.keys():
            return ext_to_delim[ext]
        else:
            return None

    def sniff_csv_format_from_data(self, fname, data):
        # Throw away the last line assuming it might have been truncated
        data = "\n".join(data.splitlines()[:-1])

        exc = None
        try:
            self.dialect = csv.Sniffer().sniff(data)
        except Exception as e:
            exc = e

        if exc or self.dialect.delimiter not in self.WHITELIST:
            # Just try the header line - sometimes files have really odd data that confuses things
            try:
                data = data.splitlines()[0]
                self.dialect = csv.Sniffer().sniff(data)
                exc = None
            except Exception as e:
                exc = e

        if exc or self.dialect.delimiter not in self.WHITELIST:
            # For some files the sniffer gets confused but limiting the choices helps. So let's guess
            # based on the file extensions
            delim = self.__ext_to_delim(fname)
            if delim is not None:
                try:
                    self.dialect = csv.Sniffer().sniff(data, [delim])
                    logger.debug(self.dialect.delimiter)
                    exc = None
                except Exception as e:
                    exc = e

        if exc or self.dialect.delimiter not in self.WHITELIST:
            # Single column files are a real pain in the you know what - so try to figure out
            # if that is why we are failing and just mark them based on their file extension.
            data = data.splitlines()[0]
            escape = False
            single_column = True
            for c in data:
                if c in self.WHITELIST and not escape:
                    # This is where it doesn't look like single column so just bail and let
                    # things fail
                    single_column = False
                    break
                # Figure out if we are potentially escaping the next char. Yes - we are
                # only supporting \ as escape, but by this point we are so screwed we need
                # to stop being fancy and try and be safe.
                if c == "\\":
                    escape = True
                else:
                    escape = False

            if single_column:
                self.dialect = csv.unix_dialect
                delim = self.__ext_to_delim(fname)
                if delim is not None:
                    self.dialect.delimiter = delim
                exc = None

        if exc or self.dialect.delimiter not in self.WHITELIST:
            logger.error(f'Failed to determine csv format for {fname}')
            logger.debug(data)
            if exc:
                raise exc
            else:
                raise Exception(
                    "Failed to detect delim - found " + self.dialect.delimiter
                )

        logger.debug(f'Detected delimter: {repr(self.dialect.delimiter)}')
        logger.debug(f'Detected escape: {repr(self.dialect.escapechar)}')
        logger.debug(f'Detected quote: {repr(self.dialect.quotechar)}')

        self.delimiter = self.__quote_and_default(
            self.dialect.delimiter, self.delimiter
        )
        self.escape = self.__quote_and_default(self.dialect.escapechar, self.escape)
        self.quote = self.__quote_and_default(self.dialect.quotechar, self.quote)

    def __quote_and_default(self, char, default):
        if char is None:
            char = default
        if self.quote_params:
            return shlex.quote(char)
        else:
            return char

    def get_dataloader_class(self):
        if self.format == self.ORC:
            return "com.dataprofiler.loader.OrcDataLoader"
        elif self.format == self.PARQUET:
            return "com.dataprofiler.loader.ParquetDataLoader"
        elif self.format == self.AVRO:
            return "com.dataprofiler.loader.AvroDataLoader"
        else:
            return "com.dataprofiler.loader.CsvDataLoader"

    def get_file_params(self):
        if self.format != self.CSV:
            p = [self.fname]
        else:
            p = [self.delimiter, self.escape, self.quote, self.charset, self.fname]

        if self.schema_fname is not None:
            return p + [self.schema_fname]
        else:
            return p

if __name__ == '__main__':
    url = 's3://dataprofiler-modern-development/test/small/small_table.csv'
    d = S3FileInfo(url)
    # d.sniff_format()
    print(d.format)
    print(key_contains_files('dataprofiler-modern-development', 'test/small/'))