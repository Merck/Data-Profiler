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
FROM golang:alpine

COPY . /go/src/nginx-ldap-auth

ENV CGO_ENABLED=0

RUN cd /go/src/nginx-ldap-auth && \
	apk add --no-cache git && \
	go get -u gopkg.in/yaml.v2 && \
	go get -u gopkg.in/ldap.v2 && \
	go build -a -x -ldflags='-s -w -extldflags -static' -v -o /go/bin/nginx-ldap-auth ./main

FROM scratch
COPY --from=0 /go/bin/nginx-ldap-auth /usr/local/bin/nginx-ldap-auth

WORKDIR /tmp

VOLUME /etc/nginx-ldap-auth

EXPOSE 5555

USER 65534:65534

CMD [ \
	"/usr/local/bin/nginx-ldap-auth", \
	"--config", \
	"/etc/nginx-ldap-auth/config.yaml" \
]
