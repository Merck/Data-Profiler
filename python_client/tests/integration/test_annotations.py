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


def test_annotations_datasets():
    # routes: 1231
    # CustomAnnotationController
    # GET /annotations/datasets controllers.CustomAnnotationController.listAllDatasetAnnotations(request: Request, max: java.util.Optional[Integer])
    req_path = f'{BASE_API_PATH}/annotations/datasets'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_tables():
    # routes: 1246
    # CustomAnnotationController
    # GET /annotations/tables controllers.CustomAnnotationController.listAllTableAnnotations(request: Request, max: java.util.Optional[Integer])
    req_path = f'{BASE_API_PATH}/annotations/tables'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_columns():
    # routes: 1261
    # CustomAnnotationController
    # GET /annotations/columns controllers.CustomAnnotationController.listAllColumnAnnotations(request: Request, max: java.util.Optional[Integer])
    req_path = f'{BASE_API_PATH}/annotations/columns'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_hierarchy_dataset_dataset():
    # routes: 1276
    # CustomAnnotationController
    # GET /annotations/hierarchy/dataset/:dataset controllers.CustomAnnotationController.listDatasetHierarchyAnnotations(request: Request, dataset: String, max: java.util.Optional[Integer])
    dataset = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/annotations/hierarchy/dataset/{dataset}'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_hierarchy_dataset_dataset_table_table():
    # routes: 1291
    # CustomAnnotationController
    # GET /annotations/hierarchy/dataset/:dataset/table/:table controllers.CustomAnnotationController.listTableHierarchyAnnotations(request: Request, dataset: String, table: String, max: java.util.Optional[Integer])
    dataset = 'int_basic_test_data'
    table = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/annotations/hierarchy/dataset/{dataset}/table/{table}'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_dataset_dataset():
    # routes: 1306
    # CustomAnnotationController
    # GET /annotations/dataset/:dataset controllers.CustomAnnotationController.listDatasetAnnotations(request: Request, dataset: String, max: java.util.Optional[Integer])
    dataset = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/annotations/dataset/{dataset}'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_dataset_dataset_table_table():
    # routes: 1321
    # CustomAnnotationController
    # GET /annotations/dataset/:dataset/table/:table controllers.CustomAnnotationController.listTableAnnotations(request: Request, dataset: String, table: String, max: java.util.Optional[Integer])
    dataset = 'int_basic_test_data'
    table = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/annotations/dataset/{dataset}/table/{table}'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_dataset_dataset_table_table_column_column():
    # routes: 1336
    # CustomAnnotationController
    # GET /annotations/dataset/:dataset/table/:table/column/:column controllers.CustomAnnotationController.listColumnAnnotations(request: Request, dataset: String, table: String, column: String, max: java.util.Optional[Integer])
    dataset = 'int_cell_level_visibilities_test_data'
    table = 'int_cell_level_visibilities_test_data'
    column = 'Location'
    req_path = f'{BASE_API_PATH}/annotations/dataset/{dataset}/table/{table}/column/{column}'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_system_datasets():
    # routes: 1352
    # CustomAnnotationController
    # GET /annotations/system/datasets controllers.CustomAnnotationController.listAllDatasetSystemAnnotations(request: Request, max: java.util.Optional[Integer])
    req_path = f'{BASE_API_PATH}/annotations/system/datasets'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_system_tables():
    # routes: 1367
    # CustomAnnotationController
    # GET /annotations/system/tables controllers.CustomAnnotationController.listAllTableSystemAnnotations(request: Request, max: java.util.Optional[Integer])
    req_path = f'{BASE_API_PATH}/annotations/system/tables'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_system_columns():
    # routes: 1382
    # CustomAnnotationController
    # GET /annotations/system/columns controllers.CustomAnnotationController.listAllColumnSystemAnnotations(request: Request, max: java.util.Optional[Integer])
    req_path = f'{BASE_API_PATH}/annotations/system/columns'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_system_hierarchy_dataset_dataset():
    # routes: 1397
    # CustomAnnotationController
    # GET /annotations/system/hierarchy/dataset/:dataset controllers.CustomAnnotationController.listDatasetHierarchySystemAnnotations(request: Request, dataset: String, max: java.util.Optional[Integer])
    dataset = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/annotations/system/hierarchy/dataset/{dataset}'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_system_hierarchy_dataset_dataset_table_table():
    # routes: 1412
    # CustomAnnotationController
    # GET /annotations/system/hierarchy/dataset/:dataset/table/:table controllers.CustomAnnotationController.listTableHierarchySystemAnnotations(request: Request, dataset: String, table: String, max: java.util.Optional[Integer])
    dataset = 'int_basic_test_data'
    table = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/annotations/system/hierarchy/dataset/{dataset}/table/{table}'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_system_dataset_dataset():
    # routes: 1427
    # CustomAnnotationController
    # GET /annotations/system/dataset/:dataset controllers.CustomAnnotationController.listDatasetSystemAnnotations(request: Request, dataset: String, max: java.util.Optional[Integer])
    dataset = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/annotations/system/dataset/{dataset}'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_system_dataset_dataset_table_table():
    # route: 1442
    # CustomAnnotationController
    # GET /annotations/system/dataset/:dataset/table/:table controllers.CustomAnnotationController.listTableSystemAnnotations(request: Request, dataset: String, table: String, max: java.util.Optional[Integer])
    dataset = 'int_basic_test_data'
    table = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/annotations/system/dataset/{dataset}/table/{table}'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_system_dataset_dataset_table_table_column_column():
    # route: 1457
    # CustomAnnotationController
    # GET /annotations/system/dataset/:dataset/table/:table/column/:column controllers.CustomAnnotationController.listColumnSystemAnnotations(request: Request, dataset: String, table: String, column: String, max: java.util.Optional[Integer])
    dataset = 'int_cell_level_visibilities_test_data'
    table = 'int_cell_level_visibilities_test_data'
    column = 'Location'
    req_path = f'{BASE_API_PATH}/annotations/system/dataset/{dataset}/table/{table}/column/{column}'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_tour_datasets():
    # route: 1473
    # CustomAnnotationController
    # GET /annotations/tour/datasets controllers.CustomAnnotationController.listAllDatasetTourAnnotations(request: Request, max: java.util.Optional[Integer])
    req_path = f'{BASE_API_PATH}/annotations/tour/datasets'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_tour_tables():
    # route: 1488
    # CustomAnnotationController
    # GET /annotations/tour/tables controllers.CustomAnnotationController.listAllTableTourAnnotations(request: Request, max: java.util.Optional[Integer])
    req_path = f'{BASE_API_PATH}/annotations/tour/tables'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_tour_columns():
    # route: 1503
    # CustomAnnotationController
    # GET /annotations/tour/columns controllers.CustomAnnotationController.listAllColumnTourAnnotations(request: Request, max: java.util.Optional[Integer])
    req_path = f'{BASE_API_PATH}/annotations/tour/columns'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_tour_hierarchy_dataset_dataset():
    # route: 1518
    # CustomAnnotationController
    # GET /annotations/tour/hierarchy/dataset/:dataset controllers.CustomAnnotationController.listDatasetHierarchyTourAnnotations(request: Request, dataset: String, max: java.util.Optional[Integer])
    dataset = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/annotations/tour/hierarchy/dataset/{dataset}'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_tour_heirarchy_dataset_dataset_table_table():
    # route: 1533
    # CustomAnnotationController
    # GET /annotations/tour/hierarchy/dataset/:dataset/table/:table controllers.CustomAnnotationController.listTableHierarchyTourAnnotations(request: Request, dataset: String, table: String, max: java.util.Optional[Integer])
    dataset = 'int_basic_test_data'
    table = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/annotations/tour/hierarchy/dataset/{dataset}/table/{table}'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_tour_dataset_dataset():
    # route: 1548
    # CustomAnnotationController
    # GET /annotations/tour/dataset/:dataset controllers.CustomAnnotationController.listDatasetTourAnnotations(request: Request, dataset: String, max: java.util.Optional[Integer])
    dataset = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/annotations/tour/dataset/{dataset}'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_tour_dataset_dataset_table_table():
    # route: 1563
    # CustomAnnotationController
    # GET /annotations/tour/dataset/:dataset/table/:table controllers.CustomAnnotationController.listTableTourAnnotations(request: Request, dataset: String, table: String, max: java.util.Optional[Integer])
    dataset = 'int_basic_test_data'
    table = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/annotations/tour/dataset/{dataset}/table/{table}'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_tour_dataset_dataset_table_table_column_column():
    # route: 1578
    # CustomAnnotationController
    # GET /annotations/tour/dataset/:dataset/table/:table/column/:column controllers.CustomAnnotationController.listColumnTourAnnotations(request: Request, dataset: String, table: String, column: String, max: java.util.Optional[Integer])
    dataset = 'int_cell_level_visibilities_test_data'
    table = 'int_cell_level_visibilities_test_data'
    column = 'Location'
    req_path = f'{BASE_API_PATH}/annotations/tour/dataset/{dataset}/table/{table}/column/{column}'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_dataset_counts():
    # route: 1594
    # CustomAnnotationController
    # GET /annotations/datasets/counts controllers.CustomAnnotationController.countAllDatasetAnnotations(request: Request)
    req_path = f'{BASE_API_PATH}/annotations/dataset/counts'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_tables_counts():
    # route: 1609
    # CustomAnnotationController
    # GET /annotations/tables/counts controllers.CustomAnnotationController.countAllTableAnnotations(request: Request)
    req_path = f'{BASE_API_PATH}/annotations/tables/counts'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_columns_counts():
    # route: 1624
    # CustomAnnotationController
    # GET /annotations/columns/counts controllers.CustomAnnotationController.countAllColumnAnnotations(request: Request)
    req_path = f'{BASE_API_PATH}/annotations/columns/counts'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_counts():
    # route: 1639
    # CustomAnnotationController
    # GET /annotations/counts controllers.CustomAnnotationController.countAllAnnotations(request: Request)
    req_path = f'{BASE_API_PATH}/annotations/counts'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_hierarchy_dataset_dataset_counts():
    # route: 1654
    # CustomAnnotationController
    # GET /annotations/hierarchy/dataset/:dataset/counts controllers.CustomAnnotationController.countAnnotationsForDatasetHierarchy(request: Request, dataset: String)
    dataset = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/annotations/hierarchy/dataset/{dataset}/counts'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_hierarchy_table_dataset_table_counts():
    # routes: 1669
    # CustomAnnotationController
    # GET /annotations/hierarchy/table/:dataset/:table/counts controllers.CustomAnnotationController.countAnnotationsForTableHierarchy(request: Request, dataset: String, table: String)
    dataset = 'int_basic_test_data'
    table = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/annotations/hierarchy/table/{dataset}/{table}/counts'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_dataset_dataset_counts():
    # routes: 1684
    # CustomAnnotationController
    # GET /annotations/dataset/:dataset/counts controllers.CustomAnnotationController.countAnnotationsForDataset(request: Request, dataset: String)
    dataset = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/annotations/dataset/{dataset}/counts'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_table_dataset_table_counts():
    # routes: 1699
    # CustomAnnotationController
    # GET /annotations/table/:dataset/:table/counts controllers.CustomAnnotationController.countAnnotationsForTable(request: Request, dataset: String, table: String)
    dataset = 'int_basic_test_data'
    table = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/annotations/table/{dataset}/{table}/counts'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_column_dataset_table_column_counts():
    # routes: 1714
    # CustomAnnotationController
    # GET /annotations/column/:dataset/:table/:column/counts controllers.CustomAnnotationController.countAnnotationsForColumn(request: Request, dataset: String, table: String, column: String)
    dataset = 'int_cell_level_visibilities_test_data'
    table = 'int_cell_level_visibilities_test_data'
    column = 'Location'
    req_path = f'{BASE_API_PATH}/annotations/column/{dataset}/{table}/{column}/counts'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_system_datasets_counts():
    # routes: 1730
    # CustomAnnotationController
    # GET /annotations/system/datasets/counts controllers.CustomAnnotationController.countAllDatasetSystemAnnotations(request: Request)
    req_path = f'{BASE_API_PATH}/annotations/system/dataset/counts'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_system_tables_counts():
    # routes: 1745
    # CustomAnnotationController
    # GET /annotations/system/tables/counts controllers.CustomAnnotationController.countAllTableSystemAnnotations(request: Request)
    req_path = f'{BASE_API_PATH}/annotations/system/tables/counts'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_system_columns_counts():
    # routes: 1760
    # CustomAnnotationController
    # GET /annotations/system/columns/counts controllers.CustomAnnotationController.countAllColumnSystemAnnotations(request: Request)
    req_path = f'{BASE_API_PATH}/annotations/system/columns/counts'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_system_counts():
    # routes: 1775
    # CustomAnnotationController
    # GET /annotations/system/counts controllers.CustomAnnotationController.countAllSystemAnnotations(request: Request)
    req_path = f'{BASE_API_PATH}/annotations/system/counts'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_system_hierarchy_dataset_dataset_counts():
    # routes: 1790
    # CustomAnnotationController
    # GET /annotations/system/hierarchy/dataset/:dataset/counts controllers.CustomAnnotationController.countSystemAnnotationsForDatasetHierarchy(request: Request, dataset: String)
    dataset = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/annotations/system/hierarchy/dataset/{dataset}/counts'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_systems_hierarchy_table_dataset_table_counts():
    # routes: 1805
    # CustomAnnotationController
    # GET /annotations/system/hierarchy/table/:dataset/:table/counts controllers.CustomAnnotationController.countSystemAnnotationsForTableHierarchy(request: Request, dataset: String, table: String)
    dataset = 'int_basic_test_data'
    table = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/annotations/system/hierarchy/table/{dataset}/{table}/counts'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_system_dataset_dataset_counts():
    # routes: 1820
    # CustomAnnotationController
    # GET /annotations/system/dataset/:dataset/counts controllers.CustomAnnotationController.countSystemAnnotationsForDataset(request: Request, dataset: String)
    dataset = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/annotations/system/dataset/{dataset}/counts'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_system_table_dataset_table_counts():
    # routes: 1835
    # CustomAnnotationController
    # GET /annotations/system/table/:dataset/:table/counts controllers.CustomAnnotationController.countSystemAnnotationsForTable(request: Request, dataset: String, table: String)
    dataset = 'int_basic_test_data'
    table = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/annotations/system/table/{dataset}/{table}/counts'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_system_column_dataset_table_column_counts():
    # routes: 1850
    # CustomAnnotationController
    # GET /annotations/system/column/:dataset/:table/:column/counts controllers.CustomAnnotationController.countSystemAnnotationsForColumn(request: Request, dataset: String, table: String, column: String)
    dataset = 'cell_level_visibilities'
    table = 'cell_level_visibilities'
    column = 'Location'
    req_path = f'{BASE_API_PATH}/annotations/system/column/{dataset}/{table}/{column}/counts'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_tour_datasets_counts():
    # routes: 1866
    # CustomAnnotationController
    # GET /annotations/tour/datasets/counts controllers.CustomAnnotationController.countAllDatasetTourAnnotations(request: Request)
    req_path = f'{BASE_API_PATH}/annotations/tour/dataset/counts'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_tour_tables_counts():
    # routes: 1881
    # CustomAnnotationController
    # GET /annotations/tour/tables/counts controllers.CustomAnnotationController.countAllTableTourAnnotations(request: Request)
    req_path = f'{BASE_API_PATH}/annotations/tour/tables/counts'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_tour_columns_counts():
    # routes: 1896
    # CustomAnnotationController
    # GET /annotations/tour/columns/counts controllers.CustomAnnotationController.countAllColumnTourAnnotations(request: Request)
    req_path = f'{BASE_API_PATH}/annotations/tour/columns/counts'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_tour_counts():
    # routes: 1911
    # CustomAnnotationController
    # GET /annotations/tour/counts controllers.CustomAnnotationController.countAllTourAnnotations(request: Request)
    req_path = f'{BASE_API_PATH}/annotations/tour/counts'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_tour_hierarchy_dataset_dataset_counts():
    # routes: 1926
    # CustomAnnotationController
    # GET /annotations/tour/hierarchy/dataset/:dataset/counts controllers.CustomAnnotationController.countTourAnnotationsForDatasetHierarchy(request: Request, dataset: String)
    dataset = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/annotations/tour/hierarchy/dataset/{dataset}/counts'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_tour_hierarchy_table_dataset_table_counts():
    # routes: 1941
    # CustomAnnotationController
    # GET /annotations/tour/hierarchy/table/:dataset/:table/counts controllers.CustomAnnotationController.countTourAnnotationsForTableHierarchy(request: Request, dataset: String, table: String)
    dataset = 'int_basic_test_data'
    table = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/annotations/tour/hierarchy/table/{dataset}/{table}/counts'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_tour_dataset_dataset_counts():
    # routes: 1956
    # CustomAnnotationController
    # GET /annotations/tour/dataset/:dataset/counts controllers.CustomAnnotationController.countTourAnnotationsForDataset(request: Request, dataset: String)
    dataset = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/annotations/tour/dataset/{dataset}/counts'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_tour_table_dataset_table_counts():
    # routes: 1971
    # CustomAnnotationController
    # GET /annotations/tour/table/:dataset/:table/counts controllers.CustomAnnotationController.countTourAnnotationsForTable(request: Request, dataset: String, table: String)
    dataset = 'int_basic_test_data'
    table = 'int_basic_test_data'
    req_path = f'{BASE_API_PATH}/annotations/tour/table/{dataset}/{table}/counts'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_tour_column_dataset_table_column_counts():
    # routes: 1986
    # CustomAnnotationController
    # GET /annotations/tour/column/:dataset/:table/:column/counts controllers.CustomAnnotationController.countTourAnnotationsForColumn(request: Request, dataset: String, table: String, column: String)
    dataset = 'int_cell_level_visibilities_test_data'
    table = 'int_cell_level_visibilities_test_data'
    column = 'Location'
    req_path = f'{BASE_API_PATH}/annotations/tour/column/{dataset}/{table}/{column}/counts'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def _create_dataset_annotation(dataset):
    body = {
        'dataset': dataset,
        'note': 'DS_ANNO',
        'annotationType': 'dataset',
        'isDataTour': False
    }
    req_path = f'{BASE_API_PATH}/annotations'
    return reqpost(req_path, headers=AUTH_HEADERS, json=body)


