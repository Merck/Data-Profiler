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
import ColumnCountsAdapter, {
  ColumnCountEndpointShape,
} from '../../services/ColumnCountsAdapter'
import ColumnCountStub from '../mocks/ColumnCount.stub'

describe('given a sample adapter', () => {
  let endpointData: Array<ColumnCountEndpointShape>
  let adapter: ColumnCountsAdapter

  beforeEach(() => {
    endpointData = ColumnCountStub.columnCountsEndpointResponse()
    adapter = new ColumnCountsAdapter()
  })

  test('then expect sane test data', () => {
    expect(endpointData).toBeTruthy()
  })

  test('then expect an adapter object', () => {
    expect(adapter).toBeTruthy()
  })

  test('then expect to transform a simple aggregate', () => {
    expect(adapter).toBeTruthy()
    const colCounts = adapter.endpointToColumnCounts(
      endpointData,
      'ds1',
      't3',
      'c10'
    )
    expect(colCounts).toBeTruthy()
    expect(colCounts.column).toBe('c10')
    expect(colCounts.dataset).toBe('ds1')
    expect(colCounts.table).toBe('t3')
    expect(colCounts.values).toEqual(expect.any(Array))
    expect(colCounts.values.length).toEqual(33)
    const sample1 = colCounts.values[0]
    expect(sample1.count).toBe(46276)
    expect(sample1.value).toBe('v1')
  })
})
