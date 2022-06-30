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
# Build Jars from data_profiler_core
FROM maven:3-openjdk-8 as JARBUILDER

ARG SPARK_CLIENT_DIR="spark-sql/spark-sql-client"

WORKDIR /home
COPY dp-core .
RUN mvn clean install -DskipTests -Plocal

# Build the ldap jar
COPY ${SPARK_CLIENT_DIR}/spark_jars/spark-hive-thriftserver_2.11-2.4.5.jar .
RUN javac -cp spark-hive-thriftserver_2.11-2.4.5.jar /home/spark-sql/src/main/java/org/apache/hive/service/auth/SparkSqlLdapAuthenticator.java -d .
RUN jar cf ldapauth.jar org

# Create the spark-sql-client container
FROM container-registry.dataprofiler.com/spark-hive-k8s:2.4.5

ARG SPARK_CLIENT_DIR="spark-sql/spark-sql-client"

# Get the spark-sql jar and the ldapauth jars build above
WORKDIR /app
COPY --from=JARBUILDER /home/spark-sql/target/spark-sql-1.jar jars/app.jar
COPY --from=JARBUILDER /home/ldapauth.jar /opt/spark/jars
COPY --from=JARBUILDER /home/spark-sql/src/main/resources/log4j.properties /etc/
COPY --from=JARBUILDER /home/spark-sql/src/main/resources/ldap.properties /etc/

ENV HIVE_CONF_DIR=/etc/hive/conf
ENV HADOOP_CONF_DIR=/etc/hive/conf

# Copy over the config and certs
COPY ${SPARK_CLIENT_DIR}/conf/hive-site.xml /etc/hive/conf/hive-site.xml
COPY ${SPARK_CLIENT_DIR}/entrypoint.sh /app/entrypoint.sh

EXPOSE 10000

ENTRYPOINT [ "/app/entrypoint.sh" ]
