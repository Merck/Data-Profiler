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

# set -x

function retrieve_vault_config() {
  local vault_address=${1:-noaddress_defined}
  local config=${2:-noconfig_defined}
  local vault_token=${3:-notoken_defined}
  local json=$(curl --silent "${vault_address}"/v1/kv/data/configs/"${config}" -H "X-Vault-Token: ${vault_token}" | jq ".data.data")
  echo "${json}"
}

config=${1}
token=${2}
payload=$(retrieve_vault_config "https://secrets.dataprofiler.com" "${config}" "${token}" $@)

echo "${payload}" > "${config}.json"