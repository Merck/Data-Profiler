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
import { Dispatch } from '@reduxjs/toolkit'
import { isEmpty } from 'lodash'
import { store } from '../index'
import SelectedDrilldown from '../drilldown/models/SelectedDrilldown'
import AggregatedSearchResult from './models/AggregatedSearchResult'
import { ChipPhrase, ChipPhraseScopeEnum } from './models/ChipPhrase'
import MetadataResult from './models/MetadataResult'
import SearchResult from './models/SearchResult'
import {
  multiSearch,
  searchColumnNames,
  searchDatasetNames,
  searchTableNames,
} from './provider'
import * as ACTIONS from './reducer'
import NameSearchAdapter from './services/NameSearchAdapter'

/**
 * entry point to updating the search box
 *
 * @param search
 * @param selectedDrilldown
 */
export const updateSearch =
  (search: string, selectedDrilldown?: SelectedDrilldown) =>
  (dispatch): void => {
    // clear old search suggestions then fetch new suggestions
    clearSearchSuggestions()(dispatch)
    clearDatasetSuggestions()(dispatch)
    clearTableSuggestions()(dispatch)
    clearColumnSuggestions()(dispatch)
    dispatch({
      type: ACTIONS.UPDATE_SEARCH,
      payload: search,
    })

    if (!search || search.trim().length === 0) {
      return
    }
    searchInFlight(true)(dispatch)
    Promise.all([
      fetchUpdatedSearchSuggestions(search, selectedDrilldown)(dispatch),
      fetchDatasetSuggestions(search, selectedDrilldown)(dispatch),
      fetchTableSuggestions(search, selectedDrilldown)(dispatch),
      fetchColumnSuggestions(search, selectedDrilldown)(dispatch),
    ]).finally(() => {
      searchInFlight(false)(dispatch)
    })
  }

export const searchInFlight =
  (isInFlight = true) =>
  (dispatch): void => {
    dispatch({
      type: ACTIONS.SET_SEARCH_IN_FLIGHT,
      payload: isInFlight,
    })
  }

export const addChipPhrase =
  (phrase: ChipPhrase) =>
  (dispatch): void => {
    if (isEmpty(phrase)) {
      return
    }
    dispatch({
      type: ACTIONS.ADD_CHIP_PHRASE,
      payload: phrase,
    })
  }

export const removeChipPhrase =
  (phrase: ChipPhrase) =>
  (dispatch): void => {
    if (isEmpty(phrase)) {
      return
    }
    dispatch({
      type: ACTIONS.REMOVE_CHIP_PHRASE,
      payload: phrase,
    })
  }

/**
 * updates the search term, search suggestion and take into account
 * the currently selected chip context
 *
 * @param input
 * @param selectedDrilldown
 */
export const fetchUpdatedSearchSuggestions =
  (input: string, selectedDrilldown?: SelectedDrilldown) =>
  (dispatch: Dispatch): Promise<Array<SearchResult>> => {
    const search = (input || '').trim()
    dispatch({
      type: ACTIONS.UPDATE_SEARCH,
      payload: '',
    })
    if (isEmpty(search)) {
      clearSearchSuggestions()(dispatch)
      return Promise.resolve([])
    }

    dispatch({
      type: ACTIONS.UPDATE_SEARCH,
      payload: search,
    })
    const chipPhrases = store.getState().multisearch.chipPhrases
    const phrases = normalizePhrases(search, chipPhrases)
    return fetchSearchSuggestions(phrases, selectedDrilldown)(dispatch)
  }

/**
 * match user search phrases against given tokens used to search backend
 * @param tokens
 * @return
 *  true if the given tokens match all the current user search phrases
 */
const haveCurrentSearchTokens = (tokens: Array<string>): boolean => {
  const currentInputSearchToken = store
    .getState()
    .multisearch.search?.toLowerCase()
  const chipPhrases = store
    .getState()
    .multisearch.chipPhrases?.filter((chip) => chip && chip.phrase)
    ?.map((chip) => chip.phrase?.trim()?.toLowerCase())
  // build user search phrases
  const phrases = new Set(Array.of(...chipPhrases, currentInputSearchToken))
  if (phrases?.size === 0 || tokens?.length === 0) {
    return false
  }

  // match tokens against user search phrases
  return tokens
    ?.filter((token) => token !== undefined)
    ?.map((token) => token.toLowerCase())
    ?.every((token) => phrases.has(token))
}

const normalizePhrases = (input: string, chips: ChipPhrase[]): string[] => {
  const search = (input?.toLowerCase() || '').trim()
  const chipPhrases = chips
    ?.filter((chip) => chip && chip.phrase)
    ?.map((chip) => chip.phrase?.trim()?.toLowerCase())
  return Array.from(new Set(Array.of(...chipPhrases, search)))
}

/**
 * Careful to call this action, you may need to manage the
 * call to update the search term yourself,
 * as this action only manages the search suggestions
 *
 * @code
 * <pre><code>
 * dispatch({
 *   type: ACTIONS.UPDATE_SEARCH,
 *   payload: search,
 * })
 * </pre></code>
 *
 *
 * See updateSearch or fetchUpdatedSearchSuggestions instead
 *  which takes into account the chips, search terms, in flight boolean
 *  and results
 *
 * @see fetchUpdatedSearchSuggestions
 * @see updateSearch
 * @param tokens
 * @param selectedDrilldown
 */
