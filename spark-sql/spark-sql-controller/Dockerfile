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
FROM container-registry.dataprofiler.com/spark-hive-k8s:2.4.5

RUN set -ex && \
    apt update --allow-releaseinfo-change && \
    apt update --no-install-recommends && \
    apt install --no-install-recommends -y bash curl golang-go git build-essential && \
    rm /bin/sh && \
    ln -sv /bin/bash /bin/sh && \
    echo "auth required pam_wheel.so use_uid" >> /etc/pam.d/su && \
    chgrp root /etc/passwd && chmod ug+rw /etc/passwd && \
    rm -rf /var/cache/apt/*

WORKDIR /app

# Copy over app and compile it
COPY main.go go.* ./
RUN env GOOS=linux GOARCH=amd64 CGO_ENABLED=0 go build -o /bin/controller
RUN env GOOS=linux GOARCH=amd64 go build -o main .

COPY sbin sbin/
COPY bin bin/

ENTRYPOINT [ "/app/bin/spark-sql-controller.sh" ]
