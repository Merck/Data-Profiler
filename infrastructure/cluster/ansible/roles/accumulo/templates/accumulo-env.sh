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
#! /usr/bin/env bash

export ACCUMULO_LOG_DIR={{ worker_data_dirs[0] }}/logs/accumulo
export ZOOKEEPER_HOME={{ zookeeper_home }}
export JAVA_HOME={{ java_home }}

export HADOOP_PREFIX={{ hadoop_home }}
export HADOOP_CONF_DIR="$HADOOP_PREFIX/etc/hadoop"
export ACCUMULO_TSERVER_OPTS="-Xmx48g -Xms48g"
export ACCUMULO_MASTER_OPTS="-Xmx8g -Xms8g"
export ACCUMULO_MONITOR_OPTS="-Xmx1g -Xms1g"
export ACCUMULO_GC_OPTS="-Xmx2g -Xms2g"
export ACCUMULO_SHELL_OPTS="-Xmx512m -Xms512m"
export ACCUMULO_GENERAL_OPTS="-XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75 -Djava.net.preferIPv4Stack=true -XX:+CMSClassUnloadingEnabled"
export ACCUMULO_OTHER_OPTS="-Xmx256m -Xms64m"
export ACCUMULO_KILL_CMD='kill -9 %p'
export NUM_TSERVERS=1
export MALLOC_ARENA_MAX=${MALLOC_ARENA_MAX:-1}
