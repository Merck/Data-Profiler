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
import NameSearchAdapter from '../../../MultiSearch/services/NameSearchAdapter'
import MetadataResultStub from '../../mocks/MetadataResult.stub'
import MetadataResult from '../../../MultiSearch/models/MetadataResult'
import { hasUncaughtExceptionCaptureCallback } from 'process'

describe('given a namesearch adapter', () => {
  let datasetResults: MetadataResult[]
  let tableResults: MetadataResult[]
  let columnResults: MetadataResult[]
  let adapter: NameSearchAdapter

  beforeEach(() => {
    datasetResults = MetadataResultStub.genDatasetResults()
    tableResults = MetadataResultStub.genTableResults()
    columnResults = MetadataResultStub.genColumnResults()
    adapter = new NameSearchAdapter()
  })

  test('then expect sane test data', () => {
    expect(datasetResults).toBeTruthy()
    expect(tableResults).toBeTruthy()
    expect(columnResults).toBeTruthy()
  })

  test('then expect an adapter object', () => {
    expect(adapter).toBeTruthy()
  })

  test('then expect to transform datset, table or column results', () => {
    const datasetHits = adapter.convertAll(datasetResults, 'dataset')
    expect(datasetHits).toBeTruthy()
    expect(datasetHits?.count).toBe(3)
    expect(datasetHits?.elements.length).toBe(3)
    const name = 'd1:t1'
    expect(datasetHits?.value).toBe(name)
    const element = datasetHits.elements[0]
    expect(element).toBeTruthy()
    expect(element?.count).toBe(1)
    expect(element?.dataset).toBe(name)
    expect(element?.table).toBe('')
    expect(element?.column).toBe('')
    expect(element?.isTitleResult).toBeTruthy()
  })

  test('then expect to transform table results', () => {
    const tableHits = adapter.convertAll(tableResults, 'table')
    expect(tableHits).toBeTruthy()
    expect(tableHits?.count).toBe(32)
    expect(tableHits?.elements.length).toBe(32)
    const name = 'biz'
    expect(tableHits?.value).toBe(name)
    const element = tableHits.elements[0]
    expect(element).toBeTruthy()
    expect(element?.count).toBe(1)
    expect(element?.dataset).toBe("Third Test Dataset")
    expect(element?.table).toBe(name)
    expect(element?.column).toBe('')
    expect(element?.isTitleResult).toBeTruthy()
  })

  test('then expect to transform column results', () => {
    const columnHits = adapter.convertAll(columnResults, 'column')
    expect(columnHits).toBeTruthy()
    expect(columnHits?.count).toBe(48)
    expect(columnHits?.elements.length).toBe(48)
    const name = 'id'
    expect(columnHits?.value).toBe(name)
    const element = columnHits.elements[0]
    expect(element?.dataset).toBe("Third Test Dataset")
    expect(element?.table).toBe('biz')
    expect(element?.column).toBe(name)
    expect(element?.isTitleResult).toBe(true)
  })

  test('then expect to group and transform column results', () => {
    const columnHits = adapter.groupAndConvertAll(columnResults, 'column')
    expect(columnHits).toBeTruthy()
    expect(columnHits.length).toBe(9)
    const key = 'id'
    const result = columnHits.find((el) => el.value === key)
    expect(result).toBeTruthy()
    const elements = result.elements
    expect(elements).toBeTruthy()
    expect(elements.length).toBe(30)
  })
})
