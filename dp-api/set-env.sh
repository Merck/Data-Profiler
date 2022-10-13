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

echo "Setting MINIKUBE ENVIRONMENT"

cat << EOF >> .env
ACCUMULO_API_TOKENS_TABLE=apiTokens
ACCUMULO_COLUMN_COUNTS_TABLE=columnCounts
ACCUMULO_DATAWAVE_ROW_TABLE=datawaveRows
ACCUMULO_INDEX_TABLE=index
ACCUMULO_INSTANCE=miniInstance
ACCUMULO_JOBTABLE=dataLoadJobs
ACCUMULO_METADATA_TABLE=metadata
ACCUMULO_NAMESPACES=curr,next
ACCUMULO_PASSWORD=
ACCUMULO_SAMPLES_TABLE=samples
ACCUMULO_USER=root
ANALYTICS_API_SITE_ID=2
ANALYTICS_UI_SITE_ID=5
AUTH_METHOD=local-developer
# AUTH_METHOD=oauth
AWS_CREDENTIALS=xx
AWS_S3_UPLOAD_ACCESS_KEY_ID=xx
AWS_S3_UPLOAD_BUCKET=xxxxx
AWS_S3_UPLOAD_SECRET_ACCESS_KEY=xx/xxxx
BACKGROUND_YARN_QUEUE=ingest
CLUSTER_NAME=development
COGNITO_AUTHSERVER=https://dataprofiler.xxxx
COGNITO_CLIENT_ID=x
COGNITO_CLIENT_SECRET=x
DATALOADING_URL=https://production-internal/jobs/graphql
DATA_PROFILER_TOOLS_JAR=s3a://xxx/dataprofiler-tools-current.jar
DEFAULT_YARN_QUEUE=ingest
DNS_EXTERNAL_ADDRESS=dataprofiler
DNS_INTERNAL_ADDRESS=production-internal
DOC_PREVIEW_HOST=
DOWNLOAD_EVENT_LISTENER_URL=http://el-download-listener.production.svc.cluster.local:8080
DOWNLOAD_YARN_QUEUE=ingest
ENVIRONMENT=development
HADOOP_DEFAULT_FS=dataprofilerFS
HADOOP_NAMENODE1=accumulo-master-production-1
HADOOP_NAMENODE2=accumulo-master-production-2
#HEALTH_CHECK_PLAY_FRAMEWORK_ENDPOINT=http://dp-ai:8082/health/check/
#HEALTH_CHECK_SLACK_ENDPOINT=https://hooks.slack.com/services/xx/xxx/xxxxx
INACTIVE_TIMEOUT_MINUTES=30
INTERACTIVE_YARN_QUEUE=ingest
JOBS_API_PATH=https://production-internal/jobs/graphql
LOAD_OUTPUT_DEST=/loader
HR_ETL_JWT=xxxxx.xx.
MICROSOFT_AZURE_APP_ID=
PLAY_FRAMEWORK_SECRET=
PLAY_REQUIRE_LOGIN_ATTRIBUTE_FOR_ACCESS=false
ROU_DB_DATABASE=rules_of_use
ROU_DB_HOST=dp-postgres
ROU_DB_PASSWORD=
ROU_DB_USERNAME=postgres
RULES_OF_USE_API_HTTP_PATH=http://dp-rou:8081
RULES_OF_USE_API_KEY=dp-rou-key
RULES_OF_USE_API_PATH=graphql
S3_BUCKET=dataprofiler-s3
SENTRY_API_DSN=https://xx@xx.ingest.sentry.io/xxxx
SENTRY_UI_DSN=https://xx@xx.ingest.sentry.io/xxxx
SENTRY_UI_RELEASE_API_TOKEN=xxxx
SLACK_WEBHOOK=https://hooks.slack.com/services/xx/xx/xxxx
SQLSYNC_EVENT_LISTENER_URL=http://el-sqlsync-listener.production.svc.cluster.local:8080
SQLSYNC_PASSWORD=xx
SQLSYNC_URL=jdbc:postgresql://postgres.production.svc.cluster.local:5432/dataprofiler
SQLSYNC_USER=postgres
TIMEOUT_MINUTES=480
OAUTH_CLIENT_ID=
OAUTH_CLIENT_SECRET=
OAUTH_AUTH_URL=https://iapi/authentication
OAUTH_SCOPE=
OAUTH_STATE=
USER_FACING_HTTP_PATH=https://dataprofiler
UPLOAD_YARN_QUEUE=ingest
USER_FACING_API_HTTP_PATH=http://localhost:9000
ZOOKEEPERS=accumulo:2181
EOF
