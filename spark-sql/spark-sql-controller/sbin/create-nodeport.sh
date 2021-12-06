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

echo "Creating nodeport service"

check_exit_code() {
  local exit_code=$1
  local msg=$2
  if [ $exit_code -ne 0 ]; then
    echo "FAILED to create nodeport service $msg"
    exit $exit_code
  else
    echo "CREATED create nodeport service $msg"
  fi
}

NAMESPACE=$1
POD_NAME=$2
ROLE_NAME=driver
POD_PORT=10000

TOKEN=$(cat /var/run/secrets/kubernetes.io/serviceaccount/token)

# Create nodeport service
# POST /api/v1/namespaces/{namespace}/services/{name}
NODEPORT_SERVICE_DESCRIPTOR=$(cat <<EOF
apiVersion: v1
kind: Service
metadata:
  labels:
    app: spark-sql
    name: ${POD_NAME}
    spark-role: ${ROLE_NAME}
  name: ${POD_NAME}
  namespace: ${NAMESPACE}
spec:
  type: NodePort
  selector:
    app: spark-sql
    name: ${POD_NAME}
    spark-role: ${ROLE_NAME}
  ports:
    - port: ${POD_PORT}
      targetPort: ${POD_PORT}
      name: jdbc-1
      protocol: TCP
    - port: 4040
      targetPort: 4040
      name: jdbc-2
      protocol: TCP
EOF
)
echo "SVC YAML DESCRIPTOR: ${NODEPORT_SERVICE_DESCRIPTOR}"
curl -k -v -X POST \
  --header "Content-Type: application/yaml" \
  --header "Authorization: Bearer ${TOKEN}" \
  --data "${NODEPORT_SERVICE_DESCRIPTOR}" \
  https://kubernetes.default.svc/api/v1/namespaces/${NAMESPACE}/services

check_exit_code $? "nodeport service"