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

#
# VAULT CLIENT SSH AUTH (CA)
#
ldap_password_file="$HOME/ssh_keys/password_payload.json"
public_key_file="$HOME/ssh_keys/public-key-payload.json"

echo authenticating to ldap
auth_token=$( curl -X POST --data @$ldap_password_file \
  "https://secrets.dataprofiler.com/v1/auth/ldap/login/$(whoami)" | jq -r '.auth.client_token')

if [ $? == 0 ]; then
  echo ldap authentication successful - auth_token: $auth_token
else
  echo ldap authentication failed. exiting...
  exit 1
fi

echo signing client public key ...
signed_key=$( curl -X POST \
  --header "x-vault-token: $auth_token" \
  --data @$public_key_file  \
  https://secrets.dataprofiler.com/v1/ssh-client-signer/sign/test-role | jq -r '.data.signed_key' )

if [ $? == 0 ]; then
  echo successfully signed client key - signed_key: $signed_key
else
  echo failed to sign client key. exiting...
  exit 1
fi

echo cert saved at $HOME/.ssh/id_rsa-cert.pub
echo $signed_key | sed '/^[[:space:]]*$/d' > $HOME/.ssh/id_rsa-cert.pub

echo setting permissions of signed certificate to '0600'
chmod 0600 $HOME/.ssh/id_rsa-cert.pub

