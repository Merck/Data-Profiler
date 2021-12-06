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

#kubectl annotate pods --selector='app==netchecker-server' \
#	prometheus.io/scrape=true prometheus.io/port=8081 --overwrite

# call this user facing endpoint, first you will need to proxy the nodeport service
#  curl http://localhost:8085/api/v1/connectivity_check
kubectl port-forward service/netchecker-service --address 0.0.0.0 8085:8081
