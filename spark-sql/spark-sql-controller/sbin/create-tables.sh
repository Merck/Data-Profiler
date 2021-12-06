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
#!/bin/bash

server_port=10000
server_instance=$1
dataset=$2
table=$3
auths=$4
admin_username=$5
admin_password=$6

echo "Creating Table View [ $server_instance, $dataset, $table, $auths ]"

database_name=${dataset//[^a-zA-Z0-9_]/_}
table_name=${table//[^a-zA-Z0-9_]/_}

create_database_query="CREATE DATABASE IF NOT EXISTS ${database_name//\"/};"

create_table_query="CREATE TABLE ${database_name//\"/}.${table_name//\"/} \
    USING org.apache.spark.sql.sources.accumulo.v1 \
    OPTIONS (dataScanSpec=\"{\\\"dataset\\\":\\\"${dataset//\"/}\\\",\\\"table\\\":\\\"${table//\"/}\\\"}\",auths=\"${auths//\"/}\");"

/opt/spark/bin/beeline \
    -u "jdbc:hive2://${server_instance}:${server_port}/;ssl=true;sslTrustStore=/certs/truststore.jks;trustStorePassword=" \
    -n "$admin_username" \
    -p "$admin_password" \
    -e "${create_database_query};${create_table_query}"
