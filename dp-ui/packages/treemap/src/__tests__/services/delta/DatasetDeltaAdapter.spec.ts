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
import DatasetDeltaAdapter from '../../../services/delta/DatasetDeltaAdapter'
import DatasetDeltaStub from '../../mocks/DatasetDelta.stub'

describe('given a dataset delta adapter', () => {
  let payload: string
  let adapter: DatasetDeltaAdapter

  beforeEach(() => {
    payload = DatasetDeltaStub.genSampleEndpointResponse()
    adapter = new DatasetDeltaAdapter()
  })

  test('then expect sane test data', () => {
    expect(payload).toBeTruthy()
  })

  test('then expect an adapter object', () => {
    expect(adapter).toBeTruthy()
  })

  test('then expect to convert a json payload to a CompelteDelta object', () => {
    const dataset = 'test_dataset'
    const completeDelta = adapter.convert(payload)
    expect(completeDelta).toBeTruthy()
    expect(completeDelta.updatedOnMillis).toBe(1621544557000)
    expect(completeDelta.lastKnownVersions).toBeTruthy()
    expect(completeDelta.lastKnownVersions.dataset).toBe(dataset)
    expect(completeDelta.deltas).toBeTruthy()
    expect(completeDelta.deltas.length).toBe(1)
    expect(completeDelta.deltas[0].dataset).toBe(dataset)
  })

  test('then expect to convert a json payload to a CompelteDelta object', () => {
    const completeDelta = adapter.convert('')
    expect(completeDelta).toBeTruthy()
    expect(completeDelta.updatedOnMillis).toBeUndefined()
    expect(completeDelta.lastKnownVersions).toBeUndefined()
    expect(completeDelta.deltas).toBeUndefined()
  })
})
