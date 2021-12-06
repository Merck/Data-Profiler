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
import RowResultsAdapter, {
  RowResultsEndpointShape,
} from '../../services/RowResultsAdapter'
import RowsStub from '../mocks/Rows.stub'

describe('given a sample adapter', () => {
  let endpointData: RowResultsEndpointShape
  let adapter: RowResultsAdapter

  beforeEach(() => {
    endpointData = RowsStub.rowsEndpointResponse()
    adapter = new RowResultsAdapter()
  })

  test('then expect sane test data', () => {
    expect(endpointData).toBeTruthy()
  })

  test('then expect an adapter object', () => {
    expect(adapter).toBeTruthy()
  })

  // test('then expect to do a simple transform of rows', () => {
  //   expect(adapter).toBeTruthy()
  //   const rowResults = adapter.endpointToRowResults(endpointData)
  //   expect(rowResults).toBeTruthy()
  //   expect(rowResults?.count).toBe(3)
  //   expect(rowResults?.rows).toEqual(expect.any(Array))
  //   expect(rowResults?.rows.length).toEqual(3)
  //   const sampleColumnName = 'qn'
  //   const sampleRow = rowResults?.rows[0].columns?.filter(
  //     (column) => column?.column === sampleColumnName
  //   )[0]
  //   expect(sampleRow).toBeTruthy()
  //   expect(sampleRow?.column).toEqual(sampleColumnName)
  //   expect(sampleRow?.value).toBe('200033264')
  // })
})
