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

#export POD_NAME=$(kubectl get pods --namespace default -l "app=prometheus,component=server" -o jsonpath="{.items[0].metadata.name}")
#kubectl --namespace default port-forward $POD_NAME 9090

#export POD_NAME=$(kubectl get pods --namespace default -l "app=grafana,release=grafana-1584628029" -o jsonpath="{.items[0].metadata.name}")
#export POD_NAME=$(kubectl get pods --namespace default -l "app.kubernetes.io/name=grafana,app.kubernetes.io/instance=grafana-1584628029" -o jsonpath="{.items[0].metadata.name}")
#kubectl --namespace default port-forward $POD_NAME 3000:80


service_name=grafana-1584628029
kubectl port-forward service/"${service_name}" --address 0.0.0.0 8083:80