def _create_table_annotation(dataset, table):
    body = {
        'dataset': dataset,
        'table': table,
        'note': 'T_ANNO',
        'annotationType': 'table',
        'isDataTour': False
    }
    req_path = f'{BASE_API_PATH}/annotations'
    return reqpost(req_path, headers=AUTH_HEADERS, json=body)


def _create_column_annotation(dataset, table, column):
    body = {
        'dataset': dataset, 
        'table': table, 
        'column': column,
        'note': 'C_ANNO',
        'annotationType': 'column',
        'isDataTour': False
    }
    req_path = f'{BASE_API_PATH}/annotations'
    return reqpost(req_path, headers=AUTH_HEADERS, json=body)


def test_annotations_create_dataset():
    # routes: 2047
    # CustomAnnotationController
    # POST /annotations controllers.CustomAnnotationController.create(request: Request)
    dataset = 'int_basic_test_data'
    res = _create_dataset_annotation(dataset)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_create_table():
    # routes: 2047
    # CustomAnnotationController
    # POST /annotations controllers.CustomAnnotationController.create(request: Request)
    dataset = 'int_basic_test_data'
    table = 'int_basic_test_data'
    res = _create_table_annotation(dataset, table)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_create_column():
    # routes: 2047
    # CustomAnnotationController
    # POST /annotations controllers.CustomAnnotationController.create(request: Request)
    dataset = 'int_cell_level_visibilities_test_data'
    table = 'int_cell_level_visibilities_test_data'
    column = 'Location'
    res = _create_column_annotation(dataset, table, column)
    assert res.status_code == 200
    assert res.json() != {}


