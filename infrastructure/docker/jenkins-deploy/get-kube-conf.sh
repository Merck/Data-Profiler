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

CONFIG_PATH='/root/.kube/config'
mkdir -p /root/.kube
curl --silent $VAULT_ADDR/v1/kv/data/configs/kubernetes -H "X-Vault-Token: $VAULT_TOKEN" | jq ".data.data.data" | sed -e 's/^"//' -e 's/"$//' | base64 --decode > $CONFIG_PATH
chmod 777 $CONFIG_PATH
echo "Set Kubernetes Config to $CONFIG_PATH"