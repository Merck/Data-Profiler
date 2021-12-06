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
#from .fixtures import AUTH_HEADERS, BASE_API_PATH
from requests import get as reqget, post as reqpost, delete as reqdel
from os import getenv

BASE_API_PATH = getenv('USER_FACING_API_HTTP_PATH', 'http://dp-api:9000')
AUTH_HEADERS = {'X-Username': 'test-developer', 'X-Api-Key': getenv('INTTESTPASS', 'local-developer')}


def test_api_keys():
    # routes: 728
    # AdminAccumuloController
    # GET   /apiKeys controllers.AdminAccumuloController.apiKeys(request: Request)
    ## ASSUMES: At time of writing (9/3/21), there are 17 entries
    req_path = f'{BASE_API_PATH}/apiKeys'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert len(res.json()) != 0 


def _create_api_key(user='developer'):
    req_path = f'{BASE_API_PATH}/apiKeys/{user}'
    return reqpost(req_path, headers=AUTH_HEADERS)


def _delete_api_key(token):
    req_path = f'{BASE_API_PATH}/apiKeys/{token}'
    return reqdel(req_path, headers=AUTH_HEADERS)


def test_api_keys_create_delete():
    # routes: 743, 758
    # AdminAccumuloController
    # POST  /apiKeys/:username controllers.AdminAccumuloController.newApiKey(request: Request, username: String)
    # DELETE /apiKeys/:token controllers.AdminAccumuloController.deleteApiKey(request: Request, token: String)
    ## THIS ASSUMES THAT THERE ARE NO API KEYS
    ## THIS WILL CREATE AN API KEY FOR THE 'developer' USER
    ## THIS WILL DELETE THE CREATED API KEY ONCE CREATED
    user = 'developer'
    res = _create_api_key(user)
    assert res.status_code == 200
    assert res.json() != {}
    api_key = res.json()['token']
    res_del = _delete_api_key(api_key)
    assert res_del.status_code == 200
    assert res_del.json() == True


if __name__ == '__main__':
    test_api_keys()
    test_api_keys_create_delete()
    print('Complete')