def test_annotations_dataset_id():
    # routes: 2002
    # CustomAnnotationController
    # DELETE /annotations/:dataset/:id controllers.CustomAnnotationController.deleteDatasetAnnotation(request: Request, dataset: String, id: String)
    dataset = 'int_basic_test_data'
    created = _create_dataset_annotation(dataset)
    assert created.status_code == 200
    id = created.json()['uuid']
    req_path = f'{BASE_API_PATH}/annotations/{dataset}/{id}'
    res = reqdel(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200


def test_annotations_dataset_table_id():
    # routes: 2017
    # CustomAnnotationController
    # DELETE /annotations/:dataset/:table/:id controllers.CustomAnnotationController.deleteTableAnnotation(request: Request, dataset: String, table: String, id: String)
    dataset = 'int_basic_test_data'
    table = 'int_basic_test_data'
    created = _create_table_annotation(dataset, table)
    assert created.status_code == 200
    id = created.json()['uuid']
    req_path = f'{BASE_API_PATH}/annotations/{dataset}/{table}/{id}'
    res = reqdel(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200


def test_annotations_dataset_table_column_id():
    # routes: 2032
    # CustomAnnotationController
    # DELETE /annotations/:dataset/:table/:column/:id controllers.CustomAnnotationController.deleteColumnAnnotation(request: Request, dataset: String, table: String, column: String, id: String)
    dataset = 'int_cell_level_visibilities_test_data'
    table = 'int_cell_level_visibilities_test_data'
    column = 'Location'
    created = _create_column_annotation(dataset, table, column)
    assert created.status_code == 200
    id = created.json()['uuid']
    req_path = f'{BASE_API_PATH}/annotations/{dataset}/{table}/{column}/{id}'
    res = reqdel(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200


if __name__ == '__main__':
    test_annotations_create_dataset()
    test_annotations_create_table()
    test_annotations_create_column()
    test_annotations_datasets()
    test_annotations_dataset_id()
    test_annotations_tables()
    test_annotations_columns()
    test_annotations_hierarchy_dataset_dataset()
    test_annotations_hierarchy_dataset_dataset_table_table()
    test_annotations_dataset_dataset()
    test_annotations_dataset_dataset_table_table()
    test_annotations_dataset_dataset_table_table_column_column
    test_annotations_system_datasets()
    test_annotations_system_tables()
    test_annotations_system_columns()
    test_annotations_system_hierarchy_dataset_dataset_table_table()
    test_annotations_system_dataset_dataset()
    test_annotations_system_dataset_dataset_table_table()
    test_annotations_system_dataset_dataset_table_table_column_column()
    test_annotations_tour_datasets()
    test_annotations_tour_tables()
    test_annotations_tour_columns()
    test_annotations_tour_hierarchy_dataset_dataset()
    test_annotations_tour_heirarchy_dataset_dataset_table_table()
    test_annotations_tour_dataset_dataset()
    test_annotations_tour_dataset_dataset_table_table_column_column()
    test_annotations_dataset_counts()
    test_annotations_tables_counts()
    test_annotations_columns_counts()
    test_annotations_counts()
    test_annotations_hierarchy_dataset_dataset_counts()
    test_annotations_hierarchy_table_dataset_table_counts()
    test_annotations_dataset_dataset_counts()
    test_annotations_table_dataset_table_counts()
    test_annotations_column_dataset_table_column_counts()
    test_annotations_system_datasets_counts()
    test_annotations_system_tables_counts()
    test_annotations_system_columns_counts()
    test_annotations_system_counts()
    test_annotations_system_hierarchy_dataset_dataset_counts()
    test_annotations_systems_hierarchy_table_dataset_table_counts()
    test_annotations_system_dataset_dataset_counts()
    test_annotations_system_table_dataset_table_counts()
    test_annotations_system_column_dataset_table_column_counts()
    test_annotations_tour_datasets_counts()
    test_annotations_tour_tables_counts()
    test_annotations_tour_columns_counts()
    test_annotations_tour_counts()
    test_annotations_tour_hierarchy_dataset_dataset_counts()
    test_annotations_tour_hierarchy_table_dataset_table_counts()
    test_annotations_tour_dataset_dataset_counts()
    test_annotations_tour_table_dataset_table_counts()
    test_annotations_tour_column_dataset_table_column_counts()
    test_annotations_dataset_table_id()
    test_annotations_dataset_table_column_id()
    print('Complete')
