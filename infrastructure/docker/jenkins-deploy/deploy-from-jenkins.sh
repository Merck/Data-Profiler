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
#!/usr/bin/env bash

set -exo pipefail

# TODO: these positional args are hard to read, it might make sense to turn these into --argparse flags or something
APP=$1
NAMESPACE=$2
NO_CACHE=$3
REVISION=$4
VERSION=$(echo "$NAMESPACE-$REVISION")
TAG=$(echo "container-registry.com/$APP:$VERSION")
PATH=$5
# development-internal, development
LOADBALANCER=$6
HELM_ARGS="--set image.name=${TAG}"
VERSION="0.0.$BUILD_NUMBER" # BUILD_NUMBER is a jenkins default variable

# currently this is only --no-cache, but we might want to have more flags for things
EXPLICIT_BUILD_ARGS=""
if [ $NO_CACHE == "true" ]; then
  EXPLICIT_BUILD_ARGS="--no-cache" 
fi

if [ -z "${LOADBALANCER}" ]; then
  shift 5 # Anything after the this argument gets passed to docker build in the "$@"
else
  HELM_ARGS="${HELM_ARGS} --set loadBalancer=${LOADBALANCER}"
  shift 6 # Anything after the this argument gets passed to docker build in the "$@"
fi

echo "${PWD}"
echo "docker" | /usr/bin/docker login -u "docker" --password-stdin container-registry.com > /dev/null 2>&1
# /usr/bin/docker is bind mounted from kubernetes, so the networking is not pod level, it's node level. 
/usr/bin/docker build $EXPLICIT_BUILD_ARGS --network host "$@" -f $PATH/Dockerfile -t $TAG ./$PATH
/usr/bin/docker push $TAG


/usr/local/bin/kubectl config set-context --current --namespace=$NAMESPACE

# Replace the chart version with jenkins build number.
/bin/sed -i "/version/c\version: $VERSION" kube/helm-charts/namespace-deploy/$APP/Chart.yaml

cd kube/helm-charts/namespace-deploy/$APP

# Check to see if the chart is installed (will most likely always be true unless it's the first deploy)
if /usr/local/bin/helm status $APP; then
  /usr/local/bin/helm upgrade --values values.yaml "${APP}" ${HELM_ARGS} .
else
  /usr/local/bin/helm install --values values.yaml "${APP}" ${HELM_ARGS} .
fi
