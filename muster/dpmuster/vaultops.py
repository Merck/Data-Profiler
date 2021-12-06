"""
  Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
 
 	Licensed to the Apache Software Foundation (ASF) under one
 	or more contributor license agreements. See the NOTICE file
 	distributed with this work for additional information
 	regarding copyright ownership. The ASF licenses this file
 	to you under the Apache License, Version 2.0 (the
 	"License"); you may not use this file except in compliance
 	with the License. You may obtain a copy of the License at
 
 	http://www.apache.org/licenses/LICENSE-2.0
 
 
 	Unless required by applicable law or agreed to in writing,
 	software distributed under the License is distributed on an
 	"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 	KIND, either express or implied. See the License for the
 	specific language governing permissions and limitations
 	under the License.
"""
from typing import List

import requests
import dpmuster.constants as constants


class VaultOpsException(Exception):
    pass


class VaultOps:

    def __init__(self):
        self.url = "{}/v1/".format(constants.VAULT_PATH)
        self.headers = None

    def login(self):

        payload = {'password': constants.AUTH.password}
        url = "{}auth/ldap/login/{}".format(self.url, constants.AUTH.username)
        r = requests.post(url, json=payload)
        try:
            self.headers = {'Authorization': 'Bearer {}'.format(r.json()['auth']['client_token'])}
        except:
            msg = 'Cannot login to vault - Could be bad username/password or vault is sealed'
            print(msg)
            raise VaultOpsException(msg)

    def list_keys(self, root) -> List[str]:
        self.login()

        url = "{}kv/metadata/{}".format(self.url, root)
        r = requests.request("LIST", url, headers=self.headers)
        if not r.ok:
            raise VaultOpsException(r.content)
        try:
            return r.json()["data"]["keys"]
        except:
            raise VaultOpsException("Failed to list from: " + root)

    def available_configs(self):
        keys = self.list_keys("configs")

        envs = [x for x in keys if not x.endswith("/")]
        return envs

    def decrypt(self, env):
        self.login()
        url = "{}kv/data/{}".format(self.url, env)
        r = requests.get(url, headers=self.headers)
        try:
            return r.json()['data']
        except:
            msg = "Cannot get vars from vault"
            print(msg)
            raise VaultOpsException(msg)

    def decrypt_and_get_key(self, path, key):
        val = self.decrypt(path)
        return val['data'][key]

    def decrypt_and_write(self, env):
        result_dict = self.decrypt(env)
        if 'data' in result_dict:
            result_dict = self.unwrap_data_elements(result_dict)
        # For ssh keys or "standalone files", we are abusing the key value store, so we have named keys
        if 'type' in result_dict and result_dict['type'] == 'file' and 'contents' in result_dict:
            with open(env, 'w') as fd:
                fd.write(result_dict['contents'])
        # The else branch will likely be an environment config
        else:
            out = ""
            for key, value in result_dict.items():
                out = out + key + "=" + value + "\n"
            with open(env, 'w') as fd:
                fd.write(out)

    def encrypt(self, path, namespace):
        self.login()
        fd = open(path)
        contents = fd.read()
        fd.close()
        url = "{}kv/data/{}".format(self.url, namespace)
        payload = {'data': {'data': {'type': 'file', 'help': 'This secret represents a "secret" file. Do not add any other keys to this secret. Use muster encrypt-vault', 'contents': contents}}}
        r = requests.post(url, headers=self.headers, json=payload)
        if r.ok:
            return True
        else:
            print("Cannot set vars from vault")
            raise VaultOpsException()

    def sign_ssh_key(self, path, signer_role):
        self.login()
        fd = open(path)
        contents = fd.read()
        fd.close()
        url = "{}ssh-client-signer/sign/{}".format(self.url, signer_role)
        payload = {
            'public_key': contents.strip('\n'),
            'valid_principals': constants.AUTH.username,
            'cert_type': 'user'
        }
        r = requests.post(url, headers=self.headers, json=payload)
        try:
            return r.json()['data']['signed_key']
        except:
            print("Failed to obtain certificate from vault")
            raise VaultOpsException()


    '''
     recurse and unwrap all the nested data elements, if any exists
    '''
    def unwrap_data_elements(self, json):
        if not json:
            return json
        return self.unwrap_data_elements(json['data']) if 'data' in json else json
