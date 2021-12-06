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
import CommonMetadata from '../../models/CommonMetadata'
import MetadataAdapter from '../../services/MetadataAdapter'
import MetadataService from '../../services/MetadataService'
import MetadataStub from '../mocks/Metadata.stub'

describe('given a metadata service', () => {
  let metadata: Readonly<CommonMetadata[]>
  let metadataService: MetadataService
  let dataset: string

  beforeEach(() => {
    const endpointData = MetadataStub.genTableMetadata()
    const metadataAdapter = new MetadataAdapter()
    metadata = metadataAdapter.datasetMetadataToCommonFormat(endpointData)
    metadataService = new MetadataService()
    dataset = ''
  })

  test('then expect sane test data', () => {
    expect(metadata).toBeTruthy()
  })

  test('then expect an object', () => {
    expect(metadataService).toBeTruthy()
  })

  test('then expect metadata service to calculate a visibility', () => {
    expect(metadataService).toBeTruthy()
    const viz = metadataService.buildViz(metadata)
    expect(viz).toBeTruthy()
    expect(viz).toEqual('LIST.')
  })

  test('then expect metadata service to find unique tables', () => {
    expect(metadataService).toBeTruthy()
    const uniqTables = metadataService.uniqTables(metadata, dataset)
    expect(uniqTables).toBeTruthy()
    expect(uniqTables).toEqual(expect.any(Array))
    expect(uniqTables.length).toEqual(30)
    expect(uniqTables[0]).toEqual('')
    expect(uniqTables.includes(''))
    expect(uniqTables.includes(''))
    expect(uniqTables[29]).toEqual('')
  })

  test('then expect metadata service to find unique tables case insensitive', () => {
    const uniqTables = metadataService.uniqTables(
      metadata,
      dataset.toLowerCase()
    )
    expect(uniqTables).toBeTruthy()
    expect(uniqTables).toEqual(expect.any(Array))
    expect(uniqTables.length).toEqual(30)
  })

  test('then expect metadata service to find - number of unique tables', () => {
    const numTables = metadataService.sumUniqTables(metadata, dataset)
    expect(numTables).toBeTruthy()
    expect(numTables).toEqual(30)
  })

  test('then expect metadata service to find - number of rows', () => {
    const total = metadataService.sumRows(metadata, dataset)
    expect(total).toBeTruthy()
    expect(total).toEqual(10557512)
  })

  test('then expect metadata service to find - number of columns', () => {
    const total = metadataService.sumColumns(metadata, dataset)
    expect(total).toBeTruthy()
    expect(total).toEqual(369)
  })

  test('then expect metadata service to find - number of values', () => {
    const total = metadataService.sumValues(metadata, dataset)
    expect(total).toBeTruthy()
    expect(total).toEqual(145595635)
  })

  test('then expect metadata service to find - number of unique values', () => {
    const total = metadataService.sumUniqValues(metadata, dataset)
    expect(total).toBeTruthy()
    expect(total).toEqual(111)
  })
})
