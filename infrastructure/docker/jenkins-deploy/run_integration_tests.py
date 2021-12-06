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
from argparse import ArgumentParser
from subprocess import CalledProcessError
from subprocess import run as process_run, PIPE
from json import loads as jloads, JSONDecodeError
from time import sleep
from os import getcwd
from sys import exit

from requests import api


def get_current_pod_info(pod_name='dp-api', namespace='development'):
    cmd = ['/bin/bash', '-c', 'kubectl get pods -n '+namespace+' -o json | jq -r \'[.items[] | select(.metadata.name | test("'+pod_name+'"))]\'']
    try:
        return jloads(process_run(cmd, check=True, stdout=PIPE).stdout.decode('utf-8').strip())
    except (CalledProcessError, JSONDecodeError):
        return []


def fetch_relevant_pod_data(pod_info):
    data = {}
    for pod in pod_info:
        status = pod.get('status', {}).get('phase')
        image = pod.get('status', {}).get('containerStatuses', [None, {}])[1].get('image')
        if (status is None) or (image is None):
            # No data, bad
            continue
        existing = data.get(image)
        if existing is not None:
            data[image].add(status)
        else:
            data[image] = {status}
    return data


def image_ready(new_image, pod_name, namespace, wait_time=3, tries=100):
    """
    Waits for the supplied image name to be fully ready
    :param new_image: str; name of image to wait for
    :param wait_time: int; seconds to wait before retrying
    :param tries: int; how many attempts to try before exiting with an error
    """
    # Waits for the supplied image name to be fully ready
    is_ready = False
    count = 0
    running_set = {'Running'}
    while is_ready is False:
        count +=1 
        if count == tries:
            return False
        pod_info = fetch_relevant_pod_data(get_current_pod_info(pod_name, namespace))
        if (new_image in pod_info) and (pod_info[new_image] == running_set):
            print('Image became ready')
            is_ready = True
            continue
        sleep(wait_time)
    return is_ready


def run_int_tests(api_path_env='', test_pass_env=''):
    cmd = ['/usr/local/bin/pytest', '-vv']
    env = {}
    if api_path_env != '':
        env['USER_FACING_API_HTTP_PATH'] = api_path_env
    if test_pass_env != '':
        env['INTTESTPASS'] = test_pass_env

    try:
        process_run(cmd, 
                    check=True, 
                    cwd='/'.join([getcwd(), 'python_client/tests/integration']),
                    env=env if env else None
                   )
        return 0
    except CalledProcessError:
        return -1


if __name__ == '__main__':
    parser = ArgumentParser(description='Waits for the supplied image to become ready on cluster')
    parser.add_argument('image', type=str, help='Fully qualified image name to wait for')
    parser.add_argument('pod_name', type=str, help='Pod name of potentially existing pod to filter on')
    parser.add_argument('namespace', type=str, help='Namespace to monitor')
    parser.add_argument('--api_host', type=str, default='', help='API Host to pass as env')
    parser.add_argument('--api_pass', type=str, default='', help='API test-developer password to pass as env')
    parser.add_argument('--test_on', type=str, default='development', help='What namespace to test on')
    args = parser.parse_args()
    if args.namespace != args.test_on:
        print('Not running integration tests')
        # Only test on development by default
        exit(0)
    print('Attempting to run integration tests')
    if image_ready(args.image, args.pod_name, args.namespace):
        exit(run_int_tests(api_path_env=args.api_host, test_pass_env=args.api_pass))
    print('Image never became ready!')
    exit(-1)  # Image never became ready!
