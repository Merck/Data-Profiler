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
import dp_external_client as dpec
from termcolor import colored
import requests
import json

def testEnvironmentFuncs(env):
    print (env.getDatasetList())
    print (env.getDatasetCount())
    print (env.getEnvironmentInfo())

def testDatasetFuncs(dataset):
    # print (dataset.getTableCount())
    # print (dataset.getTableList())
    # print (dataset.getUploadDate())
    # print (dataset.getUpdateDate())
    # print (dataset.metadata)
    # print (dataset.getVisibility())
    # print (dataset.getTableCount())
    # print (dataset.getColumnCount())
    #print (dataset.getValueCount())
    dict_dfs = dataset.importDataset()
    print (len(dict_dfs))

def testTableFuncs(table):
    # print (table.getColumnCount())
    # print (table.getColumnList())
    # print (table.getUpdateDate())
    # print (table.metadata)
    # print (table.getVisibility())
    # print (table.getTableCount())
    # print (table.getValueCount())
    #table.setFilters({"height":["81", "82", "83"]})
    # df = table.loadRows()
    # print (df.shape)
    # filtered_df = df.loc[df["weight"] == "220"]
    # print (filtered_df.shape)
    df = table.loadRowsWithSubstringMatches({"weight": ["20"]})
    print (df.shape)
    print (df["weight"].unique())
    # matches = table.loadRowsInRange(200, 210, "weight")
    # print (matches.shape)
    # print (matches["weight"].unique())

def testColumnFuncs(col):
    # print (col.getColumnDataType())
    # print (col.getValueCount())
    # print (col.getUniqueValueCount())
    # print (col.getVisibility())
    col.setFilters({"height":["81"]})
    print (col.listColumnCounts())

def testOmniFuncs(omni):
    # print ("All:", omni.searchAllColumns())
    # print ("Values:", omni.searchAllValues())
    # print ("Tables:", omni.searchAllTables())
    # print ("Columns:", omni.searchAllColumns())

    # print ("Num col:", omni.getNumColumnMatches())
    # print ("Num table:", omni.getNumTableMatches())
    # print ("Num ds:", omni.getNumDatasetMatches())
    # print ("Num value:", omni.getNumValueMatches())
    # print ("Num all matches:", omni.getNumAllMatches())
    matches = omni.getSubstringValueMatches("ebron jame")
    #print (len(matches))
    #print (matches)

def main():
    url ="https://preview-api.dataprofiler.com"
    key = ''
    if key == '':
        print (colored("MAKE SURE TO INSERT YOUR API KEY", "red"))
        return
    preview_env = dpec.Environment(api_key=key, url=url)
    #testEnvironmentFuncs(preview_env)

    dataset = dpec.Dataset(environment=preview_env, dataset_name="Athlete Attributes")
    #testDatasetFuncs(dataset)

    table = dpec.Table(environment=preview_env, dataset_name="Athlete Attributes", table_name='athlete_attributes')
    testTableFuncs(table)

    column = dpec.Column(environment=preview_env, dataset_name="Athlete Attributes", table_name='athlete_attributes', column_name='height')
    #testColumnFuncs(column)

    omni_search = dpec.Omnisearch(environment=preview_env, search_str="multiplier")
    #testOmniFuncs(omni_search)


if __name__ == "__main__":
    main()