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
from requests import get as reqget, post as reqpost
from os import getenv

BASE_API_PATH = getenv('USER_FACING_API_HTTP_PATH', 'http://dp-api:9000')
AUTH_HEADERS = {'X-Username': 'test-developer', 'X-Api-Key': getenv('INTTESTPASS', 'local-developer')}


def _check_cust_col_resp(alias):
    assert alias['aliasType'] == 'COLUMN'
    #assert alias['dataset'] == 'basic_test_data'
    #assert alias['table'] == 'int_basic_test_data'
    assert alias['column'] == 'State Name'
    assert alias['alias'] == 'State'
    assert alias['createdBy'] == 'test-developer'


def test_custom_column_names_get():
    # routes: 985
    # ElementAliasController
    # GET /custom_column_names controllers.ElementAliasController.list(request: Request)
    req_path = f'{BASE_API_PATH}/custom_column_names'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    # ASSUMES THAT test_custom_column_names_post has been already called
    _check_cust_col_resp(res.json()[0])


def test_custom_column_names_post():
    # routes: 1000
    # ElementAliasController
    # POST /custom_column_names controllers.ElementAliasController.create(request: Request)
    body = {
        'dataset': 'int_basic_test_data',
        'table': 'int_basic_test_data',
        'column': 'State Name',
        'alias': 'State'
    }
    req_path = f'{BASE_API_PATH}/custom_column_names'
    res = reqpost(req_path, headers=AUTH_HEADERS, json=body)
    assert res.status_code == 200
    _check_cust_col_resp(res.json())

# Delete is not available on the API

if __name__ == '__main__':
    test_custom_column_names_post()
    test_custom_column_names_get()
    print('Complete')
