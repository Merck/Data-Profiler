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
import ColumnCounts from '../models/ColumnCounts'
import ColumnSample from '../models/ColumnSample'
import { Comment } from '../comments/models/Comment'

// https://redux.js.org/recipes/usage-with-typescript
// define action types
export const CLEAR_SAMPLES = 'listview/clear_samples'
export const SET_SAMPLES = 'listview/set_samples'
export const CLEAR_FETCHING_SAMPLES = 'listview/clear_fetching_samples'
export const IS_FETCHING_SAMPLES = 'listview/is_fetching_samples'
export const CLEAR_COLCOUNTS = 'listview/clear_colcounts'
export const SET_COLCOUNTS = 'listview/set_colcounts'
export const SET_COLCOUNTS_FETCH_SIZE = 'listview/set_colcounts_fetch_size'
export const CLEAR_COLCOUNTS_FETCH_SIZE = 'listview/clear_colcounts_fetch_size'
export const SET_IS_INITIAL_LOAD = 'listview/set_is_initial_load'
export const RESET_IS_INITIAL_LOAD = 'listview/reset_is_initial_load'
export const SET_TAB_INDEX = 'listview/set_tab_index'
export const CLEAR_TAB_INDEX = 'listview/clear_tab_index'
export const SET_FETCHING_PROPERTIES = 'listview/set_fetching_properties'
export const SET_QUALITY_TAB_COMMENTS_COUNT =
  'listview/set_quality_tab_comments_count'
export const SET_QUALITY_TAB_COMMENTS = 'listview/set_quality_tab_comments'
export const SET_QUALITY_TAB_COMMENTS_IN_FLIGHT =
  'listview/set_quality_tab_comments_in_flight'

interface Command {
  readonly type: string
  readonly payload?: any
}

interface ClearSamples extends Command {
  readonly type: typeof CLEAR_SAMPLES
}

interface SetSamples extends Command {
  readonly type: typeof SET_SAMPLES
  readonly payload: ColumnSample[]
}

interface ClearFetchingSamples extends Command {
  readonly type: typeof CLEAR_FETCHING_SAMPLES
}

interface IsFetchingSamples extends Command {
  readonly type: typeof IS_FETCHING_SAMPLES
}

interface ClearColCounts extends Command {
  readonly type: typeof CLEAR_COLCOUNTS
}

interface SetColCounts extends Command {
  readonly type: typeof SET_COLCOUNTS
  readonly payload: ColumnCounts
}

interface SetColCountsFetchSize extends Command {
  readonly type: typeof SET_COLCOUNTS_FETCH_SIZE
  readonly payload: number
}

interface ClearColCountsFetchSize extends Command {
  readonly type: typeof CLEAR_COLCOUNTS_FETCH_SIZE
}

interface SetIsInitialLoad extends Command {
  readonly type: typeof SET_IS_INITIAL_LOAD
  readonly payload: boolean
}

interface ResetIsInitialLoad extends Command {
  readonly type: typeof RESET_IS_INITIAL_LOAD
}

interface SetTabIndex extends Command {
  readonly type: typeof SET_TAB_INDEX
  readonly payload: string
}

interface ClearTabIndex extends Command {
  readonly type: typeof CLEAR_TAB_INDEX
}

interface SetFetchingProperties extends Command {
  readonly type: typeof SET_FETCHING_PROPERTIES
  readonly payload: string
}

interface SetQualityTabCommentsCount extends Command {
  readonly type: typeof SET_QUALITY_TAB_COMMENTS_COUNT
  readonly payload: number
}

interface SetQualityTabComments extends Command {
  readonly type: typeof SET_QUALITY_TAB_COMMENTS
  readonly payload: Array<Comment>
}

interface SetQualityTabCommentsInFlight extends Command {
  readonly type: typeof SET_QUALITY_TAB_COMMENTS_IN_FLIGHT
  readonly payload: boolean
}

