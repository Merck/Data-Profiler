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
import argparse
from pprint import pprint
from .docker_helper import *


class Cluster:
    pass


def main():
    """
       Run this command like the following:
        cd muster
        source vent/bin/activate
        python -m dpmuster.build_cli build configs/kube-development-env --component ui --component api --cluster-name kube-development --base-path '../'
    :return:
    """
    parser = argparse.ArgumentParser(description="Tool to build Data Profiler images")
    parser.add_argument("--debug", default=False, action="store_true")
    subparsers = parser.add_subparsers(help="commands", dest="command")
    d = subparsers.add_parser("build", help="build images")
    d.add_argument("envfile")
    d.add_argument("--component", action="append", default=None,
                   choices=["ui", "redis", "api", "backend", "data-loader",
                            "healthcheck", "rules-of-use-api",
                            "makes", "grafana", "commands"])
    d.add_argument("--cluster-name", default=None)
    d.add_argument("--base-path", default=None)
    args = parser.parse_args()
    cluster = Cluster()
    cluster.name = args.cluster_name
    pprint(cluster)
    pprint(args)
    if args.command is None:
        parser.print_help()
        sys.exit(1)

    # Build Docker files
    if intersection(args.component, DOCKER_BUILD_MAP.keys()):
        for app in intersection(args.component, DOCKER_BUILD_MAP.keys()):
            pprint(DOCKER_BUILD_MAP[app])
            location = DOCKER_BUILD_MAP[app]['location']
            DOCKER_BUILD_MAP[app]['location'] = append_base_path(location, args.base_path)
            build_tag_push_container(DOCKER_BUILD_MAP[app], args.envfile, cluster)


def append_base_path(location, base_path=''):
    path = os.path.join(base_path, location)
    if os.path.isdir(path):
        path += os.sep
    return path


if __name__ == '__main__':
    main()
