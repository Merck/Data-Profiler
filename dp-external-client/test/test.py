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
import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import responses
import requests
import dp_external_client as dpe
import unittest
import time
import logging
import pandas as pd

# Dataset 1 (3 Tables)
#  - Table 1 (basic_test_data) LIST.PUBLIC_DATA
#  - Table 2 (tiny_test_data_copy) LIST.PUBLIC_DATA
#  - Table 3 (basic_test_data) LIST.Nick_1
# Dataset 2 (1 Table)
#  - Table 2 (annotation_test_data_ds2) LIST.PUBLIC_DATA

ROOT_URL = 'http://playframework:9000'
API_URL = ROOT_URL + '/api'
headers = {'X-Authorization-Token': 'local-developer',
           'X-Username': 'developer'}
logging.getLogger().addHandler(logging.StreamHandler())

def wait_until_data_loaded():
    iteration = 0
    while iteration < 36:
        try:
            res = requests.get(
                url=ROOT_URL+'/concepts/datasets/100', headers=headers).json()
            if res and res['elements'] and len(res['elements'].keys()) == 2:
                break
        except:
            pass
        iteration = iteration + 1
        logging.info('Waiting for API to have loaded data')
        time.sleep(5)

def create_api_key():
    requests.post(url=ROOT_URL+'/apiKeys/developer',
                  data={}, headers=headers).json()

def get_api_key():
    res = requests.get(url=ROOT_URL+'/apiKeys', headers=headers).json()
    return str(next(key for key in res if key['username'] == 'developer')['token'])

