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
FROM container-registry.dataprofiler.com/hadoop:latest


RUN mkdir -p /usr/lib/dataprofiler/tools/
ADD data_profiler_core_jars/dataprofiler-tools-current.jar /usr/lib/dataprofiler/tools/

ENV WHEEL dataprofiler_api-1.0-py3-none-any.whl

WORKDIR /opt/app

RUN pip3 install boto3
RUN apt install -y zip

ADD python_packages/${WHEEL} .
RUN pip3 install ${WHEEL}
RUN ln -fs /usr/local/bin/dpversions /opt/app/dpversions

CMD ["/bin/sleep", "99999"]
