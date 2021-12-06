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
import sys
import os
import dpmuster.constants as constants


def get_os_type():
    if sys.platform.startswith('linux'):
        return 'linux'
    elif sys.platform == "darwin":
        return 'darwin'
    else:
        raise RuntimeError("Your OS is not supported")


def ecs_cli_path():
    platform = get_os_type()
    location = constants.MUSTER_DIR + "/bin/ecs-cli-" + platform + '-amd64-latest'
    if os.path.isfile(location):
        return location
    else:
        raise RuntimeError("An ecs-cli cannot be found for your platform")
