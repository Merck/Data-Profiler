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

# SSH with cert and private key

[[ -z $1 ]] && printf "usage: $0 <host> <ldap_password_file> <public_key_file>" && exit 1

ssh -o "StrictHostKeyChecking=no" -i ~/.ssh/id_rsa-cert.pub -i ~/.ssh/id_rsa.pub $(whoami)@$1 < /dev/tty

if [ $? != 0 ]; then
  echo "ssh login failed; cert may have expired - requesting new RSA Cert..."

  $(pwd)/sign-client-key.sh

  if [ $? != 0 ]; then
    echo "failed to obtain RSA Cert; does '$(whoami)' still have access? exiting ...."
  else
    echo "successfully signed public key, reattempting authentication ...."
    ssh -o "StrictHostKeyChecking=no" -i ~/.ssh/id_rsa-cert.pub -i ~/.ssh/id_rsa.pub $(whoami)@$1 < /dev/tty
  fi
fi
