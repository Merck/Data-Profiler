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
import ColumnSample from '../../models/ColumnSample'
import ColumnSampleAdapter from '../../services/ColumnSampleAdapter'
import SamplesStub from '../mocks/Samples.stub'

describe('given a sample adapter', () => {
  let samples: Array<ColumnSample>
  let adapter: ColumnSampleAdapter

  beforeEach(() => {
    samples = SamplesStub.genTableSamples()
    adapter = new ColumnSampleAdapter()
  })

  test('then expect sane test data', () => {
    expect(samples).toBeTruthy()
  })

  test('then expect an adapter object', () => {
    expect(adapter).toBeTruthy()
  })

  test('then expect to transform a simple aggregate', () => {
    expect(adapter).toBeTruthy()
    const endpointData = SamplesStub.tableSamplesEndpointResponse()
    const tableSamples = adapter.endpointToColumnSamples(
      endpointData,
      'ds1',
      't3'
    )
    expect(tableSamples).toBeTruthy()
    expect(tableSamples).toEqual(expect.any(Array))
    expect(tableSamples.length).toEqual(16)
    const sample1 = tableSamples[0]
    expect(sample1.column).toBe('c1')
    expect(sample1.dataset).toBe('ds1')
    expect(sample1.table).toBe('t3')
    expect(sample1.count).toBe(26815)
    expect(sample1.value).toBe('v1')
  })
})
