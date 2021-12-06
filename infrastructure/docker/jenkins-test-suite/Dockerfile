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
# To build
# docker build -f Dockerfile -t container-registry.com/jenkins-test-suite:<curent-version> .
#
# To deploy
# docker push container-registry.com/jenkins-test-suite:<current-version>
#
# Update test-suite.groovy to reflect the current version
FROM odavid/jenkins-jnlp-slave:debian

RUN curl -sL -o ./setup_12.x https://deb.nodesource.com/setup_12.x && chmod +x setup_12.x && ./setup_12.x
RUN apt-get update && apt-get install -y default-jre default-jdk maven jq libssl-dev python3 python3-pip nodejs && rm -rf /var/lib/apt/lists/*
RUN pip3 install wheel && pip3 install awscli  
RUN curl -L "https://github.com/docker/compose/releases/download/1.25.5/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
RUN curl -fsSL -o /tmp/get_helm.sh https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3  && chmod 700 /tmp/get_helm.sh && /tmp/get_helm.sh
RUN npm install --global yarn lerna

ADD slack-notify /usr/local/bin/slack-notify
ADD stash-notify /usr/local/bin/stash-notify
ADD get-vault.sh /usr/local/bin/get-vault
ADD docker-login.sh /usr/local/bin/docker-login

RUN chmod -R +x /usr/local/bin/slack-notify && chmod -R +x /usr/local/bin/stash-notify &&  \
    chmod -R +x /usr/local/bin/get-vault &&  chmod -R +x /usr/local/bin/docker-login  

