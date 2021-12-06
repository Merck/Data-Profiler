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
import { DEFAULT_ORDER_BY, OrderBy } from './models/OrderBy'

// define action types
export const SET_ORDER_BY = 'treemap_and_listview/set_order_by'
export const CLEAR_ORDER_BY = 'treemap_and_listview/clear_order_by'

interface Command {
  readonly type: string
  readonly payload?: any
}

interface SetOrderBy extends Command {
  readonly type: typeof SET_ORDER_BY
  readonly payload: OrderBy
}

interface ClearOrderBy extends Command {
  readonly type: typeof CLEAR_ORDER_BY
}

export type OrderByActionTypes = SetOrderBy | ClearOrderBy

export interface OrderByState {
  selectedOrderBy: OrderBy
}

export const generateInitialState = (): OrderByState => {
  return {
    selectedOrderBy: { orderBy: DEFAULT_ORDER_BY },
  }
}

const initialState = generateInitialState()

export default (
  state = initialState,
  action: OrderByActionTypes
): OrderByState => {
  switch (action.type) {
    case SET_ORDER_BY:
      return {
        ...state,
        selectedOrderBy: { ...action.payload },
      }
    case CLEAR_ORDER_BY:
      return {
        ...state,
        selectedOrderBy: { orderBy: DEFAULT_ORDER_BY },
      }
    default:
      return state
  }
}
