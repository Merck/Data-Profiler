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
# Build python wheel
FROM python:latest as PYBUILDER
WORKDIR /home
COPY python_client .
RUN python setup.py bdist_wheel

# Deployed image
FROM python:3.8-alpine


WORKDIR /home
COPY --from=PYBUILDER /home/dist/dataprofiler_api-1.0-py3-none-any.whl .

RUN apk update && apk upgrade && \
    apk add --no-cache python3-dev libffi-dev gcc musl-dev make libevent-dev build-base curl
RUN pip install dataprofiler_api-1.0-py3-none-any.whl

#RUN curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl" && \
RUN curl -LO https://dl.k8s.io/release/v1.16.7/bin/linux/amd64/kubectl && \
    chmod +x kubectl

COPY ./tekton-canceller/cancel-tekton-job.sh .
COPY ./tekton-canceller/send-api-cancel.py .

CMD ["python", "send-api-cancel.py"]