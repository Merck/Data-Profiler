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
FROM ubuntu:focal

ENV HADOOP_VER hadoop-2.9.2
ENV HADOOP_TARBALL ${HADOOP_VER}.tar.gz

ENV SPARK_VER spark-2.4.7
ENV SPARK_TARBALL ${SPARK_VER}-bin-without-hadoop.tgz

ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=America/New_York

ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64/

ADD https://archive.apache.org/dist/hadoop/common/${HADOOP_VER}/${HADOOP_TARBALL} /tmp

WORKDIR /opt/hadoop
RUN tar xfvz /tmp/${HADOOP_TARBALL}
RUN ln -s ${HADOOP_VER} current

ADD https://archive.apache.org/dist/spark/${SPARK_VER}/${SPARK_TARBALL} /tmp

WORKDIR /opt/spark
RUN tar xfvz /tmp/${SPARK_TARBALL}
RUN ln -s ${SPARK_VER}-bin-without-hadoop current

RUN apt-get update && apt-get install openjdk-8-jdk python3 python3-pip curl htop -y

RUN pip3 install --upgrade pip

RUN groupadd --gid 1000 hdfs \
    && useradd --uid 1000 --gid hdfs --shell /bin/bash --create-home hdfs \
    && groupadd spark && useradd -r -g spark spark && usermod -a -G hdfs spark

ENV SPARK_HOME /opt/spark/current
ENV HADOOP_HOME /opt/hadoop/current

ENV PATH="$SPARK_HOME/bin:$HADOOP_HOME/bin:$PATH"

# If you want to use spark-submit you need to execute something like this
# in your scripts (we can't evaluate it here)
# export SPARK_DIST_CLASSPATH=$(hadoop classpath)
