/*
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
*/
import { uniq } from 'lodash'
import MetadataAdapter from '../../services/MetadataAdapter'
import MetadataStub from '../mocks/Metadata.stub'

describe('given a metadata adapter', () => {
  let datasetMetadata
  let tableMetadata
  let columnMetadata
  let metadataAdapter: MetadataAdapter

  beforeEach(() => {
    datasetMetadata = MetadataStub.genDatasetMetadata()
    tableMetadata = MetadataStub.genTableMetadata()
    columnMetadata = MetadataStub.genColumnMetadata()
    metadataAdapter = new MetadataAdapter()
  })

  test('then expect sane test data', () => {
    expect(datasetMetadata).toBeTruthy()
    expect(tableMetadata).toBeTruthy()
    expect(columnMetadata).toBeTruthy()
  })

  test('then expect an object', () => {
    expect(metadataAdapter).toBeTruthy()
  })

  test('then expect concept column metadata to a common metadata format', () => {
    expect(metadataAdapter).toBeTruthy()
    expect(columnMetadata).toBeTruthy()
    const commonMetadata =
      metadataAdapter.columnMetadataToCommonFormat(columnMetadata)
    expect(commonMetadata).toBeTruthy()
    expect(commonMetadata).toEqual(expect.any(Array))
    expect(commonMetadata.length).toEqual(9)
    const datasetNames = uniq(commonMetadata.map((el) => el.datasetName))
    expect(datasetNames.length).toBe(1)
    expect(datasetNames).toEqual(expect.arrayContaining(['d1']))
    const tableNames = uniq(commonMetadata.map((el) => el.tableName))
    expect(tableNames.length).toBe(1)
    expect(tableNames).toEqual(expect.arrayContaining(['t1']))
    const colNames = uniq(commonMetadata.map((el) => el.columnName))
    expect(colNames.length).toBe(9)
    expect(colNames).toEqual(
      expect.arrayContaining(['year'])
    )
    const asssertTruthyAndValid = (el: any) => {
      expect(el).toBeTruthy()
      expect(el).toBeGreaterThan(0)
    }
    commonMetadata.forEach((metadata) => {
      asssertTruthyAndValid(metadata.numTables)
      asssertTruthyAndValid(metadata.numColumns)
      asssertTruthyAndValid(metadata.numRows)
      asssertTruthyAndValid(metadata.numValues)
      asssertTruthyAndValid(metadata.numUniqueValues)
      asssertTruthyAndValid(metadata.loadedOn)
      expect(metadata.dataType).toBeTruthy()
      expect(metadata.dataType).toEqual('string')
    })
  })

  test('then expect concept table metadata to a common metadata format', () => {
    expect(metadataAdapter).toBeTruthy()
    expect(tableMetadata).toBeTruthy()
    const commonMetadata =
      metadataAdapter.tableMetadataToCommonFormat(tableMetadata)
    expect(commonMetadata).toBeTruthy()
    expect(commonMetadata).toEqual(expect.any(Array))
    expect(commonMetadata.length).toEqual(30)
    const datasetNames = uniq(commonMetadata.map((el) => el.datasetName))
    expect(datasetNames.length).toBe(1)
    expect(datasetNames).toEqual(
      expect.arrayContaining([''])
    )
    const tableNames = uniq(commonMetadata.map((el) => el.tableName))
    expect(tableNames.length).toBe(30)
    expect(tableNames).toEqual(
      expect.arrayContaining([
        '',
      ])
    )
    const asssertTruthyAndValid = (el: any) => {
      expect(el).toBeDefined()
      expect(el).toBeTruthy()
      expect(el).toBeGreaterThan(0)
    }
    commonMetadata.forEach((metadata) => {
      asssertTruthyAndValid(metadata.numTables)
      asssertTruthyAndValid(metadata.numColumns)
      asssertTruthyAndValid(metadata.numRows)
      asssertTruthyAndValid(metadata.numValues)
      asssertTruthyAndValid(metadata.loadedOn)
      asssertTruthyAndValid(metadata.updatedOn)
    })
  })

  test('then expect concept dataset metadata to a common metadata format', () => {
    expect(metadataAdapter).toBeTruthy()
    expect(datasetMetadata).toBeTruthy()
    const commonMetadata =
      metadataAdapter.datasetMetadataToCommonFormat(datasetMetadata)
    expect(commonMetadata).toBeTruthy()
    expect(commonMetadata).toEqual(expect.any(Array))
    expect(commonMetadata.length).toEqual(6)
    const datasetNames = uniq(commonMetadata.map((el) => el.datasetName))
    expect(datasetNames.length).toBe(6)
    const expectedDatasets = [
      '',
    ]
    expect(datasetNames).toEqual(expect.arrayContaining(expectedDatasets))
    const tableNames = uniq(commonMetadata.map((el) => el.tableName))
    expect(tableNames.length).toBe(1)
    // there are no table names, just dataset names
    expect(tableNames).toEqual(expect.arrayContaining(['']))
    const asssertTruthyAndValid = (el) => {
      expect(el).toBeTruthy()
      expect(el).toBeGreaterThan(0)
    }
    commonMetadata.forEach((metadata) => {
      asssertTruthyAndValid(metadata.numTables)
      asssertTruthyAndValid(metadata.numColumns)
      asssertTruthyAndValid(metadata.numRows)
      asssertTruthyAndValid(metadata.numValues)
      asssertTruthyAndValid(metadata.loadedOn)
      asssertTruthyAndValid(metadata.updatedOn)
    })
  })
})
