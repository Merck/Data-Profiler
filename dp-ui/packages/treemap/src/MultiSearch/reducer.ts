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
// https://redux.js.org/recipes/usage-with-typescript

import { isEqual, uniqWith } from 'lodash'
import AggregatedSearchResult from './models/AggregatedSearchResult'
import { ChipPhrase } from './models/ChipPhrase'
import SearchResult from './models/SearchResult'

// define action types
export const UPDATE_SEARCH = 'omnisearch/update_search'
export const ADD_CHIP_PHRASE = 'omnisearch/add_chip_phrase'
export const REMOVE_CHIP_PHRASE = 'omnisearch/remove_chip_phrase'
export const SET_SEARCH_SUGGESTIONS = 'omnisearch/set_search_suggestions'
export const SET_DATASET_SUGGESTIONS = 'omnisearch/set_dataset_suggestions'
export const SET_TABLE_SUGGESTIONS = 'omnisearch/set_table_suggestions'
export const SET_COLUMN_SUGGESTIONS = 'omnisearch/set_column_suggestions'
export const CLEAR_SEARCH_SUGGESTIONS = 'omnisearch/clear_search_suggestions'
export const SET_SEARCH_IN_FLIGHT = 'omnisearch/set_search_in_flight'

interface Command {
  readonly type: string
  readonly payload?: any
}

interface UpdateSearch extends Command {
  readonly type: typeof UPDATE_SEARCH
  readonly payload: string
}

interface AddChipPhrase extends Command {
  readonly type: typeof ADD_CHIP_PHRASE
  readonly payload: ChipPhrase
}

interface RemoveChipPhrase extends Command {
  readonly type: typeof REMOVE_CHIP_PHRASE
  readonly payload: ChipPhrase
}

interface SetSearchSuggestions extends Command {
  readonly type: typeof SET_SEARCH_SUGGESTIONS
  readonly payload: Array<SearchResult>
}

interface SetDatasetSuggestions extends Command {
  readonly type: typeof SET_DATASET_SUGGESTIONS
  readonly payload: Array<AggregatedSearchResult>
}

interface SetTableSuggestions extends Command {
  readonly type: typeof SET_TABLE_SUGGESTIONS
  readonly payload: Array<AggregatedSearchResult>
}

interface SetColumnSuggestions extends Command {
  readonly type: typeof SET_COLUMN_SUGGESTIONS
  readonly payload: Array<AggregatedSearchResult>
}

interface ClearSearchSuggestions extends Command {
  readonly type: typeof CLEAR_SEARCH_SUGGESTIONS
}

interface SetSearchInFlight extends Command {
  readonly type: typeof SET_SEARCH_IN_FLIGHT
}

export type SearchActionTypes =
  | UpdateSearch
  | AddChipPhrase
  | RemoveChipPhrase
  | SetSearchSuggestions
  | SetDatasetSuggestions
  | SetTableSuggestions
  | SetColumnSuggestions
  | ClearSearchSuggestions
  | SetSearchInFlight

// define state reducer
export interface SearchState {
  chipPhrases: ChipPhrase[]
  search: string
  searchSuggestions: Array<SearchResult>
  datasetSuggestions: Array<AggregatedSearchResult>
  tableSuggestions: Array<AggregatedSearchResult>
  columnSuggestions: Array<AggregatedSearchResult>
  isSearchInFlight: boolean
}

export const generateInitialState = (): SearchState => {
  return {
    chipPhrases: [],
    search: undefined,
    searchSuggestions: [],
    datasetSuggestions: [],
    tableSuggestions: [],
    columnSuggestions: [],
    isSearchInFlight: false,
  }
}

const initialState = generateInitialState()

export default (
  state: SearchState = initialState,
  action: SearchActionTypes
): SearchState => {
  switch (action.type) {
    case UPDATE_SEARCH:
      return {
        ...state,
        search: action.payload,
      }
    case ADD_CHIP_PHRASE:
      return {
        ...state,
        // TODO: could support a single chip
        //  For now, we delete all chips
        //  need to figure out how to handle deleting
        //  arbitrary chips and displaying the correctly
        // chipPhrases: uniq([action.payload]),
        // _.uniqWith(objects, _.isEqual);
        // chipPhrases: uniq([...state.chipPhrases, action.payload]),
        chipPhrases: uniqWith(
          [...state.chipPhrases, { ...action.payload }],
          isEqual
        ),
      }
    case REMOVE_CHIP_PHRASE:
      return {
        ...state,
        chipPhrases: [],
        // TODO: can only support a delete of all chips at the moment
        //  need to figure out how to handle deleting
        //  arbitrary chips and displaying the correctly
        // chipPhrases: [...state.chipPhrases].filter(
        //   (phrase) => phrase != action.payload
        // ),
      }
    case SET_SEARCH_SUGGESTIONS:
      return {
        ...state,
        searchSuggestions: [...action.payload],
      }
    case SET_DATASET_SUGGESTIONS:
      return {
        ...state,
        datasetSuggestions: [...action.payload],
      }
    case SET_TABLE_SUGGESTIONS:
      return {
        ...state,
        tableSuggestions: [...action.payload],
      }
    case SET_COLUMN_SUGGESTIONS:
      return {
        ...state,
        columnSuggestions: [...action.payload],
      }
    case CLEAR_SEARCH_SUGGESTIONS:
      return {
        ...state,
        datasetSuggestions: [],
        tableSuggestions: [],
        columnSuggestions: [],
        searchSuggestions: [],
      }
    case SET_SEARCH_IN_FLIGHT:
      return {
        ...state,
        isSearchInFlight: action.payload,
      }
    default:
      return state
  }
}
