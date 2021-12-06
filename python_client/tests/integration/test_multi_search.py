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


def test_multi_search():
    # routes: 803
    # AccumuloControllerMultiSearch
    # POST /multi-search     controllers.accumulo.AccumuloControllerMultiSearch.multiSearch(request: Request)
    body = {
        # Type should be auto set?
        'level': 'global',
        'groupingLevel': 'global',
        'dataset': 'int_basic_test_data',
        'table': 'int_basic_test_data', 
        'column': 'State name', 
        'term': ['Texas'],
        'begins_with': False,
        'limit': 2,
        'filters': {},
        #'filters': {'': ['']},
        'columnOrder': [''],
        'substring_match': False,
        'v2Query': '',
        'pagesize': 1,
        'startlocation': '',
        'returnFullValues': False,
        'returnVisibilities': False
    }
    req_path = f'{BASE_API_PATH}/multi-search'
    res = reqpost(req_path, headers=AUTH_HEADERS, json=body)
    assert res.status_code == 200
    assert res.json() != {}


if __name__ == '__main__':
    test_multi_search()
    print('Complete')