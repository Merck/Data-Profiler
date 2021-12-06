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
import { isBoolean, isEmpty } from 'lodash'
import { createSelector } from 'reselect'
import { StoreState } from '..'
import RowViewerLaunch from '../models/RowViewerLaunch'
import SelectedView from '../models/SelectedView'
import MultisearchAdapter from './services/MultisearchAdapter'
import MultisearchService from './services/MultisearchService'
import { getSelectedDrilldown } from '../drilldown/selectors'

export const getMultiSearch = (state: StoreState) => state.multisearch
export const getSearchSuggestions = (state: StoreState) =>
  state.multisearch.searchSuggestions
export const getLastSearchToken = (state: StoreState) =>
  state.multisearch.search
export const getPhrases = (state: StoreState) => state.multisearch.chipPhrases
export const getHoveredValue = (state: StoreState) => state.treemap.hoveredValue
export const getDatasetSuggestions = (state: StoreState) =>
  state.multisearch.datasetSuggestions
export const getTableSuggestions = (state: StoreState) =>
  state.multisearch.tableSuggestions
export const getColumnSuggestions = (state: StoreState) =>
  state.multisearch.columnSuggestions
// note selected view has to be serialized and unserialized to store in redux store
export const getSelectedView = (state: StoreState): SelectedView =>
  SelectedView.of(state.treemap.selectedView)
export const isSearchInFlight = (state: StoreState) =>
  state.multisearch.isSearchInFlight
export const getSelectedRowView = (state: StoreState): RowViewerLaunch =>
  state.treemap.rowViewer

export const getDisplayedSearchResults = createSelector(
  [getSearchSuggestions],
  (searchSuggestions) => {
    const adapter = new MultisearchAdapter()
    const displayResults =
      adapter.endpointToAggegratedResults(searchSuggestions)
    const service = new MultisearchService()
    const topByValueCounts =
      service.topDisplaySearchResultsByValueCounts(displayResults)
    return topByValueCounts
  }
)

export const getSummedSearchResults = createSelector(
  [
    getLastSearchToken,
    isSearchInFlight,
    getDisplayedSearchResults,
    getDatasetSuggestions,
    getTableSuggestions,
    getColumnSuggestions,
  ],
  (
    search,
    isSearchInFlight,
    searchValueHits,
    datasetHits,
    tableHits,
    columnHits
  ) => {
    const service = new MultisearchService()
    if (isBoolean(isSearchInFlight) && isSearchInFlight) {
      // console.log("search is in flight, nothing to do...")
      return service.emptyResult()
    }
    // console.log("getSummedSearchResults")
    // console.log(displayedResults)
    // console.log(search)
    const allHits = [
      ...(searchValueHits || []),
      ...(datasetHits || []),
      ...(tableHits || []),
      ...(columnHits || []),
    ]
    return service.sumAllResults(allHits, search)
  }
)

export const getDisplayedTableSuggestions = createSelector(
  [getTableSuggestions],
  (tableSuggestions) => {
    return tableSuggestions
  }
)

export const getDisplayedColumnSuggestions = createSelector(
  [getColumnSuggestions],
  (columnSuggestions) => {
    return columnSuggestions
  }
)

export const getSearchTerms = createSelector([getPhrases], (phrases) => {
  if (isEmpty(phrases)) return []
  return phrases.map((p) => p.phrase)
})

export const getSelectedDrilldownForTreeMap = createSelector(
  [getSelectedDrilldown],
  (drilldown) => {
    // clear column so that preview drawer loads correctly
    drilldown.column = undefined
    return drilldown
  }
)
