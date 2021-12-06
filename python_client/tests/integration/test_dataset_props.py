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


def _get_ds_properties(ds):
    req_path = f'{BASE_API_PATH}/{ds}/properties'
    return reqget(req_path, headers=AUTH_HEADERS)


def test_dataset_properties_get():
    # routes: 1016
    # AccumuloControllerDatasetProp
    # GET /:dataset/properties controllers.accumulo.AccumuloControllerDatasetProp.getDatasetProperties(request: Request, dataset: String)
    dataset = 'int_basic_test_data'
    res = _get_ds_properties(dataset)
    assert res.status_code == 200
    jres = res.json()  # If the post is run, then the matching fails as something may have changed
    assert jres['origin'] == 'upload'  # This fixes that


def test_dataset_properties_post():
    # routes: 1031
    # AccumuloControllerDatasetProp
    # POST /:dataset/properties controllers.accumulo.AccumuloControllerDatasetProp.setDatasetProperties(request: Request, dataset: String)
    dataset = 'int_basic_test_data'
    body = {'type': 'chicken'}
    req_path = f'{BASE_API_PATH}/{dataset}/properties'
    res = reqpost(req_path, headers=AUTH_HEADERS, json=body)
    assert res.status_code == 200
    gres = _get_ds_properties(dataset)
    assert gres.status_code == 200
    gres_json = gres.json()
    assert gres_json['origin'] == 'upload'
    assert gres_json['type'] == 'chicken'


def _get_ds_t_properties(ds, table):
    req_path = f'{BASE_API_PATH}/{ds}/{table}/properties'
    return reqget(req_path, headers=AUTH_HEADERS)


def test_dataset_tables_properties_get():
    # routes: 1046
    # AccumuloControllerDatasetProp
    # GET /:dataset/:table/properties controllers.accumulo.AccumuloControllerDatasetProp.getTableProperties(request: Request, dataset: String, table: String)
    dataset = 'int_basic_test_data'
    table = 'int_basic_test_data'
    res = _get_ds_t_properties(dataset, table)
    assert res.status_code == 200
    jres = res.json()  # If the post is run, then the matching fails as something may have changed
    assert jres['origin'] == 'upload'  # This fixes that


def test_dataset_tables_properties_post():
    # routes: 1062
    # AccumuloControllerDatasetProp
    # POST /:dataset/:table/properties controllers.accumulo.AccumuloControllerDatasetProp.getTableProperties(request: Request, dataset: String, table: String)
    dataset = 'int_basic_test_data'
    table = 'int_basic_test_data'
    body = {'type': 'chicken'}
    req_path = f'{BASE_API_PATH}/{dataset}/{table}/properties'
    res = reqpost(req_path, headers=AUTH_HEADERS, json=body)
    assert res.status_code == 200
    gres = _get_ds_t_properties(dataset, table)
    assert gres.status_code == 200
    assert gres.json() == {**{'origin': 'upload'}, **body}


def _get_ds_t_c_properties(dataset, table, column):
    req_path = f'{BASE_API_PATH}/{dataset}/{table}/{column}/properties'
    return reqget(req_path, headers=AUTH_HEADERS)

def test_dataset_table_column_properties_get():
    # routes: 1077
    #  AccumuloControllerDatasetProp
    # GET /:dataset/:table/:column/properties controllers.accumulo.AccumuloControllerDatasetProp.getColumnProperties(request: Request, dataset: String, table: String, column: String) 
    dataset = 'int_cell_level_visibilities_test_data'
    table = 'int_cell_level_visibilities_test_data'
    column = 'Salary'
    res = _get_ds_t_c_properties(dataset, table, column)
    assert res.status_code == 200
    jres = res.json()  # If the post is run, then the matching fails as something may have changed
    assert jres['origin'] == 'upload'  # This fixes that


def test_dataset_table_column_properties_post():
    # routes: 1092
    #  AccumuloControllerDatasetProp
    # POST /:dataset/:table/:column/properties controllers.accumulo.AccumuloControllerDatasetProp.getColumnProperties(request: Request, dataset: String, table: String, column: String) 
    dataset = 'int_cell_level_visibilities_test_data'
    table = 'int_cell_level_visibilities_test_data'
    column = 'Salary'
    body = {'type': 'chicken'}
    req_path = f'{BASE_API_PATH}/{dataset}/{table}/{column}/properties'
    res = reqpost(req_path, headers=AUTH_HEADERS, json=body)
    assert res.status_code == 200
    gres = _get_ds_t_c_properties(dataset, table, column)
    assert gres.status_code == 200
    assert gres.json() == {**{'origin': 'upload'}, **body}


if __name__ == '__main__':
    test_dataset_properties_get()
    test_dataset_properties_post()
    test_dataset_tables_properties_get()
    test_dataset_tables_properties_post()
    test_dataset_table_column_properties_get()
    test_dataset_table_column_properties_post()
    print('Complete')
