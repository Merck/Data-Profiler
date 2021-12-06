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
import SearchResultStub from '../../mocks/SearchResult.stub'
import MultisearchAdapter from '../../../MultiSearch/services/MultisearchAdapter'
import SearchResult from '../../../MultiSearch/models/SearchResult'

describe('given a multisearch adapter', () => {
  let results: Array<SearchResult>
  let multiValueResults: Array<SearchResult>
  let adapter: MultisearchAdapter

  beforeEach(() => {
    results = SearchResultStub.genSimpleResults()
    multiValueResults = SearchResultStub.genMultiValueResults()
    adapter = new MultisearchAdapter()
  })

  test('then expect sane test data', () => {
    expect(results).toBeTruthy()
  })

  test('then expect an adapter object', () => {
    expect(adapter).toBeTruthy()
  })

  test('then expect to transform a simple aggregate', () => {
    expect(results).toBeTruthy()
    expect(adapter).toBeTruthy()
    const aggregatedResults = adapter.endpointToAggegratedResults(results)
    expect(aggregatedResults).toBeTruthy()
    expect(aggregatedResults).toEqual(expect.any(Array))
    expect(aggregatedResults.length).toEqual(1)

    const aggregatedResult = aggregatedResults[0]
    expect(aggregatedResult.count).toBe(5858)
    expect(aggregatedResult.value).toEqual('v1')
    const elements = aggregatedResult.elements
    expect(elements).toEqual(expect.any(Array))
    expect(elements.length).toEqual(3)

    const el = elements[0]
    expect(el).toBeTruthy()
    expect(el.dataset).toBe('d1')
    expect(el.table).toBe('t1')
    expect(el.column).toBe('c1')
    expect(el.value).toBe('v1')
    expect(el.count).toBe(5)

    const el1 = elements[1]
    expect(el1).toBeTruthy()
    expect(el1.dataset).toBe('d1')
    expect(el1.table).toBe('t1')
    expect(el1.column).toBe('c1')
    expect(el1.value).toBe('v1')
    expect(el1.count).toBe(5822)
  })
})
