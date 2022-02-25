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

APP_NAME=spark-sql
NAMESPACE=default
K8S_MASTER="k8s://$(kubectl cluster-info| head -n1| egrep -o -e "https:\/\/([0-9]|..)+:(\d)+")"
DRIVER_MEM=512m
EXECUTOR_MEM=1g
NUM_EXECUTORS=1
DRIVER_LOCAL_VOLUME=/localdir
EXECUTOR_LOCAL_VOLUME=/localdir

echo "deleting existing spark-sql pod"
kubectl delete pod spark-sql

$SPARK_HOME/bin/spark-submit --master ${K8S_MASTER} \
  --deploy-mode cluster \
  --name ${APP_NAME} \
  --class com.dataprofiler.sql.hive.DistributedSQLEngine \
  --conf spark.driver.memory=${DRIVER_MEM} \
  --conf spark.executor.memory=${EXECUTOR_MEM} \
  --conf spark.executor.instances=${NUM_EXECUTORS} \
  --conf spark.driver.port=32000 \
  --conf spark.driver.blockManager.port=32001 \
  --conf spark.blockManager.port=32002 \
  --conf spark.sql.hive.server2.enable.doAs=false \
  --conf spark.sql.hive.server2.thrift.bind.host=0.0.0.0 \
  --conf spark.sql.hive.server2.thrift.http.port=10000 \
  --conf spark.sql.hive.server2.thrift.port=10016 \
  --conf spark.hive.server2.transport.mode=http \
  --conf spark.hive.server2.use.SSL=false \
  --conf spark.hive.server2.authentication=NOSASL \
  --conf spark.hive.server2.thrift.sasl.qop=auth \
  --conf spark.sql.warehouse.dir=${DRIVER_LOCAL_VOLUME}/spark-warehouse \
  --conf spark.sql.hive.thriftServer.singleSession=true \
  --conf spark.sql.catalogImplementation=in-memory \
  --conf spark.hadoop.metastore.catalog.default=spark \
  --conf spark.hadoop.hive.execution.engine=spark \
  --conf spark.kubernetes.executor.request.cores=1 \
  --conf spark.kubernetes.container.image=spark-sql \
  --conf spark.kubernetes.container.image.pullPolicy=Never \
  --conf spark.kubernetes.driver.pod.name=${APP_NAME} \
  --conf spark.kubernetes.authenticate.driver.serviceAccountName=spark \
  --conf spark.kubernetes.namespace=${NAMESPACE} \
  --conf spark.kubernetes.driver.label.app=${APP_NAME} \
  --conf spark.kubernetes.driver.volumes.persistentVolumeClaim.spark-local-dir-localdirpvc.mount.path=${DRIVER_LOCAL_VOLUME} \
  --conf spark.kubernetes.driver.volumes.persistentVolumeClaim.spark-local-dir-localdirpvc.mount.readOnly=false \
  --conf spark.kubernetes.driver.volumes.persistentVolumeClaim.spark-local-dir-localdirpvc.options.claimName=spark-driver-localdir-pvc \
  --conf spark.kubernetes.executor.volumes.persistentVolumeClaim.spark-local-dir-localdirpvc.mount.path=${EXECUTOR_LOCAL_VOLUME} \
  --conf spark.kubernetes.executor.volumes.persistentVolumeClaim.spark-local-dir-localdirpvc.mount.readOnly=false \
  --conf spark.kubernetes.executor.volumes.persistentVolumeClaim.spark-local-dir-localdirpvc.options.claimName=spark-exec-localdir-pvc \
  local:///app.jar