export type ListViewActionTypes =
  | ClearSamples
  | SetSamples
  | ClearFetchingSamples
  | IsFetchingSamples
  | ClearColCounts
  | SetColCounts
  | SetColCountsFetchSize
  | ClearColCountsFetchSize
  | SetIsInitialLoad
  | ResetIsInitialLoad
  | SetTabIndex
  | ClearTabIndex
  | SetFetchingProperties
  | SetQualityTabCommentsCount
  | SetQualityTabComments
  | SetQualityTabCommentsInFlight

// define state reducer
export interface ListViewState {
  isInitialLoad: boolean
  isFetchingSamples: boolean
  samples: ColumnSample[]
  colCounts: ColumnCounts
  colCountsFetchSize: number
  tabIndex: string
  fetchingProperties: string
  qualityTabCommentsCount: number
  qualityTabComments: Comment[]
  qualityTabCommentsInFlight: boolean
}

export const DEFAULT_COL_COUNTS_FETCH_SIZE = 300
const emptyColCount = (): ColumnCounts => ({
  dataset: '',
  table: '',
  column: '',
  values: [],
})

const defaultTab = 'overview'

export const generateInitialState = (): ListViewState => {
  return {
    isInitialLoad: true,
    isFetchingSamples: false,
    samples: [],
    colCounts: emptyColCount(),
    colCountsFetchSize: DEFAULT_COL_COUNTS_FETCH_SIZE,
    tabIndex: defaultTab,
    fetchingProperties: '',
    qualityTabCommentsCount: null,
    qualityTabComments: [],
    qualityTabCommentsInFlight: false,
  }
}

const initialState = generateInitialState()

export default (
  state: ListViewState = initialState,
  action: ListViewActionTypes
): ListViewState => {
  switch (action.type) {
    case SET_SAMPLES:
      return {
        ...state,
        samples: [...action.payload],
        isFetchingSamples: false,
      }
    case CLEAR_SAMPLES:
      return {
        ...state,
        samples: [],
        isFetchingSamples: false,
      }
    case IS_FETCHING_SAMPLES:
      return {
        ...state,
        isFetchingSamples: true,
      }
    case CLEAR_FETCHING_SAMPLES:
      return {
        ...state,
        isFetchingSamples: false,
      }
    case SET_COLCOUNTS:
      return {
        ...state,
        colCounts: { ...action.payload },
      }
    case CLEAR_COLCOUNTS:
      return {
        ...state,
        colCounts: emptyColCount(),
      }
    case SET_COLCOUNTS_FETCH_SIZE:
      return {
        ...state,
        colCountsFetchSize: action.payload,
      }
    case CLEAR_COLCOUNTS_FETCH_SIZE:
      return {
        ...state,
        colCountsFetchSize: DEFAULT_COL_COUNTS_FETCH_SIZE,
      }
    case SET_IS_INITIAL_LOAD:
      return {
        ...state,
        isInitialLoad: action.payload,
      }
    case RESET_IS_INITIAL_LOAD:
      return {
        ...state,
        isInitialLoad: true,
      }
    case CLEAR_TAB_INDEX:
      return {
        ...state,
        tabIndex: defaultTab,
      }
    case SET_TAB_INDEX:
      return {
        ...state,
        tabIndex: action.payload || defaultTab,
      }
    case SET_FETCHING_PROPERTIES:
      return {
        ...state,
        fetchingProperties: action.payload,
      }
    case SET_QUALITY_TAB_COMMENTS_COUNT:
      return {
        ...state,
        qualityTabCommentsCount: action.payload,
      }
    case SET_QUALITY_TAB_COMMENTS:
      return {
        ...state,
        qualityTabComments: action.payload,
      }
    case SET_QUALITY_TAB_COMMENTS_IN_FLIGHT:
      return {
        ...state,
        qualityTabCommentsInFlight: action.payload,
      }
    default:
      return state
  }
}
