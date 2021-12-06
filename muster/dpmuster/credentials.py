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
import configparser
import requests
import os
import sys
import subprocess
import dpmuster.constants as constants
from bs4 import BeautifulSoup
from .ecs_helper import ecs_cli_path

AUTH_URL = ''

class CredentialDownloadException(Exception):
    pass


class Credentials:
    PROFILE_NAMES = {
        0: 'default'
    }

    def __init__(self, profile_name=None, kind=None):
        self.profile_name = profile_name
        if self.profile_name:
            self.kind = constants.NAMED
        else:
            if kind:
                self.kind = kind
            else:
                self.kind = constants.credentials_kind
            assert (self.kind != constants.NAMED)

            self.profile_name = self.PROFILE_NAMES[self.kind]
        self.aws_creds_fname = os.path.expanduser('~/.aws/credentials')
        self.ecs_creds_fname = os.path.expanduser('~/.ecs/credentials')

    def get_config(self):
        config = configparser.ConfigParser()
        if os.path.exists(self.aws_creds_fname):
            config.read(self.aws_creds_fname)

        return config

    def get_saved_credentials(self):
        config = self.get_config()
        if self.profile_name not in config:
            return None
        else:
            return config[self.profile_name]

    def save_credentials(self, credentials):
        assert (self.profile_name.startswith('muster_'))
        config = self.get_config()
        config[self.profile_name] = credentials
        self.save_aws_credentials(config)
        self.save_ecs_cli_credentials(config)

    def save_aws_credentials(self, config):
        awsdir = os.path.expanduser('~/.aws')
        if not os.path.exists(awsdir):
            os.mkdir(awsdir, 0o770)
        config.write(open(self.aws_creds_fname, 'w'))

    def save_ecs_cli_credentials(self, config):
        command = [
            ecs_cli_path(),
            'configure',
            'profile',
            '--access-key',
            config[self.profile_name]['aws_access_key_id'],
            '--secret-key',
            config[self.profile_name]['aws_secret_access_key'],
            '--session-token',
            config[self.profile_name]['aws_session_token'],
            '--profile-name',
            self.profile_name
        ]
        p = subprocess.call(
            command, cwd=constants.MUSTER_DIR, stdout=subprocess.PIPE)
        if p > 0:
            raise RuntimeError('Could not save ecs-cli credentials')

    def get_session(self, force_download=False):
        if self.get_saved_credentials() is None or force_download:
            download = True
        else:
            download = False

        if download and self.kind not in [constants.LAB, constants.MODERN]:
            raise Exception("Tried to download credentials")

        if download:
            self.download()

        boto_session = boto3.Session(region_name=constants.DEFAULT_REGION,
                                     profile_name=self.profile_name)

        # See if credentials are active or have expired
        # sts.get_caller_identity doesn't require any iam grants, and therefore is the best "ping" command
        boto_sts_client = boto_session.client('sts')
        try:
            boto_sts_client.get_caller_identity()
        except boto_sts_client.exceptions.ClientError as e:
            print("Refreshing AWS Credentials")
            self.download()
            boto_session = boto3.Session(region_name=constants.DEFAULT_REGION,
                                         profile_name=self.profile_name)

        return boto_session

    def parse_credentials(self, creds):
        required_keys = {"aws_access_key_id", "aws_secret_access_key",
                         "aws_session_token"}

        credentials = {}
        lines = creds.split('\n')
        for line in lines:
            line = line.strip()
            tokens = line.split("=")
            if len(tokens) < 2:
                continue
            credentials[tokens[0]] = "=".join(tokens[1:])

        if len(required_keys ^ set(credentials.keys())) != 0:
            print("invalid aws credentials file")
            sys.exit(1)

        return credentials

    def download(self):
        user = constants.AUTH.username
        passwd = constants.AUTH.password
        try:
            url = AUTH_URL

            r = requests.get(url, auth=(user, passwd), verify=False)
        except Exception as e:
            print("error connecting to merk auth")
            print(e)
            raise CredentialDownloadException()

        if r.status_code != 200:
            print("error fetching keys")
            raise CredentialDownloadException()

        credentials = self.parse_credentials(
            BeautifulSoup(r.content, 'html.parser').pre.string)
        self.save_credentials(credentials)
