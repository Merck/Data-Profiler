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
import MultiSearchService from '../../../MultiSearch/services/MultisearchService'
import AggregatedSearchResult from '../../../MultiSearch/models/AggregatedSearchResult'

describe('given a multisearch service', () => {
  let results: SearchResult[]
  let multiValueResults: SearchResult[]
  let aggregatedResults: AggregatedSearchResult[]
  let service: MultiSearchService

  beforeEach(() => {
    results = SearchResultStub.genSimpleResults()
    multiValueResults = SearchResultStub.genMultiValueResults()
    const adapter = new MultisearchAdapter()
    aggregatedResults = adapter.endpointToAggegratedResults(results)
    service = new MultiSearchService()
  })

  test('then expect sane test data', () => {
    expect(results).toBeTruthy()
    expect(multiValueResults).toBeTruthy()
    expect(aggregatedResults).toBeTruthy()
  })

  test('then expect a valid class under test', () => {
    expect(service).toBeTruthy()
  })

  test('then expect to calculate top 2 aggregate elements', () => {
    const topElements = service.topSearchElements(aggregatedResults, 2)
    expect(topElements).toBeTruthy()
    expect(topElements).toEqual(expect.any(Array))
    expect(topElements.length).toEqual(2)

    const el = topElements[0]
    expect(el).toBeTruthy()
    expect(el.dataset).toBe('d1')
    expect(el.table).toBe('t1')
    expect(el.column).toBe('c1')
    expect(el.value).toBe('v1')
    expect(el.count).toBe(5822)
  })

  test('then expect to calculate top 2 aggregate elements', () => {
    const topElements = service.topSearchElements(aggregatedResults, 2)
    expect(topElements).toBeTruthy()
    expect(topElements).toEqual(expect.any(Array))
    expect(topElements.length).toEqual(2)

    const el = topElements[0]
    expect(el).toBeTruthy()
    expect(el.dataset).toBe('d1')
    expect(el.table).toBe('t1')
    expect(el.column).toBe('c1')
    expect(el.value).toBe('v1')
    expect(el.count).toBe(5822)

  })

  test('then expect sum all results into a single result value', () => {
    const adapter = new MultisearchAdapter()
    const aggregatedResults =
      adapter.endpointToAggegratedResults(multiValueResults)
    const singleResultValue = service.sumAllResults(
      aggregatedResults,
      'last search term'
    )

    expect(singleResultValue).toBeTruthy()
    expect(singleResultValue.value).toBe('last search term')
    expect(singleResultValue.count).toBe(10868)
    const elements = singleResultValue.elements
    expect(elements).toEqual(expect.any(Array))
    expect(elements.length).toEqual(6)

    const el = elements[0]
    expect(el).toBeTruthy()
    expect(el.count).toBe(31)


  })
})