class TestApiClient(unittest.TestCase):
    def setUp(self):
        self.api_key = str(get_api_key())
        self.env = dpe.Environment(self.api_key, ROOT_URL)

    def test_Environment_EnvironmentInfo(self):
        EnvInfo = self.env.getEnvironmentInfo()

        dataset1info = EnvInfo["Dataset 1"]
        dataset2info = EnvInfo["Dataset 2"]
        
        self.assertEqual(len(EnvInfo.keys()),2)
        self.assertEqual(dataset1info['num_tables'],3)
        self.assertEqual(dataset1info['num_columns'],26)
        self.assertEqual(dataset2info['dataset_name'], 'Dataset 2')
        self.assertEqual(dataset2info['num_values'],42)
        self.assertEqual(dataset2info['table_name'], None)

    def test_Environment_DatasetList(self):
        datasets = self.env.getDatasetList()
        self.assertIn('Dataset 1', datasets)
        self.assertIn('Dataset 2', datasets)

    def test_Environment_DatasetCount(self):
        self.assertEqual(self.env.getDatasetCount(),2)

    def test_Environment_UserAccessList(self):
        UAL = self.env.getUserAccessList(visibility = 'LIST.PUBLIC_DATA')
        self.assertEqual(UAL[0],'developer')
        
    def test_Environment_SampleJoin(self):
        dataset_a = "Dataset 1"
        table_a = "Table 1"
        column_a = "CBSA Code"
        dataset_b = "Dataset 1"
        table_b = "Table 1"
        column_b = "CBSA Code"
        sample_size = 10
        json_result = self.env.getSampleJoin(dataset_a, table_a, column_a, dataset_b, table_b, column_b, sample_size)

        self.assertEqual(json_result.shape[1],12)
        self.assertEqual(json_result.shape[0] ,100)
        #self.assertEqual(json_result['meta']['num_columns'] ,12)
        #self.assertEqual(json_result['meta']['num_rows'], 5089)


    def test_Environment_SampleJoinMetaData(self):
        dataset_a = "Dataset 2"
        table_a = "Table 2"
        column_a = "Annotation Type"
        dataset_b = "Dataset 2"
        table_b = "Table 2"
        column_b = "Annotation Type"
        sample_size = 3
        json_result = self.env.getSampleJoin(dataset_a, table_a, column_a, dataset_b, table_b, column_b, sample_size)
        self.assertEqual(json_result.shape[1],7)
        self.assertEqual(json_result.shape[0],5 )
        #self.assertEqual(json_result['meta']['num_columns'] ,7)
        #self.assertEqual(json_result['meta']['num_rows'], 14)
        

    def test_Environment_AllJobs(self):
        AJ = self.env.getAllJobs()
        self.assertEqual(AJ,{})

    # Class Dataset #
    
    def test_Dataset_DatasetInfo(self):
        dataset = "Dataset 2"
        dataset_class = dpe.Dataset(self.env, dataset)
        datasetinfo = dataset_class.getDatasetInfo()
        self.assertEqual(len(datasetinfo.keys()),1) 
        self.assertEqual(datasetinfo['Table 2']['num_columns'],7)
        self.assertEqual(datasetinfo['Table 2']['num_values'], 42)

    def test_Dataset_DatasetMetadata(self):
        dataset = "Dataset 2"
        dataset_class = dpe.Dataset(self.env, dataset)
        metadata = dataset_class.getDatasetMetadata()
        self.assertEqual(metadata['num_tables'],1)
        self.assertEqual(metadata['num_columns'],7)
        self.assertEqual(metadata['dataset_name'],'Dataset 2') 

    def test_Dataset_TableList(self):
        dataset = "Dataset 1"
        dataset_class = dpe.Dataset(self.env, dataset)
        self.assertEqual(len(dataset_class.getTableList()),2)

    def test_Dataset_UploadDate(self):
        dataset = "Dataset 2"
        dataset_class = dpe.Dataset(self.env, dataset)
        self.assertTrue(dataset_class.getUploadDate().day == pd.Timestamp.now().day or
         dataset_class.getUploadDate().day == (pd.Timestamp.now().day)-1)

    def test_Dataset_UpdateDate(self):
        dataset = "Dataset 1"
        dataset_class = dpe.Dataset(self.env, dataset)
        self.assertTrue(dataset_class.getUpdateDate().day == pd.Timestamp.now().day or
         dataset_class.getUpdateDate().day == (pd.Timestamp.now().day)-1)
        

    def test_Dataset_PullTimestamp(self):
        dataset = "Dataset 2"
        dataset_class = dpe.Dataset(self.env, dataset)       
        self.assertEqual(dataset_class.getPullTimestamp().month,
        pd.Timestamp.now().month)

    def test_Dataset_Visibility(self):
        dataset = "Dataset 1"
        dataset_class = dpe.Dataset(self.env, dataset)
        self.assertEqual(dataset_class.getVisibility(),"LIST.PUBLIC_DATA")

    def test_Dataset_TableCount(self):
        dataset = "Dataset 2"
        dataset_class = dpe.Dataset(self.env, dataset)
        self.assertEqual(dataset_class.getTableCount(),1)

    def test_Dataset_ColumnCount1(self):
        dataset = "Dataset 2"
        dataset_class = dpe.Dataset(self.env, dataset)
        self.assertEqual(dataset_class.getColumnCount(),7)

    def test_Dataset_ColumnCount2(self):
        dataset = "Dataset 1"
        dataset_class = dpe.Dataset(self.env, dataset)
        self.assertEqual(dataset_class.getColumnCount(),26) 
    
    def test_Dataset_ValueCount(self):
        dataset = "Dataset 1"
        dataset_class = dpe.Dataset(self.env, dataset)
        self.assertEqual(dataset_class.getValueCount(),45174)

    def test_Dataset_UserAccessList(self):
        dataset = "Dataset 1"
        dataset_class = dpe.Dataset(self.env, dataset)
        self.assertIn('developer', dataset_class.getUserAccessList())

    def test_Dataset_UploadCount(self):
        dataset = "Dataset 2"
        dataset_class = dpe.Dataset(self.env, dataset)
        self.assertEqual(dataset_class.getUploadCount(),0)  

    def test_Dataset_AllUploadDates(self):
        dataset = "Dataset 2"
        dataset_class = dpe.Dataset(self.env, dataset)
        self.assertEqual(dataset_class.getAllUploadDates(), [])

    def test_Dataset_loadTable(self):
        dataset = "Dataset 2"
        dataset_class = dpe.Dataset(self.env, dataset)
        table_data = dataset_class.loadTable('Table 2')
        self.assertEqual(table_data.shape[0],6)
        self.assertEqual(table_data.shape[1],7)

    def test_Dataset_importDataset(self):
        dataset = "Dataset 1"
        dataset_class = dpe.Dataset(self.env, dataset)
        imported_data = dataset_class.importDataset()
        self.assertEqual(imported_data['Table 1'].shape , (1882, 12))
        self.assertEqual(imported_data['Table 2'].shape , (3,2))


    # Class Table #
    def test_Table_TableInfo(self):
        dataset = "Dataset 1"
        table = "Table 2"
        table_class = dpe.Table(self.env, dataset, table)
        tableinfo = table_class.getTableInfo()
        self.assertIn('State Name', tableinfo.keys())
        self.assertIn('rating', tableinfo.keys())

        self.assertEqual(tableinfo['State Name']['num_values'],3)
        self.assertEqual(tableinfo['rating']['num_unique_values'],2)
    
    def test_Table_TableMetadata(self):
        dataset = "Dataset 2"
        table = "Table 2"
        table_class = dpe.Table(self.env, dataset, table)
        metadata = table_class.getTableMetadata()
        self.assertEqual(metadata['table_name'], 'Table 2')
        self.assertEqual(metadata['num_values'], 42) # Considers missing values
        self.assertEqual(metadata['num_columns'], 7)

    def test_Table_filter1(self):
        dataset = "Dataset 1"
        table = "Table 1"
        table_class = dpe.Table(self.env, dataset, table)
        table_class.setFilters({"State Name": ["California"]}) 
        load_data = table_class.loadRows()
        self.assertTrue(isinstance(load_data, pd.DataFrame))
        self.assertEqual(load_data.shape[0], 45)

    def test_Table_filter2(self):
        dataset = "Dataset 2"
        table = "Table 2"
        table_class = dpe.Table(self.env, dataset, table)
        table_class.setFilters({"Annotation Type": ["DATASET","COLUMN"]}) 
        load_data = table_class.loadRows()
        self.assertTrue(isinstance(load_data, pd.DataFrame))
        self.assertEqual(load_data.shape[0], 5)

    def test_Table_filter3(self):
        dataset = "Dataset 1"
        table = "Table 1"
        table_class = dpe.Table(self.env, dataset, table)
        table_class.setFilters({"FIPS State Code": ["53","38","56"]}) 
        load_data = table_class.loadRows()
        self.assertTrue(isinstance(load_data, pd.DataFrame))
        self.assertEqual(load_data.shape[0], 52)

    def test_Table_ColumnList(self):
        dataset = "Dataset 2"
        table = 'Table 2'
        table_class = dpe.Table(self.env, dataset, table)
        self.assertEqual(len(table_class.getColumnList()),7)

    def test_Table_UploadDate(self):
        dataset = "Dataset 2"
        table = 'Table 2'
        table_class = dpe.Table(self.env, dataset, table)
        self.assertTrue(table_class.getUploadDate().day == pd.Timestamp.now().day or 
        table_class.getUploadDate().day == (pd.Timestamp.now().day)-1)

    def test_Table_UpdateDate(self):
        dataset = "Dataset 1"
        table = 'Table 1'
        table_class = dpe.Table(self.env, dataset, table)

        self.assertTrue(table_class.getUpdateDate().day == pd.Timestamp.now().day or 
        table_class.getUpdateDate().day == (pd.Timestamp.now().day)-1)
        

    def test_Table_PullTimestamp(self):
        dataset = "Dataset 1"
        table = 'Table 2'
        table_class = dpe.Table(self.env, dataset, table)

        self.assertEqual(table_class.getPullTimestamp().month,
        pd.Timestamp.now().month)


    def test_Table_Visibility(self):
        dataset = "Dataset 1"
        table = 'Table 2'
        table_class = dpe.Table(self.env, dataset, table)
        self.assertEqual(table_class.getVisibility(), "LIST.PUBLIC_DATA")

    def test_Table_ColumnCount(self):
        dataset = "Dataset 2"
        table = 'Table 2'
        table_class = dpe.Table(self.env, dataset, table)
        self.assertEqual(table_class.getColumnCount(), 7)

    def test_Table_ValueCount(self):
        dataset = "Dataset 2"
        table = 'Table 2'
        table_class = dpe.Table(self.env, dataset, table)
        self.assertEqual(table_class.getValueCount(), 42)

    def test_Table_UserAccessList(self):
        dataset = "Dataset 1"
        table = 'Table 1'
        table_class = dpe.Table(self.env, dataset, table)
        self.assertIn('developer',table_class.getUserAccessList())
    
    def test_Table_loadRows(self):
        dataset = "Dataset 1"
        table = 'Table 1'
        table_class = dpe.Table(self.env, dataset, table)
        table_data = table_class.loadRows()
        self.assertEqual(table_data.shape,  (1882, 12))

    def test_Table_loadTableSample1(self):
        dataset = "Dataset 1"
        table = 'Table 1'
        table_class = dpe.Table(self.env, dataset, table)
        sdata = table_class.loadTableSample(sample_size=50)
        self.assertEqual(sdata.shape[0], 50)

    def test_Table_loadTableSample2(self):
        dataset = "Dataset 1"
        table = 'Table 1'
        table_class = dpe.Table(self.env, dataset, table)
        sdata = table_class.loadTableSample()
        self.assertEqual(sdata.shape[0], 100)

    def test_Table_SubstringValueMatches1(self):
        dataset = "Dataset 1"
        table = 'Table 1'
        table_class = dpe.Table(self.env, dataset, table)

        substring = 'Al'
        column = 'State Name'
        cnt = 0
        cnt_cnt = 0
        result = table_class.getSubstringValueMatches(substring,column)
        for x in result:
            if substring.lower() in x['value'].lower():
                cnt += 1
                cnt_cnt += x['count']
        
        self.assertEqual(cnt,3)
        self.assertEqual(len(result),3)
        self.assertEqual(cnt_cnt,89)

    def test_Table_SubstringValueMatches2(self):
        dataset = "Dataset 2"
        table = 'Table 2'
        table_class = dpe.Table(self.env, dataset, table)

        substring = 't1-c1'
        column = 'Column'
        cnt = 0
        cnt_cnt = 0

        result = table_class.getSubstringValueMatches(substring,column)
        for x in result:
            if substring.lower() in x['value'].lower():
                cnt += 1
                cnt_cnt += x['count']

        self.assertEqual(cnt,1)
        self.assertEqual(cnt_cnt,3)

    def test_Table_SubstringValueMatches3(self):
        dataset = "Dataset 1"
        table = 'Table 1'
        table_class = dpe.Table(self.env, dataset, table)

        substring = 23
        column = 'FIPS County Code'
        cnt = 0
        cnt_cnt = 0
        result = table_class.getSubstringValueMatches(substring,column)
        for x in result:
            if str(substring) in x['value'].lower():
                cnt += 1
                cnt_cnt += x['count']
        
        self.assertEqual(cnt,9)
        self.assertEqual(len(result),9)
        self.assertEqual(cnt_cnt,47)


    def test_Table_loadRowsWithSubstringMatches(self):
        dataset = "Dataset 1"
        table = 'Table 1'
        table_class = dpe.Table(self.env, dataset, table)
        filters = {'State Name':['Al','la'] ,'Central/Outlying County': ['Central'] }
        filtered_data = table_class.loadRowsWithSubstringMatches(filters)
        self.assertEqual(filtered_data.shape,(121, 12))

    def test_Table_loadRowsInRange1(self):
        dataset = "Dataset 1"
        table = 'Table 1'
        table_class = dpe.Table(self.env, dataset, table)
        filtered_data = table_class.loadRowsInRange(10,20,"FIPS County Code")
        self.assertEqual(filtered_data.shape[0],156)

    def test_Table_loadRowsInRange2(self):
        dataset = "Dataset 1"
        table = 'Table 1'
        table_class = dpe.Table(self.env, dataset, table)
        filtered_data = table_class.loadRowsInRange('abc&','20',"FIPS County Code")
        self.assertEqual(filtered_data,None)

    def test_Table_loadRowsWithException1(self):
        dataset = "Dataset 1"
        table = 'Table 1'
        table_class = dpe.Table(self.env, dataset, table)
        ex_filter = {'Metropolitan/Micropolitan Statistical Area' : \
                    ['Metropolitan Statistical Area']} 
        
        filtered_data = table_class.loadRowsWithException(ex_filter)
        self.assertEqual(filtered_data.shape, (646,12))

    '''def test_Table_loadRowsWithException2(self):
        dataset = "Dataset 1"
        table = 'Table 1'
        table_class = dpe.Table(self.env, dataset, table)

        ex_filter = {'FIPS County Code': ['45','47']}

        filtered_data = table_class.loadRowsWithException(ex_filter)
        self.assertEqual(filtered_data.shape, (1831,12)) '''

    def test_Table_loadRowsWithException3(self):
        dataset = "Dataset 2"
        table = 'Table 2'
        table_class = dpe.Table(self.env, dataset, table)

        ex_filter = {'Annotation Type': ['DATASET']}

        filtered_data = table_class.loadRowsWithException(ex_filter)
        self.assertEqual(filtered_data.shape[0],4)

    # def test_Table_NAscount(self):
    #     dataset = "Dataset 2"
    #     table = 'Table 2'
    #     table_class = dpe.Table(self.env, dataset, table)
    #     NAs_count = table_class.getNAscount()
    #     self.assertEqual(NAs_count['Table'],2)
    #     self.assertEqual(NAs_count['Column'],3)

    # def test_Table_Quantiles1(self):
    #     dataset = "Dataset 1"
    #     table = 'Table 1'
    #     table_class = dpe.Table(self.env, dataset, table)

    #     quantiles = table_class.getQuantiles()
    #     self.assertEqual(quantiles['FIPS State Code'][0] , 1)
    #     self.assertEqual(quantiles['FIPS State Code'][1] , 72.0)
    #     self.assertEqual(quantiles['FIPS State Code'][0.25] , 18.0)
        
    # def test_Table_Quantiles2(self):
    #     dataset = "Dataset 2"
    #     table = 'Table 2'
    #     columns = ['Note', 'CreatedOn']
    #     table_class = dpe.Table(self.env, dataset, table)
    #     self.assertEqual(table_class.getQuantiles(columns = columns),None)
        

    # def test_Table_DescriptiveStats(self):
    #     dataset = "Dataset 1"
    #     table = 'Table 1'
    #     table_class = dpe.Table(self.env, dataset, table)

    #     DS = table_class.getDescriptiveStats()
    #     # print(DS)
    #     self.assertEqual(DS['median']['FIPS County Code'],75.0)
    #     self.assertEqual(DS['mean']['CSA Code'],337.02)
    #     self.assertEqual(DS['std']['FIPS State Code'],16.94)
        # print(DS['skewness'])
        

    # Class Column #  
    def test_Column_ColumnMetadata(self):
        dataset = "Dataset 1"
        table = 'Table 2'
        column = 'rating'
        column_class = dpe.Column(self.env, dataset, table, column)
        metadata = column_class.getColumnMetadata()
        self.assertEqual(metadata['num_unique_values'], 2)
        self.assertEqual(metadata['data_type'], 'string')

    def test_Column_setFilters(self):
        dataset = "Dataset 1"
        table = 'Table 1'
        column = 'CBSA Title'
        column_class = dpe.Column(self.env, dataset, table, column)
        val = ["Abilene, TX","Auburn, NY"]
        column_class.setFilters(val)
        # Is there a way to check if the filter is applied
    
    def test_Column_ColumnDataType1(self):
        dataset = "Dataset 2"
        table = 'Table 2'
        column = 'Annotation Type'

        column_class = dpe.Column(self.env, dataset, table, column)
        self.assertEqual(column_class.getColumnDataType(),'string')


    def test_Column_ColumnDataType2(self):
        dataset = "Dataset 1"
        table = 'Table 1'
        column = 'CBSA Code'

        column_class = dpe.Column(self.env, dataset, table, column)
        self.assertEqual(column_class.getColumnDataType(),'integer')
  
    def test_Column_ValueCount(self):
        dataset = "Dataset 1"
        table = 'Table 2'
        column = 'rating'
        column_class = dpe.Column(self.env, dataset, table, column)
        self.assertEqual(column_class.getValueCount(),3)
    
    def test_Column_UniqueValueCount(self):
        dataset = "Dataset 1"
        table = 'Table 1'
        column = 'Metropolitan/Micropolitan Statistical Area'
        column_class = dpe.Column(self.env, dataset, table, column)
        self.assertEqual(column_class.getUniqueValueCount(),2)

    def test_Column_Visibility(self):
        dataset = "Dataset 2"
        table = 'Table 2'
        column = 'Annotation Type'
        column_class = dpe.Column(self.env, dataset, table, column)
        self.assertEqual(column_class.getVisibility(),"LIST.PUBLIC_DATA")
    
    def test_Column_UserAccessList(self):
        dataset = "Dataset 1"
        table = 'Table 1'
        column = 'CBSA Code'
        column_class = dpe.Column(self.env, dataset, table, column)
        self.assertIn('developer',column_class.getUserAccessList())
    
    def test_Column_listColumnCounts1(self):
        dataset = "Dataset 1"
        table = 'Table 2'
        column = 'rating'
        column_class = dpe.Column(self.env, dataset, table, column)

        self.assertEqual(len(column_class.listColumnCounts()),2)

    def test_Column_listColumnCounts2(self):
        dataset = "Dataset 1"
        table = 'Table 1'
        column = 'CBSA Code'
        column_class = dpe.Column(self.env, dataset, table, column)
        colcnt = column_class.listColumnCounts()
        self.assertEqual(len(colcnt),929)

    def test_Column_ColumnValuesInRange1(self):
        dataset = 'Dataset 2'
        table = 'Table 2'
        column = 'Dataset'
        column_class = dpe.Column(self.env, dataset, table, column)
        self.assertRaises(TypeError, column_class.getColumnValuesInRange(3,10))
        self.assertEqual([],column_class.getColumnValuesInRange(3,10))

    def test_Column_ColumnValuesInRange2(self):
        dataset = 'Dataset 1'
        table = 'Table 1'
        column = 'FIPS State Code'
        column_class = dpe.Column(self.env, dataset, table, column)
        cvals = column_class.getColumnValuesInRange(10,20)
        self.assertEqual(len(cvals), 9)

    # def test_Column_NAscount(self):
    #     dataset = "Dataset 2"
    #     table = 'Table 2'
    #     column = 'Column'

    #     column_class = dpe.Column(self.env, dataset, table, column)
    #     self.assertEqual(column_class.getNAscount(),3)

    # def test_Column_isfloat1(self):
    #     dataset = 'Dataset 1'
    #     table = 'Table 1'
    #     column = 'CSA Code'

    #     column_class = dpe.Column(self.env, dataset, table, column)
    #     self.assertEqual(column_class.isfloat(),True)

    # def test_Column_isfloat2(self):
    #     dataset = 'Dataset 1'
    #     table = 'Table 1'
    #     column = "Metropolitan Division Title"

    #     column_class = dpe.Column(self.env, dataset, table, column)
    #     self.assertEqual(column_class.isfloat(),False)

    # def test_Column_isint(self):
    #     dataset = 'Dataset 1'
    #     table = 'Table 1'
    #     column = 'CSA Code'

    #     column_class = dpe.Column(self.env, dataset, table, column)
    #     self.assertEqual(column_class.isint(),True)

    # def test_Column_isint(self):
    #     dataset = 'Dataset 1'
    #     table = 'Table 1'
    #     column = 'CSA Code'

    #     column_class = dpe.Column(self.env, dataset, table, column)
    #     print(column_class.getdatatype())

    # def test_Column_datatype(self):
    #     dataset = 'Dataset 1'
    #     table = 'Table 1'
    #     column = 'CSA Code'

    #     column_class = dpe.Column(self.env, dataset, table, column)
    #     print('test_Column_datatype---',column_class.getdatatype())
    #     print(dpe.Column(self.env, dataset, table, 'CBSA Title').getdatatype())


