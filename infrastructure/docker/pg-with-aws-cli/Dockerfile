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

FROM data-profiler/nodepg:latest
#https://stackoverflow.com/questions/51033689/how-to-fix-error-on-postgres-install-ubuntu
RUN mkdir -p /usr/share/man/man1 && mkdir -p /usr/share/man/man7

RUN apt-get update && apt-get install -y wget
RUN wget -q https://www.postgresql.org/media/keys/ACCC4CF8.asc -O- | apt-key add - && echo "deb http://apt.postgresql.org/pub/repos/apt/ stretch-pgdg main" | tee /etc/apt/sources.list.d/postgresql.list
RUN apt-get update && apt-get install -y python3-pip postgresql-10 && pip3 install awscli && rm -rf /var/lib/apt/lists/*
