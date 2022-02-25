#!/usr/bin/env bash
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


# We want to run with almost all of the memory allocated to the container, but it's really not
# easy to get the amount of memory inside of the container. So we are just going to take the memory
# for the whole system and subtract a little
MEM=$(expr $(awk '/MemTotal/ { printf "%.f \n", $2/1024/1024 }' /proc/meminfo) - 2)

/opt/api/target/universal/stage/bin/dataprofiler-api -J-Xms${MEM}G -J-Xmx${MEM}G \
  -Dplay.http.secret.key=$PLAY_FRAMEWORK_SECRET \
  -Dpidfile.path=/dev/null \
  "$@"
