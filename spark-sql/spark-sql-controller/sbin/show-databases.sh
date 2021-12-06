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

echo "Show databases"

server_instance=$1
admin_username=$2
admin_password=$3

server_port=10000

show_databases_qry="SHOW DATABASES;"

/opt/spark/bin/beeline -u "jdbc:hive2://${server_instance}:${server_port}/;ssl=true;sslTrustStore=/certs/truststore.jks;trustStorePassword=" \
    -n "$admin_username" \
    -p "$admin_password" \
    -e "$show_databases_qry"
