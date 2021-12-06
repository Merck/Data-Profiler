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
from requests import get as reqget
from os import getenv

BASE_API_PATH = getenv('USER_FACING_API_HTTP_PATH', 'http://dp-api:9000')
AUTH_HEADERS = {'X-Username': 'test-developer', 'X-Api-Key': getenv('INTTESTPASS', 'local-developer')}


def test_supertables():
    # routes: 1122
    # AccumuloControllerSuperTables
    # GET /supertables controllers.accumulo.AccumuloControllerSuperTables.supertables(request: Request)
    req_path = f'{BASE_API_PATH}/supertables'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    #assert res.json() != {}  # No super tables in standalone


def test_supertables_dataset():
    # routes 1137
    # AccumuloControllerSuperTables
    # GET /supertables/:dataset controllers.accumulo.AccumuloControllerSuperTables.supertablesTreemap(request: Request, dataset: String)
    dataset = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/supertables/{dataset}'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    #assert res.json() != {}  # No super tables in standalone

if __name__ == '__main__':
    test_supertables()
    test_supertables_dataset()
    print('Complete')
