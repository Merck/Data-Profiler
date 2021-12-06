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
FROM container-registry.dataprofiler.com/nodeyarn:latest AS builder

EXPOSE 80
WORKDIR /opt/app

ARG CLUSTER_NAME
ARG SENTRY_UI_DSN
ARG SENTRY_UI_RELEASE_API_TOKEN
ARG USER_FACING_API_HTTP_PATH
ARG CURRENT_REVISION
ARG USER_FACING_UI_HTTP_PATH
ARG MICROSOFT_AZURE_APP_ID
ARG ANALYTICS_UI_SITE_ID
ARG USE_ALTERNATE_AUTH
ARG KIBANA_PARTIAL_URL

# TODO - figure out a better way to dockercache.
ADD . /opt/app

# TODO - figure out a better way to run cache/build
RUN yarn install && yarn run bootstrap && yarn run build

# Copy to runner
FROM nginx:mainline-alpine
COPY --from=builder /opt/app/packages/parent/build /opt/app/build
ADD conf/nginx.conf /etc/nginx/nginx.conf
CMD ["nginx"]
