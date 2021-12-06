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
from requests import post as reqpost
from os import getenv

BASE_API_PATH = getenv('USER_FACING_API_HTTP_PATH', 'http://dp-api:9000')
AUTH_HEADERS = {'X-Username': 'test-developer', 'X-Api-Key': getenv('INTTESTPASS', 'local-developer')} 


def test_v2_datasets_graphql():
    # routes: 72
    # AccumuloControllerV2
    # POST /v2/datasets/graphql controllers.accumulo.AccumuloControllerV2.graphql(request: Request)
    # schema: graphql
    body = {
        'query': '{metadata{dataset_name dataset_display_name}}',
    }
    req_path = f'{BASE_API_PATH}/v2/datasets/graphql'
    res = reqpost(req_path, headers=AUTH_HEADERS, json=body)
    assert res.status_code == 200
    assert res.json() != {}


def test_v2_rows():
    # routes: 167
    # AccumuloControllerV2
    # POST /v2/rows controllers.accumulo.AccumuloControllerV2.datawaverows(request: Request)
    # PagedRowScanSpec: 231
    body = {
        'dataset': 'int_basic_test_data',
        'table': 'int_basic_test_data',
        #'limit': 1,  # optional
        #'pageSize': 1,  # optional
        #'filters: {}, # optional
        #'startLocation': '' . #optional
    }
    req_path = f'{BASE_API_PATH}/v2/rows'
    res = reqpost(req_path, headers=AUTH_HEADERS, json=body)
    assert res.status_code == 200
    assert res.json() != {}


def test_data_rows():
    # routes: 680
    # AccumuloControllerV2
    # POST   /data/rows        controllers.accumulo.AccumuloControllerV2.datawaverows(request: Request)
    # PagedRowScanSpec: 231
    body = {
        'dataset': 'int_basic_test_data',
        'table': 'int_basic_test_data',
        #'limit': 1,  # optional
        #'pageSize': 1,  # optional
        #'filters: {}, # optional
        #'startLocation': '' . #optional
    }
    req_path = f'{BASE_API_PATH}/data/rows'
    res = reqpost(req_path, headers=AUTH_HEADERS, json=body)
    assert res.status_code == 200
    assert res.json() != {}


def test_datawave_rows():
    # routes: 696
    # AccumuloControllerV2
    # POST   /data/datawaverows        controllers.accumulo.AccumuloControllerV2.datawaverows(request: Request)
    # PagedRowScanSpec: 231
    body = {
        'dataset': 'int_basic_test_data',
        'table': 'int_basic_test_data',
        #'limit': 1,  # optional
        #'pageSize': 1,  # optional
        #'filters: {}, # optional
        #'startLocation': '' . #optional
    }
    req_path = f'{BASE_API_PATH}/data/datawaverows'
    res = reqpost(req_path, headers=AUTH_HEADERS, json=body)
    assert res.status_code == 200
    assert res.json() != {}


if __name__ == '__main__':
    test_v2_datasets_graphql()
    test_v2_rows()
    test_data_rows()
    test_datawave_rows()
    print('Complete')
