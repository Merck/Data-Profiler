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
from pathlib import Path

VAULT_PATH = 'https://secrets.dataprofiler.com'
JENKINS_PATH = 'https://admin.dataprofiler.com/jenkins/'
UBUNTU_AMI_ID = "ami-aa2ea6d0"  # 16.04
RHEL_AMI_ID = "ami-c998b6b2"  # RHEL 7.4
# These will all need to be able to be set by commandline by some point
DEFAULT_REGION = "us-east-1"
ENVIRONMENT = "Production"

# This is a global because we create Credentials all over the place
DEFAULT = 0
NAMED = 1
# global credentials_kind
credentials_kind = DEFAULT
ROOT_DIR = str(Path(os.path.dirname(os.path.dirname(os.path.realpath(__file__)))).parents[0])
MUSTER_DIR = ROOT_DIR + '/muster'
# global AUTH
AUTH = None