export const fetchSearchSuggestions =
  (tokens: Array<string>, selectedDrilldown?: SelectedDrilldown) =>
  (dispatch: Dispatch): Promise<Array<SearchResult>> => {
    if (isEmpty(tokens) || !selectedDrilldown) {
      clearSearchSuggestions()(dispatch)
      return Promise.resolve([])
    }

    return multiSearch(tokens, selectedDrilldown)
      .then((data) => {
        const current = haveCurrentSearchTokens(tokens)
        if (!current) {
          // console.log(
          //   'not current search tokens! discarding search results for ',
          //   tokens
          // )
          return []
        }
        dispatch({
          type: ACTIONS.SET_SEARCH_SUGGESTIONS,
          payload: data
            ? data
            : [
                {
                  error: 'Failed to fetch values',
                },
              ],
        })
        return data
      })
      .catch((err: Response) => {
        console.warn(err)
        clearSearchSuggestions()(dispatch)
        return []
      })
  }

export const fetchDatasetSuggestions =
  (input?: string, selectedDrilldown?: SelectedDrilldown) =>
  (dispatch: Dispatch): Promise<AggregatedSearchResult[]> => {
    const search = (input || '').trim()
    const chipPhrases = store
      .getState()
      .multisearch.chipPhrases?.filter(
        (chip) =>
          chip?.scope === ChipPhraseScopeEnum.ALL ||
          chip?.scope === ChipPhraseScopeEnum.DATASET_TITLE
      )
    const phrases = normalizePhrases(search, chipPhrases)
    return searchDatasetNames(phrases, selectedDrilldown)
      .then((arr: Array<MetadataResult>) => {
        const current = haveCurrentSearchTokens(phrases)
        if (!current) {
          // console.log(
          //   'not current search tokens! discarding dataset name results for ',
          //   phrases
          // )
          return []
        }

        const namesearchAdapter = new NameSearchAdapter()
        const hits = namesearchAdapter.groupAndConvertAll(arr, 'dataset')
        dispatch({
          type: ACTIONS.SET_DATASET_SUGGESTIONS,
          payload: hits,
        })

        return hits
      })
      .catch((err: Response) => {
        console.warn(err)
        clearDatasetSuggestions()(dispatch)
        return []
      })
  }

export const fetchTableSuggestions =
  (input?: string, selectedDrilldown?: SelectedDrilldown) =>
  (dispatch: Dispatch): Promise<AggregatedSearchResult[]> => {
    const search = (input || '').trim()
    const chipPhrases = store
      .getState()
      .multisearch.chipPhrases?.filter(
        (chip) =>
          chip?.scope === ChipPhraseScopeEnum.ALL ||
          chip?.scope === ChipPhraseScopeEnum.TABLE_TITLE
      )
    const phrases = normalizePhrases(search, chipPhrases)
    return searchTableNames(phrases, selectedDrilldown)
      .then((arr: Array<MetadataResult>) => {
        const current = haveCurrentSearchTokens(phrases)
        if (!current) {
          // console.log(
          //   'not current search tokens! discarding table name results for ',
          //   phrases
          // )
          return []
        }

        const namesearchAdapter = new NameSearchAdapter()
        const hits = namesearchAdapter.groupAndConvertAll(arr, 'table')
        dispatch({
          type: ACTIONS.SET_TABLE_SUGGESTIONS,
          payload: hits,
        })
        return hits
      })
      .catch((err: Response) => {
        console.warn(err)
        clearTableSuggestions()(dispatch)
        return []
      })
  }

export const fetchColumnSuggestions =
  (input?: string, selectedDrilldown?: SelectedDrilldown) =>
  (dispatch: Dispatch): Promise<AggregatedSearchResult[]> => {
    const search = (input || '').trim()
    const chipPhrases = store
      .getState()
      .multisearch.chipPhrases?.filter(
        (chip) =>
          chip?.scope === ChipPhraseScopeEnum.ALL ||
          chip?.scope === ChipPhraseScopeEnum.COLUMN_TITLE
      )
    const phrases = normalizePhrases(search, chipPhrases)
    return searchColumnNames(phrases, selectedDrilldown)
      .then((arr: Array<MetadataResult>) => {
        const current = haveCurrentSearchTokens(phrases)
        if (!current) {
          // console.log(
          //   'not current search tokens! discarding column name results for ',
          //   phrases
          // )
          return []
        }

        const namesearchAdapter = new NameSearchAdapter()
        const hits = namesearchAdapter.groupAndConvertAll(arr, 'column')
        dispatch({
          type: ACTIONS.SET_COLUMN_SUGGESTIONS,
          payload: hits,
        })
        return hits
      })
      .catch((err: Response) => {
        console.warn(err)
        clearColumnSuggestions()(dispatch)
        return []
      })
  }

export const clearDatasetSuggestions =
  () =>
  (dispatch: Dispatch): void => {
    dispatch({
      type: ACTIONS.SET_DATASET_SUGGESTIONS,
      payload: [],
    })
  }

export const clearTableSuggestions =
  () =>
  (dispatch: Dispatch): void => {
    dispatch({
      type: ACTIONS.SET_TABLE_SUGGESTIONS,
      payload: [],
    })
  }

export const clearColumnSuggestions =
  () =>
  (dispatch: Dispatch): void => {
    dispatch({
      type: ACTIONS.SET_COLUMN_SUGGESTIONS,
      payload: [],
    })
  }

export const clearSearchSuggestions =
  () =>
  (dispatch: Dispatch): void => {
    dispatch({
      type: ACTIONS.CLEAR_SEARCH_SUGGESTIONS,
    })
  }
