#!/bin/bash
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


attributes=(`cat default_attributes`)

function query {
  attr=$1
  prefix='{"query":"mutation {markAttributeActive(value:\"'
  suffix='\"){id value is_active}}"}'
  qry="$prefix${attr}$suffix"
}

# Ensure dp-rou is ready
is_ready="false"
while [ "$is_ready" = false ]; do
    #echo "Waiting for dp-rou to report as RUNNING..."
    _status=`kubectl get pods | grep dp-rou | awk '{print $3;}'`
    if [ "$_status" = "Running" ]; then
      is_ready="true"
    else
      is_ready="false"
    fi
    sleep 2
done

# Activate attributes
for attr in ${attributes[@]};
do
  query "${attr}"
  echo "    > activating ${attr}"
  # echo -n -e "\e[1A\e[0K\r"
  # echo "$qry"
  curl localhost:8081/graphql -s -XPOST \
    -H 'Authorization: dp-rou-key' \
    -H 'Content-Type: application/json' \
    --retry-connrefused \
    --connect-timeout 5 \
    --max-time 10 \
    --retry 5 \
    --retry-delay 0 \
    --retry-max-time 60 \
    --data "$qry" > /dev/null
done


