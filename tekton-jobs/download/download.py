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
import os
from datetime import datetime
from boto3 import client as aws_client
from shutil import which as shwhich
from argparse import ArgumentParser
from logging import getLogger, DEBUG as LOGGING_DEBUG
from tempfile import NamedTemporaryFile, TemporaryDirectory
from subprocess import CalledProcessError, call as sub_call

from dataprofiler.config import Config
from dataprofiler.java import run_spark_submit_command
from dataprofiler.logging_helper import setup_logger_with_job_id
from dataprofiler.job_api import JobType, JobStatus, Job, JobApi

logger = getLogger('download')
logger.setLevel(LOGGING_DEBUG)


S3_EXPORT_KEY = 'exports'
S3_URL_EXPIRATION = 14 * 24 * 60 * 60  # 14 days in seconds


def is_valid_config(conf: Config) -> bool:
    if not conf:
        return False
    return True


def files_in_dir(dirname):
    return [os.path.join(dirname, f) for f in os.listdir(dirname) if
            os.path.isfile(os.path.join(dirname, f))]


def execute(conf: Config, job: Job) -> bool:
    if job.type != JobType.DOWNLOAD:
        msg = 'Tried to execute job as a download job, but it was not a download job'
        logger.error(msg)
        job.set_status(JobStatus.ERROR)
        job.set_status_details(msg)
        return False

    with NamedTemporaryFile(delete=False) as fd:
        logger.info('Writing out download spec...')
        logger.debug(job.details.to_json())
        fd.write(job.details.to_json().encode())
        fd.flush()
        with TemporaryDirectory(dir='/tmp') as working_dir:
            try:
                run_spark_submit_command(c, 'com.dataprofiler.CsvExport', ['--fname', fd.name, '--run-spark-locally', working_dir])
            except CalledProcessError as e:
                msg = 'Failed to execute download (cli failed)'
                logger.exception(msg)
                job.set_status(JobStatus.ERROR)
                job.set_status_details(msg)
                return False

            date_str = datetime.now().strftime('%m-%d-%Y-%H-%M-%S')

            s3 = aws_client('s3')

            zip_dir = f'{job.creating_user}-data-export-{date_str}'
            zip_path = os.path.join(working_dir, zip_dir)

            os.mkdir(zip_path)
            for dl in range(len(job.details.downloads)):
                path = os.path.join(working_dir, str(dl))
                if not os.path.exists:
                    # Table was unexpectedly empty
                    logger.debug('Download {path} did not exist - skipping')
                    continue
                files = files_in_dir(path)
                csv_fname = None
                for fname in files:
                    if fname.endswith('.csv'):
                        csv_fname = fname
                        break
                if csv_fname is None:
                    msg = f'Missing CSV, failed to find output in {path}'
                    logger.exception(msg)
                    job.set_status(JobStatus.ERROR)
                    job.set_status_details(msg)
                    return False

                download = job.details.downloads[i]
                new_csv_fname = '{}-{}-{}'.format(date_str,
                                                download['dataset'],
                                                download['table'])
                t = download['type']
                if t == 'row':
                    new_csv_fname = new_csv_fname + '-rows'
                    if len(download['filters'].keys()) > 0:
                        new_csv_fname = new_csv_fname + '-filtered'
                elif t == 'column_count':
                    new_csv_fname = new_csv_fname + '-' + download[
                        'column'] + '-column-count'

                new_csv_fname = new_csv_fname + '.csv'

                os.rename(csv_fname, os.path.join(zip_path, new_csv_fname))
            zip_file = zip_path + '.zip'
            files = files_in_dir(zip_path)
            logger.debug(f'File to download: {files}')
            zip_cmd_path = shwhich('zip')
            if zip_cmd_path is None:
                msg = 'Failed to find zip command'
                logger.exception(msg)
                job.set_status(JobStatus.ERROR)
                job.set_status_details(msg)
                return False

            cmd = [zip_cmd_path, '-j', zip_file] + files
            logger.debug(f'Zip cmd: {" ".join(cmd)}')
            sub_call(cmd, cwd=working_dir)

            s3key = S3_EXPORT_KEY + zip_file

            s3.upload_file(zip_file, c.s3Bucket, s3key)

            signed_url = s3.generate_presigned_url('get_object',
                                                        Params={'Bucket': c.s3Bucket,
                                                                'Key': s3key},
                                                        ExpiresIn=S3_URL_EXPIRATION)

            job.details.s3Path = signed_url
            job.set_details(job.details)
    return True


def run_job(conf: Config, job_id: str) -> None:
    job = None
    try:
        if not is_valid_config(conf):
            msg = f'Config is invalid, stopping job: {conf}'
            logger.error(msg)
            job.set_status(JobStatus.ERROR)
            job.set_status_details(msg)
            return
        api = JobApi(conf.jobsApiPath)
        job = api.job(job_id)
        if execute(conf, job) is True:
            job.set_status(JobStatus.COMPLETE)
    except Exception:
        msg = 'Unhandled exception'
        logger.exception(msg)
        if job is not None:
            job.set_status(JobStatus.ERROR)
            job.set_status_details(msg)


def get_args():
    argument_parser = ArgumentParser(description='Execute download job (typically as a tekton job')
    argument_parser.add_argument('job_id', help='Job ID to execute')
    return argument_parser.parse_args()


if __name__ == '__main__':
    args = get_args()
    setup_logger_with_job_id(logger, args.job_id)
    c = Config()
    c.from_env()
    start_time = datetime.now()
    run_job(c, args.job_id)
    execution_time = datetime.now() - start_time
    logger.info(f'Total execution time: {execution_time}')
