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
import { stringify } from 'query-string'
import CommonMetadata from '../../models/CommonMetadata'
import ReorderCommonMetadata from '../../models/ReorderCommonMetadata'
import AggregatedSearchResult from '../../MultiSearch/models/AggregatedSearchResult'
import MultisearchAdapter from '../../MultiSearch/services/MultisearchAdapter'
import MetadataAdapter from '../../services/MetadataAdapter'
import ReorderComparators from '../../services/ReorderMetadataComparators'
import ReorderMetadataService from '../../services/ReorderMetadataService'
import MetadataStub from '../mocks/Metadata.stub'
import SearchResultStub from '../mocks/SearchResult.stub'

describe('given a reorder metadata service', () => {
  let metadata: Readonly<CommonMetadata[]>
  let service: ReorderMetadataService
  let hits: AggregatedSearchResult

  beforeEach(() => {
    const endpointData = MetadataStub.genUnsortedTableMetadata()
    const metadataAdapter = new MetadataAdapter()
    metadata = metadataAdapter.datasetMetadataToCommonFormat(endpointData)
    service = new ReorderMetadataService()

    const results = SearchResultStub.genResultsForListViewTest()
    const adapter = new MultisearchAdapter()
    const aggregatedResults = adapter.endpointToAggegratedResults(results)
    hits = aggregatedResults[0]
  })

  test('then expect sane test data', () => {
    expect(metadata).toBeTruthy()
    expect(hits).toBeTruthy()
  })

  test('then expect an object', () => {
    expect(service).toBeTruthy()
  })

  test('then expect metadata service to sort an empty list', () => {
    expect(service).toBeTruthy()
    const sorted = service.reorderDatasets([], undefined)
    expect(sorted).toBeTruthy()
    expect(sorted.length).toEqual(0)
  })

  test('then expect metadata service to sort with metadata with no search hits', () => {
    expect(service).toBeTruthy()
    const sorted = service.reorderDatasets(metadata, undefined)
    expect(sorted).toBeTruthy()
    expect(sorted).toEqual(expect.any(Array))
    expect(sorted.length).toEqual(4)
    const names = ['123', 'AA', 'ZZZZ', 'aaaa']
    for (let i = 0; i < sorted.length; i++) {
      const dataset = sorted[i].commonMetadata.datasetName
      const expected = names.shift()
      expect(dataset).toEqual(expected)
    }
  })

  test('then expect metadata service to sort with metadata with search hits', () => {
    expect(service).toBeTruthy()
    const sorted = service.reorderDatasets(metadata, hits)
    expect(sorted).toBeTruthy()
    expect(sorted).toEqual(expect.any(Array))
    expect(sorted.length).toEqual(4)
    const names = ['aaaa', '123', 'AA', 'ZZZZ']
    for (let i = 0; i < sorted.length; i++) {
      const dataset = sorted[i].commonMetadata.datasetName
      const expected = names.shift()
      expect(dataset).toEqual(expected)
    }
  })

  test('then expect metadata service to sort with metadata title hits', () => {
    const compareByDatasetTitleMatch =
      ReorderComparators.compareByDatasetTitleMatch('csv', 'asc')
    expect(compareByDatasetTitleMatch).toBeTruthy()
    const names = [
      '',
    ]
    const sorted = genComparatorTestingData().sort(compareByDatasetTitleMatch)
    for (let i = 0; i < sorted.length; i++) {
      const dataset = sorted[i].commonMetadata.datasetName
      const expected = names.shift()
      expect(dataset).toEqual(expected)
    }
  })

  test('then expect metadata service to sort with metadata title hits', () => {
    const compareByDatasetTitleMatch =
      ReorderComparators.compareByDatasetTitleMatch('csv', 'desc')
    expect(compareByDatasetTitleMatch).toBeTruthy()
    const names = [
      '',
    ].reverse()
    const sorted = genComparatorTestingData().sort(compareByDatasetTitleMatch)
    for (let i = 0; i < sorted.length; i++) {
      const dataset = sorted[i].commonMetadata.datasetName
      const expected = names.shift()
      expect(dataset).toEqual(expected)
    }
  })

  test('then expect metadata service to sort with metadata value hits, asc', () => {
    const comparator = ReorderComparators.compareByHits('asc')
    expect(comparator).toBeTruthy()
    const names = [
      '',
    ]
    const sorted = genComparatorTestingData().sort(comparator)
    for (let i = 0; i < sorted.length; i++) {
      const dataset = sorted[i].commonMetadata.datasetName
      const expected = names.shift()
      expect(dataset).toEqual(expected)
    }
  })

  test('then expect metadata service to sort with metadata value hits, desc', () => {
    const comparator = ReorderComparators.compareByHits('desc')
    expect(comparator).toBeTruthy()
    const names = [
      '',
    ].reverse()
    const sorted = genComparatorTestingData().sort(comparator)
    for (let i = 0; i < sorted.length; i++) {
      const dataset = sorted[i].commonMetadata.datasetName
      const expected = names.shift()
      expect(dataset).toEqual(expected)
    }
  })

  test('then expect metadata service to sort with metadata loaded on dates, asc', () => {
    const comparator = ReorderComparators.compareByLoadedOn('asc')
    expect(comparator).toBeTruthy()
    const names = [
      '',
    ]
    const sorted = genComparatorTestingData().sort(comparator)
    for (let i = 0; i < sorted.length; i++) {
      const dataset = sorted[i].commonMetadata.datasetName
      const expected = names.shift()
      expect(dataset).toEqual(expected)
    }
  })

  test('then expect metadata service to sort with metadata loaded on dates, desc', () => {
    const comparator = ReorderComparators.compareByLoadedOn('desc')
    expect(comparator).toBeTruthy()
    const names = [
      '',
    ].reverse()
    const sorted = genComparatorTestingData().sort(comparator)
    for (let i = 0; i < sorted.length; i++) {
      const dataset = sorted[i].commonMetadata.datasetName
      const expected = names.shift()
      expect(dataset).toEqual(expected)
    }
  })

  const genComparatorTestingData = (): Partial<ReorderCommonMetadata>[] => {
    const baseDayMillis = 1615584224861
    const millisDay = 86400 * 1000
    return [
      genReorderCommonMetadata(1001, {
        datasetName: 'docsCSVs',
        datasetDisplayName: 'docsCSVs',
        tableName: 't1',
        columnName: 'c1',
        dataType: 'string',
        numTables: 3,
        numColumns: 3,
        numRows: 1002,
        numUniqueValues: 60,
        numValues: 99,
        loadedOn: baseDayMillis - millisDay * 1,
        updatedOn: baseDayMillis - millisDay * 1,
        visibility: '',
      }),
      genReorderCommonMetadata(202, {
        datasetName: 'testing_csv',
        datasetDisplayName: 'testing_csv',
        tableName: 't1',
        columnName: 'c1',
        dataType: 'string',
        numTables: 3,
        numColumns: 3,
        numRows: 1000,
        numUniqueValues: 60,
        numValues: 80,
        loadedOn: baseDayMillis - millisDay * 10,
        updatedOn: baseDayMillis - millisDay * 10,
        visibility: '',
      }),
      genReorderCommonMetadata(9999, {
        datasetName: 'testing_datasets_and_avro_and_csvs',
        datasetDisplayName: 'testing_datasets_and_avro_and_csvs',
        tableName: 't1',
        columnName: 'c1',
        dataType: 'string',
        numTables: 3,
        numColumns: 3,
        numRows: 2,
        numUniqueValues: 2,
        numValues: 430,
        loadedOn: baseDayMillis,
        updatedOn: baseDayMillis,
        visibility: '',
      }),
      genReorderCommonMetadata(16, {
        datasetName: 'csv_testing1',
        datasetDisplayName: 'csv_testing1',
        tableName: 't1',
        columnName: 'c1',
        dataType: 'string',
        numTables: 3,
        numColumns: 3,
        numRows: 2000,
        numUniqueValues: 600,
        numValues: 120,
        loadedOn: baseDayMillis - millisDay * 5,
        updatedOn: baseDayMillis - millisDay * 5,
        visibility: '',
      }),
    ]
  }

  const genReorderCommonMetadata = (
    numHits: number,
    obj: CommonMetadata
  ): Partial<ReorderCommonMetadata> => {
    return {
      numHits,
      commonMetadata: {
        ...obj,
      },
    }
  }
})
