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
import ColumnCounts from '../../models/ColumnCounts'
import ColumnCountsAdapter from '../../services/ColumnCountsAdapter'
import { ColumnCountsService } from '../../services/ColumnCountsService'
import ColumnCountStub from '../mocks/ColumnCount.stub'

describe('given a column counts service', () => {
  let service: ColumnCountsService
  let data: ColumnCounts

  beforeEach(() => {
    service = new ColumnCountsService()
    const endpointData = ColumnCountStub.columnCountsEndpointResponse()
    const adapter = new ColumnCountsAdapter()
    data = adapter.endpointToColumnCounts(endpointData)
  })

  test('then expect sane test data', () => {
    expect(data).toBeTruthy()
  })

  test('then expect a service', () => {
    expect(service).toBeTruthy()
  })

  test('then expect to rerank column count - global filter, matching and nonmatching', () => {
    const filter = 'california'
    const partition = service.rerank(data, [], [filter])
    expect(partition).toBeTruthy()
    const matched = partition['HAS_GLOBAL_MATCH_ONLY']
    expect(matched).toBeTruthy()
    expect(matched.values.length).toBe(2)
    const nonmatched = partition['NO_MATCH']
    expect(nonmatched).toBeTruthy()
    expect(nonmatched.values.length).toBe(data.values.length - 2)
  })

  test('then expect to rerank column count - global filter, nonmatching only', () => {
    const partition = service.rerank(data, [], [''])
    expect(partition).toBeTruthy()
    const matched = partition['HAS_GLOBAL_MATCH_ONLY']
    expect(matched).toBeTruthy()
    expect(matched.values.length).toBe(0)
    const nonmatched = partition['NO_MATCH']
    expect(nonmatched).toBeTruthy()
    expect(nonmatched.values.length).toBe(data.values.length)
  })

  test('then expect to rerank column count - local and global filters, matching and nonmatching', () => {
    const globalFilter = 'california'
    const localFilter = 'sur'
    const partition = service.rerank(data, [localFilter], [globalFilter])
    expect(partition).toBeTruthy()
    const localAndGlobalMatched = partition['HAS_LOCAL_AND_GLOBAL_MATCH']
    expect(localAndGlobalMatched).toBeTruthy()
    expect(localAndGlobalMatched.values.length).toBe(1)
    const localMatched = partition['HAS_LOCAL_MATCH_ONLY']
    expect(localMatched).toBeTruthy()
    expect(localMatched.values.length).toBe(0)
    const globalMatched = partition['HAS_GLOBAL_MATCH_ONLY']
    expect(globalMatched).toBeTruthy()
    expect(globalMatched.values.length).toBe(1)
    const nonmatched = partition['NO_MATCH']
    expect(nonmatched).toBeTruthy()
    expect(nonmatched.values.length).toBe(data.values.length - 1 - 1)
  })

  test('then expect to rerank column count - local and global filters, nonmatches only 1', () => {
    const partition = service.rerank(data, [''], [''])
    expect(partition).toBeTruthy()
    const localAndGlobalMatched = partition['HAS_LOCAL_AND_GLOBAL_MATCH']
    expect(localAndGlobalMatched).toBeTruthy()
    expect(localAndGlobalMatched.values.length).toBe(0)
    const localMatched = partition['HAS_LOCAL_MATCH_ONLY']
    expect(localMatched).toBeTruthy()
    expect(localMatched.values.length).toBe(0)
    const globalMatched = partition['HAS_GLOBAL_MATCH_ONLY']
    expect(globalMatched).toBeTruthy()
    expect(globalMatched.values.length).toBe(0)
    const nonmatched = partition['NO_MATCH']
    expect(nonmatched).toBeTruthy()
    expect(nonmatched.values.length).toBe(data.values.length)
  })

  test('then expect to rerank column count - local and global filters, nonmatches only 2', () => {
    const partition = service.rerank(data, [], [])
    expect(partition).toBeTruthy()
    const localAndGlobalMatched = partition['HAS_LOCAL_AND_GLOBAL_MATCH']
    expect(localAndGlobalMatched).toBeTruthy()
    expect(localAndGlobalMatched.values.length).toBe(0)
    const localMatched = partition['HAS_LOCAL_MATCH_ONLY']
    expect(localMatched).toBeTruthy()
    expect(localMatched.values.length).toBe(0)
    const globalMatched = partition['HAS_GLOBAL_MATCH_ONLY']
    expect(globalMatched).toBeTruthy()
    expect(globalMatched.values.length).toBe(0)
    const nonmatched = partition['NO_MATCH']
    expect(nonmatched).toBeTruthy()
    expect(nonmatched.values.length).toBe(data.values.length)
  })
})
