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
FROM data-profiler/hadoop:latest

RUN pip3 install --upgrade pip

ENV WHEEL dataprofiler_api-1.0-py3-none-any.whl
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64

WORKDIR /opt/app
ADD data_profiler_core_jars/dataprofiler-tools-current.jar .
ADD python_packages/${WHEEL} .
RUN pip3 install ${WHEEL}

RUN groupadd spark || echo 'spark group already added'
RUN useradd -r -g spark spark || echo 'spark user already exists'
RUN usermod -a -G hdfs spark || echo 'spark user already in hdfs group'

USER spark

CMD ["python3", "/usr/local/bin/daemon.py", "daemon"]
