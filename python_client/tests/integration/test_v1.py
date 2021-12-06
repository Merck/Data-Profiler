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
from json import loads as jloads
from os import getenv

BASE_API_PATH = getenv('USER_FACING_API_HTTP_PATH', 'http://dp-api:9000')
AUTH_HEADERS = {'X-Username': 'test-developer', 'X-Api-Key': getenv('INTTESTPASS', 'local-developer')} 


def test_v1_datasets():
    # routes: 50
    # AccumuloControllerV1
    # GET /v1/datasets controllers.accumulo.AccumuloControllerV1.datasets(request: Request)
    req_path = f'{BASE_API_PATH}/v1/datasets'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_v1_tables_dataset():
    # routes: 94
    # AccumuloControllerV1
    # GET /v1/tables/:dataset controllers.accumulo.AccumuloControllerV1.tables(request: Request, dataset: String)
    dataset = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/v1/tables/{dataset}'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_v1_columns_dataset_table():
    # routes: 116
    # AccumuloControllerV1
    # GET /v1/columns/:dataset/:table  controllers.accumulo.AccumuloControllerV1.columns(request: Request, dataset: String, table: String)
    dataset = 'int_basic_test_data'
    table = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/v1/columns/{dataset}/{table}'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_v1_rows():
    # Potentially to be deprecated!
    # routes: 197
    # AccumuloControllerStreaming
    # POST /v1/rows controllers.AccumuloControllerStreaming.rowsStream(request: Request)
    # RowScanSpec: 215
    body = {
        'dataset': 'int_basic_test_data',
        'table': 'int_basic_test_data',
        #'limit': 1,  # optional
    }
    req_path = f'{BASE_API_PATH}/v1/rows'
    res = reqpost(req_path, headers=AUTH_HEADERS, json=body)
    assert res.status_code == 200
    res_text = res.text[:-1]  # There is a $ at the end of the response body
    loaded = jloads(res_text)
    assert loaded != {}


def test_v1_search():
    # routes: 254
    # AccumuloControllerV1
    # POST /v1/search controllers.accumulo.AccumuloControllerV1.search(request: Request)
    # SearchScanSpec: 144
    body = {
        'term': ['Texas'],
        #'begins_with': False,  # optional
        #'limit': 1,  # optional
        #'dataset': 'int_basic_test_data',  # optional
        #'table': 'int_basic_test_data',  # optional
    }
    req_path = f'{BASE_API_PATH}/v1/search'
    res = reqpost(req_path, headers=AUTH_HEADERS, json=body)
    assert res.status_code == 200
    assert res.json() != {}


def test_search():
    # routes: 788
    # AccumuloControllerV1
    # POST /search     controllers.accumulo.AccumuloControllerV1.search(request: Request)
    body = {
        'term': ['Texas'],
        #'begins_with': False,  # optional
        #'limit': 1,  # optional
        #'dataset': 'int_basic_test_data',  # optional
        #'table': 'int_basic_test_data',  # optional
    }
    req_path = f'{BASE_API_PATH}/v1/search'
    res = reqpost(req_path, headers=AUTH_HEADERS, json=body)
    assert res.status_code == 200
    assert res.json() != {}


def test_v1_col_counts():
    # routes: 286
    # AccumuloControllerV1
    # POST /v1/colCounts controllers.accumulo.AccumuloControllerV1.colCounts(request: Request)
    # ColumnCountSpec: 358
    body = {
        'dataset': 'int_basic_test_data',
        'table': 'int_basic_test_data',
        'column': 'State Name',
        'start': 0,
        'end': 20,
        'sort': 'CNT_ASC'  # Looks like CNT_ASC, CNT_DESC, VAL_ASC, VAL_DESC
    }
    req_path = f'{BASE_API_PATH}/v1/colCounts'
    res = reqpost(req_path, headers=AUTH_HEADERS, json=body)
    assert res.status_code == 200
    assert res.json() != {}


def test_colcounts():
    # routes: 394
    # AccumuloControllerV1
    # POST  /colcounts  controllers.accumulo.AccumuloControllerV1.colCounts(request: Request)
    body = {
        'dataset': 'int_basic_test_data',
        'table': 'int_basic_test_data',
        'column': 'State Name',
        'start': 0,
        'end': 5,
        'normalized': False,  # optional
        'return_visibilities': True  # optional
    }
    req_path = f'{BASE_API_PATH}/colcounts'
    res = reqpost(req_path, headers=AUTH_HEADERS, json=body)
    assert res.status_code == 200
    assert res.json() != {}


if __name__ == '__main__':
    test_v1_datasets()
    test_v1_tables_dataset()
    test_v1_columns_dataset_table()
    test_v1_rows()
    test_v1_search()
    test_search()
    test_v1_col_counts()
    test_colcounts()
    print('Complete')
