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
import subprocess
import os
from .git_helper import extract_git_commit
from .env_helper import get_env_var_value
import dpmuster.constants as constants


def build_tag_push_container(build_ctx, env_file, cluster):
    current_rev = cluster.name + '-' + extract_git_commit()
    build_args = []
    build_args.append('--build-arg')
    build_args.append(
        'CURRENT_REVISION' + '=' + current_rev)
    if build_ctx.get('build_args', None) is not None:
        for argname in build_ctx['build_args']:
            build_args.append('--build-arg')
            build_args.append(
                argname + '=' + get_env_var_value(env_file, argname))

    location = build_ctx['location']
    container_name = build_ctx['container_name']

    registry_path = 'container-registry.dataprofiler.com'
    tag = registry_path + '/' + container_name + ':' + current_rev

    # Docker build
    command = ["docker", "build", "-f", location + '/Dockerfile', "-t", tag]
    command.extend(build_args)
    command.append(location)
    print(" ".join(command))

    docker_creds = os.path.expanduser("~/.docker/config.json")
    has_docker_login = False

    try:
        with open(docker_creds) as f:
            if registry_path in f.read():
                has_docker_login = True
    except FileNotFoundError:
        pass

    # Docker login
    if not has_docker_login:
        login_ret = subprocess.call(["docker", "login", registry_path, '-u', 'docker', '-p', 'docker'], cwd=constants.ROOT_DIR)
        if login_ret > 0:
            raise RuntimeError(
                'Docker login for ' + container_name + ' not successful')

    # Docker build
    build_ret = subprocess.call(command, cwd=constants.ROOT_DIR)
    if build_ret > 0:
        raise RuntimeError(
            'Docker build for ' + container_name + ' not successful')

    # Docker push
    push_ret = subprocess.call(['docker', 'push', tag], cwd=constants.ROOT_DIR)
    if push_ret > 0:
        raise RuntimeError(
            'Docker push for ' + container_name + ' not successful')


def intersection(l1, l2):
    if not l1 or not l2:
        return []
    return list(set(l1).intersection(l2))
