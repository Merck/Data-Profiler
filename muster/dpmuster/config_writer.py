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

import jinja2
import json
import pathlib

from .vaultops import VaultOps, VaultOpsException


def to_camel_case(snake_str):
    parts = snake_str.split("_")
    if len(parts) == 1:
        return parts[0].lower()
    else:
        return parts[0].lower() + "".join(x.title() for x in parts[1:])


def write_dataprofiler_config(env):
    out = {}

    for k, v in env.items():
        key = to_camel_case(k)
        out[key] = v

    config_dir = os.path.expanduser('~/.dataprofiler')
    try:
        os.mkdir(config_dir)
    except OSError:
        pass

    with open(os.path.join(config_dir, 'config'), 'w') as fd:
        json.dump(out, fd, indent=4, sort_keys=True)


def write_web_env(env):
    outstrs = []
    envcopy = env.copy()
    envcopy["USER_FACING_API_HTTP_PATH"] = "http://localhost:9000"
    for k, v in envcopy.items():
        outstrs.append(f"{k.upper()}={v}")
    outstrs.append("AUTH_METHOD=local-developer")
    outstrs.append(
        "RULES_OF_USE_API_HTTP_PATH=https://development.dataprofiler.com/rou")

    final_str = "\n".join(outstrs)

    with open("../web/api/.env", "w") as fd:
        fd.write(final_str)


def set_dev_env(envname: str):

    v = VaultOps()
    try:
        env = v.decrypt("configs/" + envname)
    except VaultOpsException:
        print("Could not find configuration for env: " + envname)
        print("Available envs are: " + ", ".join(v.available_configs()))
        return
    env = env["data"]

    # fixup rou to be the externally visiable url - this may not work for all environments
    rouname = envname.split("-")[1] + "-internal"
    env["RULES_OF_USE_API_HTTP_PATH"] = "https://{}.dataprofiler.com/rou".format(
        rouname)

    write_dataprofiler_config(env)
    write_web_env(env)
