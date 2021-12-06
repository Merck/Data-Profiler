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
import { StoreState } from '../../index'
import {
  getPreviewDrawerDrilldown,
  getCommentLevel,
} from '../../Drawers/selectors'
import StoreStateStub, { WITH_STATE } from '../mocks/StoreState.stub'

describe('given a preview drawer redux selector', () => {
  let store: StoreState

  beforeEach(() => {
    store = StoreStateStub.genStoreState(
      WITH_STATE.DATA,
      WITH_STATE.DATASET_DRILLDOWN,
      WITH_STATE.SUGGESTIONS,
      WITH_STATE.PREVIEW_DRAWER,
      WITH_STATE.COMMENTS
    )
  })

  it('expect a selected drilldown', () => {
    const drawer = getPreviewDrawerDrilldown(store)
    expect(drawer).toBeTruthy()
    const { dataset, table, column } = drawer
    expect(dataset).toBe('d1')
    expect(table).toBe('t1')
    expect(column).toBe('c1')
  })

  it('expect a comment selected drilldown', () => {
    const drawer = getCommentLevel(store)
    expect(drawer).toBeTruthy()
    const { dataset, table, column } = drawer
    // comment level can now be independent of the drawer
    // since comments are across the app
    expect(dataset).toBe('d1')
    expect(table).toBe(undefined)
    expect(column).toBe(undefined)
  })
})
