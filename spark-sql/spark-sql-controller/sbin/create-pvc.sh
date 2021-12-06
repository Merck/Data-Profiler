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
#!/bin/bash

echo "Creating Kubernetes Persistent Volume Claim"

NAMESPACE=$1
PVC_NAME=$2
PVC_TYPE=$3
PVC_SIZE=$4
POD_NAME=$5
POD_ROLE=$6

PVC_DESCRIPTOR=$(cat <<EOF
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: ${PVC_NAME}
  namespace: ${NAMESPACE}
  labels:
    app: spark-sql
    name: ${POD_NAME}
    spark-role: ${POD_ROLE}
  annotations: {}
spec:
  selector:
    app: spark-sql
    name: ${POD_NAME}
    spark-role: ${POD_ROLE}
  accessModes:
    - ReadWriteMany
  resources:
    requests:
      storage: ${PVC_SIZE}
  storageClassName: ${PVC_TYPE}
EOF
)

echo "PERSISTENT VOLUME CLAIM YAML DESCRIPTOR:"
echo "${PVC_DESCRIPTOR}"

TOKEN=$(cat /var/run/secrets/kubernetes.io/serviceaccount/token)

curl -k -v -X POST \
  --header "Content-Type: application/yaml" \
  --header "Authorization: Bearer ${TOKEN}" \
  --data "${PVC_DESCRIPTOR}" \
  https://kubernetes.default.svc/api/v1/namespaces/${NAMESPACE}/persistentvolumeclaims