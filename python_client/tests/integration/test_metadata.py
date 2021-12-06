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


def test_metadata_dataset_valid_dataset():
    # routes: 28
    # AccumuloControllerMetadata
    # GET /metadata/:dataset controllers.accumulo.AccumuloControllerMetadata.metadata(request: Request, dataset: String)
    dataset = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/metadata/{dataset}'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_metadata_versions():
    # routes: 1171
    # AccumuloControllerMetadata
    # GET /metadata/versions controllers.accumulo.AccumuloControllerMetadata.metadataVersions(request: Request)
    req_path = f'{BASE_API_PATH}/metadata/versions'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_metadata_version_version():
    # routes: 1186
    # AccumuloControllerMetadata
    # GET /metadata/version/:version controllers.accumulo.AccumuloControllerMetadata.metadataVersion(request: Request, version: String)
    version = '23037ac5-447a-40b7-b7eb-121e2764af19'
    req_path = f'{BASE_API_PATH}/metadata/version/{version}'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


#def test_metadata_version_history_max_max():
#    # NOTE: This hangs up seemingly indefinitely
#    # routes: 1201
#    # MetadataVersionHistoryController
#    # GET /metadata/version/history/max/:max controllers.MetadataVersionHistoryController.metadataVersionHistory(request: Request, max: Integer)
#    max = 1
#    req_path = f'{BASE_API_PATH}/metadata/version/history/max/{max}'
#    res = reqget(req_path, headers=AUTH_HEADERS)
#    assert res.status_code == 200
#    assert res.json() != {}


#def test_metadata_version_history_dataset_dataset_max_max():
#    # NOTE: This hangs up seemingly indefinitely
#    # routes: 1216
#    # MetadataVersionHistoryController
#    # GET /metadata/version/history/dataset/:dataset/max/:max controllers.MetadataVersionHistoryController.metadataVersionHistoryByDataset(request: Request, dataset: String, max: Integer)
#    dataset = 'int_basic_test_data'
#    max = 1
#    req_path = f'{BASE_API_PATH}/metadata/version/history/dataset/{dataset}/max/{max}'
#    res = reqget(req_path, headers=AUTH_HEADERS)
#    assert res.status_code == 200
#    assert res.json() != {}


if __name__ == '__main__':
    test_metadata_dataset_valid_dataset()
    test_metadata_versions()
    test_metadata_version_version()
#    test_metadata_version_history_max_max()
#    test_metadata_version_history_dataset_dataset_max_max()
    print('Complete')