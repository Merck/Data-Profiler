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
# Build Jar
FROM maven:3-openjdk-8 as JARBUILDER
WORKDIR /home
COPY data_profiler_core .
RUN mvn clean install -DskipTests -Plocal

# Build python wheel
FROM python:latest as PYBUILDER
WORKDIR /home
COPY python_client .
RUN python setup.py bdist_wheel

FROM alpine

RUN apk update && apk upgrade
RUN apk add --no-cache gcc python3-dev musl-dev linux-headers libffi-dev make libevent-dev build-base bash
RUN ln -s /usr/libexec/gcc/x86_64-alpine-linux-musl/10.2.1/cc1 /usr/local/bin/cc1plus
# Install extra tools
RUN apk add --no-cache curl vim
# Install java8
RUN apk add --no-cache openjdk8
# Install python (and update)
RUN apk add --no-cache python3 py3-pip && \
    pip3 install --no-cache-dir --upgrade pip && \
    pip3 install --no-cache-dir ipython

# Get dataprofiler jar
WORKDIR /usr/lib/dataprofiler/tools/
COPY --from=JARBUILDER /home/tools/target/dataprofiler-tools-1.jar .

# Get python wheel
WORKDIR /opt/app
RUN pip3 install gevent==1.5.0

COPY --from=PYBUILDER /home/dist/dataprofiler_api-1.0-py3-none-any.whl .
RUN pip3 install dataprofiler_api-1.0-py3-none-any.whl && \
    ln -s /usr/bin/dpversions /opt/app/dpversions
COPY dataprofiler-tools/cluster-shell/helpers/ .

# Get Pytests
COPY ./python_client/tests ./python_tests

CMD ["/bin/bash"]
