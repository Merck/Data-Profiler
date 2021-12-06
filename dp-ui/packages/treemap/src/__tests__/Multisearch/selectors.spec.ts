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
import { getSelectedDrilldown } from '../../drilldown/selectors'
import { StoreState } from '../../index'
import {
  getDisplayedSearchResults,
  getSummedSearchResults,
  getDisplayedTableSuggestions,
} from '../../MultiSearch/selectors'
import StoreStateStub, { WITH_STATE } from '../mocks/StoreState.stub'

describe('given a omnisearch redux selector', () => {
  let store: StoreState

  beforeEach(() => {
    store = StoreStateStub.genStoreState(
      WITH_STATE.DATA,
      WITH_STATE.SUGGESTIONS
    )
  })

  it('returns the correct displayed search results state', () => {
    const displaySearchResults = getDisplayedSearchResults(store)
    expect(displaySearchResults).toBeTruthy()
    expect(displaySearchResults).toEqual(expect.any(Array))
    expect(displaySearchResults.length).toEqual(2)
    const el1 = displaySearchResults[0]
    expect(el1).toBeTruthy()
    expect(el1.value).toEqual('cat liver')
    expect(el1.count).toEqual(5858)
  })

  it('returns the correct summed display search results state', () => {
    const summedResults = getSummedSearchResults(store)
    expect(summedResults).toBeTruthy()
    expect(summedResults.elements).toEqual(expect.any(Array))
    expect(summedResults.elements.length).toEqual(89)
    const el1 = summedResults.elements[0]
    expect(el1).toBeTruthy()
    expect(el1.value).toEqual('v1')
    expect(el1.dataset).toEqual('d1')
    expect(el1.count).toEqual(5)
  })

  it('and no selected drilldown, returns the correct table metadata state', () => {
    const table = 'biz'
    const hits = getDisplayedTableSuggestions(store)
    expect(hits).toBeTruthy()
    expect(hits).toEqual(expect.any(Array))
    expect(hits.length).toEqual(25)
    const hit1 = hits[0]
    expect(hit1.count).toBe(2)
    expect(hit1.value).toBe(table)
    expect(hit1.elements).toEqual(expect.any(Array))
    expect(hit1.elements.length).toBe(2)
    const element1 = hit1.elements.find((el) => el.table === table)
    expect(element1).toBeTruthy()
    expect(element1).toEqual({
      dataset: "Third Test Dataset",
      value: 'biz',
      table: 'biz',
      tableId: 'biz',
      column: '',
      count: 1,
      isTitleResult: true,
      error: undefined,
    })
  })

  it('and a dataset drilldown, returns the correct table metadata state', () => {
    store = StoreStateStub.genStoreState(
      WITH_STATE.DATASET_DRILLDOWN,
      WITH_STATE.DATA,
      WITH_STATE.SUGGESTIONS
    )
    const hits = getDisplayedTableSuggestions(store)
    expect(hits).toBeTruthy()
    expect(hits).toEqual(expect.any(Array))
    expect(hits.length).toEqual(10)

    const el1 = hits[0]
    expect(el1).toBeTruthy()
    expect(el1.count).toBe(3)
    expect(el1.value).toBe('v1')
    expect(el1.elements).toEqual(expect.any(Array))
    expect(el1.elements.length).toEqual(3)
  })

  it('and a table drilldown, returns the correct table metadata state', () => {
    store = StoreStateStub.genStoreState(
      WITH_STATE.TABLE_DRILLDOWN,
      WITH_STATE.DATA,
      WITH_STATE.SUGGESTIONS
    )
    const hits = getDisplayedTableSuggestions(store)
    expect(hits).toBeTruthy()
    expect(hits).toEqual(expect.any(Array))
    expect(hits.length).toEqual(1)

    const el1 = hits[0]
    expect(el1).toBeTruthy()
    expect(el1.count).toBe(3)
    expect(el1.value).toBe('v1')
    expect(el1.elements).toEqual(expect.any(Array))
    expect(el1.elements.length).toEqual(3)
  })

  it('and a drilldown, returns the drilldown as an object not a interface', () => {
    store = StoreStateStub.genStoreState(
      WITH_STATE.DATASET_DRILLDOWN,
      WITH_STATE.DATA,
      WITH_STATE.SUGGESTIONS
    )
    const drilldown = getSelectedDrilldown(store)
    expect(drilldown).toBeTruthy()
    expect(drilldown.isDatasetView).toBeTruthy()
    expect(drilldown.isDatasetView()).toBeFalsy()
    expect(drilldown.isTableView).toBeTruthy()
    expect(drilldown.isTableView()).toBeTruthy()
    expect(drilldown.dataset).toEqual(store.drilldown['dataset'])
  })
})
