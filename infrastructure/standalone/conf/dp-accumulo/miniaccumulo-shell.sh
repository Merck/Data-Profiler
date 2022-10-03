#
# Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
#
#	Licensed to the Apache Software Foundation (ASF) under one
#	or more contributor license agreements. See the NOTICE file
#	distributed with this work for additional information
#	regarding copyright ownership. The ASF licenses this file
#	to you under the Apache License, Version 2.0 (the
#	"License"); you may not use this file except in compliance
#	with the License. You may obtain a copy of the License at
#
#	http://www.apache.org/licenses/LICENSE-2.0
#
#
#	Unless required by applicable law or agreed to in writing,
#	software distributed under the License is distributed on an
#	"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#	KIND, either express or implied. See the License for the
#	specific language governing permissions and limitations
#	under the License.
#
#! /bin/bash

set -x
# Start miniaccumulo shell
zookeeper1_port=${2:-2181}
zookeepers="dp-accumulo:${zookeeper1_port}"
config="${config_dir}/conf/client.conf"
java -cp "/opt/spark/jars/*:./dataprofiler-tools-current.jar" com.dataprofiler.shell.MiniAccumuloShell \
        -u root \
        -p "" \
        -z miniInstance "${zookeepers}"
