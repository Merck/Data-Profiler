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
#! /bin/bash

# This script will allow the image to run in kube so you can delete things from the cluster

IMAGE=$1
TAG=$2
NAMESPACE=$3

if [ "$#" -ne 3 ]; then
    echo "usage: kube-run.sh IMAGE TAG NAMESPACE" >&2
    exit 2
fi

kubectl run -i -t $TAG -n $NAMESPACE --rm=true --restart=Never --tty --image=container-registry.dataprofiler.com/$IMAGE\:latest --overrides='{"apiVersion":"v1","spec":{"imagePullSecrets":[{"name":"dataprofiler-registry-credentials"}],"containers":[{"name":"'$TAG'","image":"container-registry.dataprofiler.com/'$IMAGE':latest","envFrom":[{"configMapRef":{"name":"dataprofiler-web-'$NAMESPACE'-env-file"}}]}]}}' -- /bin/bash
