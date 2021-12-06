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
import SelectedDrilldown from './models/SelectedDrilldown'

// https://redux.js.org/recipes/usage-with-typescript
// define action types
export const CLEAR_DRILLDOWN = 'treemap_and_listview/clear_drilldown'
export const SET_DRILLDOWN = 'treemap_and_listview/set_drilldown'

interface Command {
  readonly type: string
  readonly payload?: any
}

interface ClearDrilldown extends Command {
  readonly type: typeof CLEAR_DRILLDOWN
}

interface SetDrilldown extends Command {
  readonly type: typeof SET_DRILLDOWN
  readonly payload: Record<string, any>
}

export type DrilldownActionTypes = ClearDrilldown | SetDrilldown

export interface SelectedDrilldownState extends Record<string, any> {}

const emptyDrilldown = (): SelectedDrilldown =>
  SelectedDrilldown.of({
    dataset: '',
    table: '',
    column: '',
  })

export const generateInitialState = (): SelectedDrilldownState => {
  return emptyDrilldown().serialize()
}

const initialState = generateInitialState()

export default (
  state = initialState,
  action: DrilldownActionTypes
): SelectedDrilldownState => {
  switch (action.type) {
    case SET_DRILLDOWN:
      const payload = action.payload || emptyDrilldown().serialize()
      return {
        ...payload,
      }
    case CLEAR_DRILLDOWN:
      return {
        ...emptyDrilldown().serialize(),
      }
    default:
      return {
        ...state,
      }
  }
}
