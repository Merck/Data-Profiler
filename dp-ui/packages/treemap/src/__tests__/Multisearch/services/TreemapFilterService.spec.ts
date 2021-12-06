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
import TreemapFilterService from '../../../services/TreemapFilterService'
import TreemapObject from '../../../models/TreemapObject'
import TreemapObjectStub from '../../mocks/TreemapObject.stub'
import SelectedDrilldown from '../../../drilldown/models/SelectedDrilldown'

describe('given a treemap filter service', () => {
  let data: TreemapObject
  let service: TreemapFilterService

  beforeEach(() => {
    data = TreemapObjectStub.genDataStub()
    service = new TreemapFilterService()
  })

  test('then expect sane test data', () => {
    expect(data).toBeTruthy()
    expect(data.children).toEqual(expect.any(Array))
    expect(data.children.length).toEqual(28)
  })

  test('then expect a service object', () => {
    expect(service).toBeTruthy()
  })

  test('then expect to filter against a 0 filter', () => {
    expect(data).toBeTruthy()
    expect(service).toBeTruthy()
    const selectedDrilldown = new SelectedDrilldown()
    const filtered = service.generateContextSensitiveFilteredTreemapData(
      data,
      undefined,
      selectedDrilldown
    )
    expect(filtered).toBeTruthy()
    expect(filtered.children).toEqual(expect.any(Array))
    expect(filtered.children.length).toEqual(28)
  })

  test('then expect to filter against a drilldown filter only', () => {
    expect(data).toBeTruthy()
    expect(service).toBeTruthy()
    const selectedDrilldown = new SelectedDrilldown('d1')
    const filtered = service.generateContextSensitiveFilteredTreemapData(
      data,
      undefined,
      selectedDrilldown
    )
    expect(filtered).toBeTruthy()
    expect(filtered.children).toEqual(expect.any(Array))
    expect(filtered.children.length).toEqual(1)
    const table1 = filtered.children[0]
    expect(table1).toBeTruthy()
    expect(table1).toEqual({
      name: 't1',
      size: 110745,
      updated: 'year',
      children: [],
      scaleColor: undefined,
    })
  })
})
