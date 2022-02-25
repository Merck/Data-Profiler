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

# check that the db-postgres is up
is_running="false"
while [ "$is_running" = false ]; do
    db_status=`kubectl get pods | grep dp-postgres | awk '{print $3;}'`
    if [ "$db_status" = "Running" ]; then
      is_running="true"
    else
      is_running="false"
    fi
    sleep 2
done

if [ "$is_running" = true ]; then
    kubectl exec -it $(kubectl get pods | grep dp-postgres | awk '{print $1;}') -c dp-postgres -- \
      psql -U postgres -d rules_of_use \
      -c "insert into applications (app,key,created_at,updated_at) values('Data Profiler', 'dp-rou-key', current_timestamp, current_timestamp);" \
      > /dev/null
else
    exit 1
fi