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

kubectl proxy
#kubectl proxy --accept-hosts='.*' --address=0.0.0.0 &

# kubectl port-forward -n monitoring service/grafana 3000:3000 --address 0.0.0.0
# kubectl port-forward -n kube-system service/kubernetes-dashboard 10443:443 --address 0.0.0.0
# kubectl port-forward service/dmk-spark-livy 8998:8998 --address 0.0.0.0
# kubectl port-forward service/dmk-spark-webui 8080:8080 --address 0.0.0.0