# Class Omni #  

    def test_Omni_AllMatches1(self):
        omni = dpe.Omnisearch(self.env, 'Central')
        allmatches = omni.getAllMatches()
        
        self.assertEqual(len(allmatches['datasets']) , 0)
        self.assertEqual(len(allmatches['tables']) , 0)
        self.assertEqual(len(allmatches['values']), 3)
        self.assertEqual(len(allmatches['columns']), 1)


    def test_Omni_AllMatches2(self):
        omni = dpe.Omnisearch(self.env, 'Created')
        allmatches = omni.getAllMatches()
        
        self.assertEqual(len(allmatches['datasets']) , 0)
        self.assertEqual(len(allmatches['tables']) , 0)
        self.assertEqual(len(allmatches['values']), 3)
        self.assertEqual(len(allmatches['columns']), 2)

    def test_Omni_AllMatches3(self):
        omni = dpe.Omnisearch(self.env, 'dataset')
        allmatches = omni.getAllMatches()
        self.assertEqual(len(allmatches['datasets']) , 2)
        self.assertEqual(len(allmatches['tables']) , 0)
        self.assertEqual(len(allmatches['values']), 1)
        self.assertEqual(len(allmatches['columns']), 1)

    def test_Omni_ValueMatches(self):
        search_strg = 'Lee'
        omni = dpe.Omnisearch(self.env, search_strg)
        search_all_vals = omni.getValueMatches()
        cnt = 0
        for x in search_all_vals:
            if search_strg.lower() in x['value'].lower():
                cnt += 1
        self.assertEqual(cnt,2)
        self.assertEqual(len(search_all_vals),2)  

    def test_Omni_DatasetMatches(self):
        search_strg = 'dataset'
        omni = dpe.Omnisearch(self.env, search_strg)
        dataset_match = omni.getDatasetMatches()
        cnt = 0
        for x in dataset_match:
            if search_strg.lower() in x['dataset'].lower():
                cnt += 1
        self.assertEqual(cnt,2)
        self.assertEqual(len(dataset_match),2)

    def test_Omni_ColumnMatches(self):
        search_strg = 'Metro'
        omni = dpe.Omnisearch(self.env, search_strg)
        column_match = omni.getColumnMatches()
        cnt = 0
        for x in column_match:
            if search_strg.lower() in x['column'].lower():
                cnt += 1
        self.assertEqual(cnt,3)
        self.assertEqual(len(column_match),3)

    def test_Omni_NumAllMatches(self):
        omni = dpe.Omnisearch(self.env,'san')
        self.assertEqual(omni.getNumAllMatches(), 43)

if __name__ == '__main__':
    wait_until_data_loaded()
    create_api_key()
    unittest.main()