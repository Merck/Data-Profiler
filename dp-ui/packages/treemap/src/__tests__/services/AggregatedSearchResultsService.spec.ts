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
import AggregatedSearchResult from '../../MultiSearch/models/AggregatedSearchResult'
import AggregratedSearchResultService from '../../services/AggregatedSearchResultService'
import AggregratedSearchResultStub from '../mocks/AggregratedSearchResult.stub'

describe('given a search results service', () => {
  let results: AggregatedSearchResult
  let service: AggregratedSearchResultService
  let dataset: string
  let table: string
  let titleHitDataset: string
  let titleHitTable: string

  beforeEach(() => {
    dataset = 'd1'
    table = 't1'
    titleHitDataset = 'maryland'
    titleHitTable = 'baltimore'
    results = AggregratedSearchResultStub.genSample()
    service = new AggregratedSearchResultService()
  })

  test('then expect sane test data', () => {
    expect(results).toBeTruthy()
    expect(dataset).toBeTruthy()
    expect(results.elements).toBeTruthy()
    expect(results.elements.length).toBeGreaterThan(1)
  })

  test('then expect an service object', () => {
    expect(service).toBeTruthy()
  })

  test('then expect to calculate number of uniq datasets', () => {
    const numDatasets = service.calcNumDatasets(results)
    expect(numDatasets).toEqual(1)
  })

  test('then expect to calculate number of matched tables, also properly count title hits', () => {
    const results = AggregratedSearchResultStub.genSampleWithMultipleTitleHit()
    const hasTitleHits = service.hasTitleHit(results)
    expect(hasTitleHits).toBeTruthy()
    const numDatasets = service.calcNumDatasets(results)
    expect(numDatasets).toEqual(3)
  })

  test('then expect to calculate number of matched tables', () => {
    const numTables = service.calcNumTables(results, dataset)
    expect(numTables).toEqual(1)
  })

  test('then expect to calculate number of matched tables', () => {
    const results = AggregratedSearchResultStub.genSampleWithMultipleTitleHit()
    const hasTitleHits = service.hasTitleHit(results)
    expect(hasTitleHits).toBeTruthy()
    const numTables = service.calcNumTables(results, 'senate dataset1')
    expect(numTables).toEqual(2)
  })

  test('then expect to calculate number of matched columns', () => {
    const numColumns = service.calcNumColumns(results, dataset, table)
    expect(numColumns).toEqual(1)
  })

  test('then expect to calculate number of matched columns for a dataset. also properly count title hits', () => {
    const results = AggregratedSearchResultStub.genSampleWithMultipleTitleHit()
    const hasTitleHits = service.hasTitleHit(results)
    expect(hasTitleHits).toBeTruthy()
    const numColumns = service.calcNumColumns(results, 'senate dataset1')
    expect(numColumns).toEqual(2)
  })

  test('then expect to calculate number of matched columns for a dataset and table. also properly count title hits', () => {
    const results = AggregratedSearchResultStub.genSampleWithMultipleTitleHit()
    const hasTitleHits = service.hasTitleHit(results)
    expect(hasTitleHits).toBeTruthy()
    const numColumns = service.calcNumColumns(
      results,
      'senate dataset1',
      'senate table1'
    )
    expect(numColumns).toEqual(1)
  })

  test('then expect to sum number of hit value counts', () => {
    const hitsValueCount = service.calcValueHitCount(results, dataset, table)
    expect(hitsValueCount).toEqual(536)
  })

  test('then expect to sum number of hit value counts and not include title hits', () => {
    const results = AggregratedSearchResultStub.genSampleWithTitleHit()
    const hasTitleHits = service.hasTitleHit(results)
    expect(hasTitleHits).toBeTruthy()
    const hitsValueCount = service.calcValueHitCount(
      results,
      titleHitDataset,
      titleHitTable
    )
    expect(hitsValueCount).toEqual(32)
  })

  test('then expect to detect title hits', () => {
    const results = AggregratedSearchResultStub.genSampleWithTitleHit()
    const hasTitleHits = service.hasTitleHit(results)
    expect(hasTitleHits).toBeTruthy()
  })

  test('then expect to filter title hits', () => {
    const results = AggregratedSearchResultStub.genSampleWithTitleHit()
    const aggregatedResults = service.filterForTitleHits(results)
    expect(aggregatedResults).toBeTruthy()
    expect(aggregatedResults?.value).toBe('bill')
    expect(aggregatedResults?.elements?.length).toBe(1)
    const titleHit = aggregatedResults?.elements[0]
    expect(titleHit.count).toBe(1)
    expect(titleHit.column).toBe('')
    expect(aggregatedResults?.count).toBe(1)
  })

  test('then expect to filter nontitle hits', () => {
    const results = AggregratedSearchResultStub.genSampleWithTitleHit()
    const aggregatedResults = service.filterNoneTitleHits(results)
    expect(aggregatedResults).toBeTruthy()
    expect(aggregatedResults?.value).toBe('bill')
    expect(aggregatedResults?.elements?.length).toBe(1)
    const titleHit = aggregatedResults?.elements[0]
    expect(titleHit.count).toBe(32)
    expect(titleHit.column).toBe('activity')
    expect(aggregatedResults?.count).toBe(32)
  })

  test('then expect to not fail when no title hits found', () => {
    const results = AggregratedSearchResultStub.genSample()
    // search for title hits when none exist
    const aggregatedResults = service.filterForTitleHits(results)
    expect(aggregatedResults).toBeTruthy()
    expect(aggregatedResults?.value).toBe('maryland')
    expect(aggregatedResults?.count).toBe(0)
    expect(aggregatedResults?.elements?.length).toBe(0)
  })
})
