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

# Final Container
FROM container-registry.dataprofiler.com/hadoop:latest

WORKDIR /opt/app

COPY --from=JARBUILDER /home/tools/target/dataprofiler-tools-1.jar ./dataprofiler-tools-current.jar
COPY --from=PYBUILDER /home/dist/dataprofiler_api-1.0-py3-none-any.whl .

RUN apt install -y zip
RUN pip3 install dataprofiler_api-1.0-py3-none-any.whl

COPY ./tekton-jobs/connection-engine/connection_engine.py .
COPY ./tekton-jobs/connection-engine/connection_engine.sh .

CMD ["tail", "-f", "/dev/null"]
