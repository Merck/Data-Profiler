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
# A small script to install in the correct namespace

usage() {
    echo "Usage: $0 development|preview|production"
}

if [ $# -eq 0 ] || [ $# -gt 1 ]; then
    echo "Incorrect number of arguments"
    usage
    exit -1
fi

if [ $1 = "development" ]; then
    echo "Upgrade: $1"
    helm upgrade --values values.yaml --set loadBalancer=development.dataprofiler.com dp-cluster-proxies . -n development

elif [ $1 = "preview" ]; then
    echo "Upgrade: $1"
    helm upgrade --values values.yaml --set loadBalancer=preview.dataprofiler.com dp-cluster-proxies . -n preview

elif [ $1 = "production" ]; then
    echo "Upgrade: $1"
    helm upgrade --values values.yaml --set loadBalancer=dataprofiler.com dp-cluster-proxies . -n production

else
    echo "Unknown argument $1"
    usage
    exit -1
fi
