#!/usr/bin/env bash
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

SPARK_HOME=/opt/spark

NAMESPACE="${1}"
USER="${2}"
DRIVER_MEM="${3}"
EXECUTOR_MEM="${4}"
NUM_EXECUTORS=${5}
ZOOKEEPERS="${6}"

APP_NAME=spark-sql
DEFAULT_DATABASE="${USER}"
DRIVER_VOLUME_PREFIX="spark-sql-${USER}-${NAMESPACE}-driver"
EXECUTOR_VOLUME_PREFIX="spark-sql-${USER}-${NAMESPACE}-executor"
DRIVER_LOCAL_VOLUME="/local-driver-${USER}"
EXECUTOR_LOCAL_VOLUME="/local-executor-${USER}"
K8S_MASTER="k8s://https://kubernetes.default.svc"
IMAGE_NAME="container-registry.dataprofiler.com/spark-sql-client:latest"
POD_NAME="${APP_NAME}-${USER}"

$SPARK_HOME/bin/spark-submit \
  --master ${K8S_MASTER} \
  --deploy-mode cluster \
  --name "${APP_NAME}" \
  --class com.dataprofiler.sql.hive.DistributedSQLEngine \
  --packages com.amazonaws:aws-java-sdk-s3:1.11.375,commons-logging:commons-logging:1.1.3 \
  --conf spark.driver.memory="${DRIVER_MEM}" \
  --conf spark.executor.memory="${EXECUTOR_MEM}" \
  --conf spark.executor.instances="${NUM_EXECUTORS}" \
  --conf spark.driver.maxResultSize=4G \
  --conf spark.driver.port=32000 \
  --conf spark.driver.blockManager.port=32001 \
  --conf spark.blockManager.port=32002 \
  --conf spark.sql.hive.server2.enable.doAs=false \
  --conf spark.hive.server2.use.SSL=true \
  --conf spark.driver.extraJavaOptions="-Divy.cache.dir=/tmp -Divy.home=/tmp" \
  --conf spark.sql.hive.server2.thrift.bind.host=0.0.0.0 \
  --conf spark.sql.hive.server2.thrift.http.port=10000 \
  --conf spark.sql.hive.server2.thrift.port=10016 \
  --conf spark.hive.server2.transport.mode=http \
  --conf spark.sql.warehouse.dir=${DRIVER_LOCAL_VOLUME}/spark-warehouse \
  --conf spark.sql.hive.thriftServer.singleSession=true \
  --conf spark.sql.catalogImplementation=in-memory \
  --conf spark.sql.thriftServer.incrementalCollect=true \
  --conf spark.hadoop.metastore.catalog.default=spark \
  --conf spark.hadoop.hive.execution.engine=spark \
  --conf spark.kubernetes.executor.request.cores=100m \
  --conf spark.kubernetes.container.image=${IMAGE_NAME} \
  --conf spark.kubernetes.container.image.pullPolicy=Always \
  --conf spark.kubernetes.container.image.pullSecrets=dataprofiler-registry-credentials \
  --conf spark.kubernetes.driver.pod.name=${POD_NAME} \
  --conf spark.kubernetes.authenticate.driver.serviceAccountName=spark-sql \
  --conf spark.kubernetes.namespace=${NAMESPACE} \
  --conf spark.kubernetes.driver.label.app=${APP_NAME} \
  --conf spark.kubernetes.driver.label.name=${POD_NAME} \
  --conf spark.kubernetes.driver.secretKeyRef.LDAP_SERVICE_PASSWORD=ldap-service-password:LDAP_SERVICE_PASSWORD \
  --conf spark.kubernetes.driverEnv.DEFAULT_DATABASE="${DEFAULT_DATABASE}" \
  --conf spark.kubernetes.driver.volumes.persistentVolumeClaim.${DRIVER_VOLUME_PREFIX}.mount.path=${DRIVER_LOCAL_VOLUME} \
  --conf spark.kubernetes.driver.volumes.persistentVolumeClaim.${DRIVER_VOLUME_PREFIX}.mount.readOnly=false \
  --conf spark.kubernetes.driver.volumes.persistentVolumeClaim.${DRIVER_VOLUME_PREFIX}.options.claimName=${DRIVER_VOLUME_PREFIX} \
  --conf spark.kubernetes.executor.volumes.persistentVolumeClaim.${EXECUTOR_VOLUME_PREFIX}.mount.path=${EXECUTOR_LOCAL_VOLUME} \
  --conf spark.kubernetes.executor.volumes.persistentVolumeClaim.${EXECUTOR_VOLUME_PREFIX}.mount.readOnly=false \
  --conf spark.kubernetes.executor.volumes.persistentVolumeClaim.${EXECUTOR_VOLUME_PREFIX}.options.claimName=${EXECUTOR_VOLUME_PREFIX} \
  --conf spark.kubernetes.driver.secrets.dp-spark-sql-certs=/certs \
  --conf spark.kubernetes.executor.secrets.dp-spark-sql-certs=/certs \
  local:///app/jars/app.jar --zookeepers ${ZOOKEEPERS} \
  --accumulo-instance dataprofiler
