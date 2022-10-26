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
FROM ubuntu:jammy

# https://www.scala-sbt.org/1.x/docs/Installing-sbt-on-Linux.html#Ubuntu+and+other+Debian-based+distributions

ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=America/New_York
ENV BUILD_DEPS="apt-transport-https curl gnupg"

RUN apt update && \
        apt upgrade -y && \
        apt install -y curl && \
        apt install -y openjdk-11-jdk && \
        apt install -y maven && \
        apt install -y gnupg

RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list && \
        echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list && \
        curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | gpg --no-default-keyring --keyring gnupg-ring:/etc/apt/trusted.gpg.d/scalasbt-release.gpg --import &&  chmod 644 /etc/apt/trusted.gpg.d/scalasbt-release.gpg

RUN apt update && \
        apt install -y sbt && \
        cd /root && \
        sbt -version

RUN update-alternatives --install /usr/bin/java java /usr/lib/jvm/java-11-openjdk-amd64/bin/java 1 && \
        update-alternatives --install /usr/bin/javac javac /usr/lib/jvm/java-11-openjdk-amd64/bin/javac 1 && \
        update-alternatives --set javac /usr/lib/jvm/java-11-openjdk-amd64/bin/javac && \
        update-alternatives --set java /usr/lib/jvm/java-11-openjdk-amd64/bin/java

RUN apt-get remove --purge -y apt-transport-https curl gnupg && \
        apt-get autoremove -y && \
        rm -R /var/lib/apt/lists/*
