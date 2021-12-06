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
import {
  clearColumnData,
  clearTableMetadata,
  fetchAndSetColumnMetadata,
  fetchAndSetTableMetadata,
  setFilterValue,
  setSelectedDrilldown,
  setSelectedView,
  searchInFlight,
} from './actions'
import {
  clearColumnCounts,
  closePreviewDrawer,
  fetchAndSetDatasetsMetadata,
  fetchColumnCounts,
  openPreviewDrawer,
  updatePreviewDrawerDrilldown,
} from './Drawers/actions'
import { clearCommentCounts, fetchCommentCounts } from './comments/actions'
import { store } from './index'
import SelectedDrilldown from './drilldown/models/SelectedDrilldown'
import {
  clearColumnSuggestions,
  clearDatasetSuggestions,
  clearSearchSuggestions,
  clearTableSuggestions,
  fetchColumnSuggestions,
  fetchDatasetSuggestions,
  fetchTableSuggestions,
} from './MultiSearch/actions'
import { ChipPhraseScopeEnum } from './MultiSearch/models/ChipPhrase'
import { multiSearch } from './MultiSearch/provider'
import MultisearchAdapter from './MultiSearch/services/MultisearchAdapter'
import SelectedView, { SELECTED_VIEW_ENUM } from './models/SelectedView'
import MultisearchService from './MultiSearch/services/MultisearchService'

/**
 * used during treemap and list view switch over
 * @param selectedDrilldown
 */
export const updateSelectedViewWithRefresh =
  (selectedView: SelectedView) =>
  (dispatch: Dispatch): void => {
    const datasetMeta = store.getState().previewdrawer.datasetsMeta
    if (isEmpty(datasetMeta)) {
      fetchAndSetDatasetsMetadata()(dispatch)
    }

    // requery the search terms since the view changed
    if (selectedView?.currentView() === SELECTED_VIEW_ENUM.LIST) {
      // list view, query with not drilldown
      // no drilldown is used since list view shows cards for all datasets
      ensureRefreshFilter()(dispatch)
    } else {
      // treemap view, query with selected drilldown context
      const curDrilldown = SelectedDrilldown.of(store.getState().drilldown)
      ensureRefreshFilter(curDrilldown)(dispatch)
    }
    // set selected view
    setSelectedView(selectedView)(dispatch)
  }

/**
 * Used mostly for the treemap and search state
 * all actions needed to handle a drill in and out of datasets,tables, and columns
 * used for breadcrumb click out or clicking into a cell
 */
export const updateSelectedDrilldownWithRefresh =
  (selectedDrilldown?: Readonly<SelectedDrilldown>) =>
  (dispatch: Dispatch): void => {
    if (!selectedDrilldown) return

    // clear data to reduce flash of old content
    cleanTreemapState(selectedDrilldown)(dispatch)

    const curDrilldown = SelectedDrilldown.of(store.getState().drilldown)
    if (!selectedDrilldown.equals(curDrilldown)) {
      // requery the search terms since the view changed
      // determine how and when best to requery
      //    if chip phrases are selected
      //    make sure backing out of a breadcrumbs refreshs
      //    search results filter
      ensureRefreshFilter(selectedDrilldown)(dispatch)
    }

    const previewDrawSelection = SelectedDrilldown.from(selectedDrilldown)
    updatePreviewDrawerDrilldown(previewDrawSelection)(dispatch)

    if (selectedDrilldown.isTableView()) {
      fetchAndSetTableMetadata(selectedDrilldown.dataset)(dispatch)
      fetchCommentCounts(selectedDrilldown)(dispatch)
    } else if (selectedDrilldown.isColumnView()) {
      fetchAndSetColumnMetadata(
        selectedDrilldown.dataset,
        selectedDrilldown.table
      )(dispatch)
      // TODO: fetch comment counts on table select
      fetchCommentCounts(selectedDrilldown)(dispatch)
    } else if (selectedDrilldown.isSpecificColumnView()) {
      fetchColumnCounts(previewDrawSelection)(dispatch)
      fetchCommentCounts(previewDrawSelection)(dispatch)
      openPreviewDrawer()(dispatch)
    }

    const treemapDrilldown = SelectedDrilldown.from(selectedDrilldown)
    // NOTE: this logic was moved to MultiSearch/selectors.ts
    // // the treemap does not need the selected column
    // //  only the preview drawer needs the column
    // treemapDrilldown.column = undefined

    setSelectedDrilldown(treemapDrilldown)(dispatch)
  }

/**
 * if the selected drill down changed, make sure to requery the search phrases against the correct view
 * @param selectedDrilldown
 */
export const ensureRefreshFilter =
  (selectedDrilldown = new SelectedDrilldown()) =>
  (dispatch: Dispatch): void => {
    // requery the search terms since the view changed
    const chipPhrases = store.getState().multisearch.chipPhrases
    const phrases = Array.of(...chipPhrases?.map((chip) => chip?.phrase))
    if (!phrases || isEmpty(phrases)) {
      return
    }

    searchInFlight(true)(dispatch)

    const displayPhrase = [...phrases].join(' AND ')
    const empty = {
      value: '',
      count: 0,
      elements: [],
    }
    setFilterValue(empty)(dispatch)
    const adapter = new MultisearchAdapter()
    const service = new MultisearchService()
    chipPhrases?.map((chip) => {
      const phrase = chip?.phrase || ''
      const normalizedPhrase = phrase.toLowerCase()
      const caseInsensitiveValue = (hit) =>
        hit.value.toLowerCase().includes(normalizedPhrase)
      switch (chip?.scope) {
        case ChipPhraseScopeEnum.VALUES:
          multiSearch(phrases, selectedDrilldown)
            .then((data) => {
              const results = adapter.endpointToAggegratedResults(data)
              const filter = {
                ...service.sumAllResults(results, displayPhrase),
                value: phrase,
              }
              setFilterValue(filter)(dispatch)
            })
            .finally(() => {
              searchInFlight(false)(dispatch)
            })
          break
        case ChipPhraseScopeEnum.ALL:
          Promise.all([
            multiSearch([phrase], selectedDrilldown),
            fetchDatasetSuggestions(phrase, selectedDrilldown)(dispatch),
            fetchTableSuggestions(phrase, selectedDrilldown)(dispatch),
            fetchColumnSuggestions(phrase, selectedDrilldown)(dispatch),
          ])
            .then(([data, datasetHits, tableHits, columnHits]) => {
              let results = adapter.endpointToAggegratedResults(data)
              if (datasetHits && !isEmpty(datasetHits)) {
                const hits = datasetHits.filter(caseInsensitiveValue)
                results = [...results, ...(hits || [])]
              }

              if (tableHits && !isEmpty(tableHits)) {
                const hits = tableHits.filter(caseInsensitiveValue)
                results = [...results, ...(hits || [])]
              }

              if (columnHits && !isEmpty(columnHits)) {
                const hits = columnHits.filter(caseInsensitiveValue)
                results = [...results, ...(hits || [])]
              }

              const filter = service.sumAllResults(results, displayPhrase)
              const allHits = {
                ...filter,
                value: phrase,
                elements: [...filter.elements],
              }
              setFilterValue(allHits)(dispatch)
              clearDatasetSuggestions()(dispatch)
              clearTableSuggestions()(dispatch)
              clearColumnSuggestions()(dispatch)
            })
            .finally(() => {
              searchInFlight(false)(dispatch)
            })
          break
        case ChipPhraseScopeEnum.DATASET_TITLE:
          fetchDatasetSuggestions(
            phrase,
            selectedDrilldown
          )(dispatch)
            .then((hits) => {
              const hit = hits.find(caseInsensitiveValue)
              setFilterValue(hit)(dispatch)
              clearDatasetSuggestions()(dispatch)
            })
            .finally(() => {
              searchInFlight(false)(dispatch)
            })
          break
        case ChipPhraseScopeEnum.TABLE_TITLE:
          fetchTableSuggestions(
            phrase,
            selectedDrilldown
          )(dispatch)
            .then((hits) => {
              const hit = hits.find(caseInsensitiveValue)
              setFilterValue(hit)(dispatch)
              clearColumnSuggestions()(dispatch)
            })
            .finally(() => {
              searchInFlight(false)(dispatch)
            })
          break
        case ChipPhraseScopeEnum.COLUMN_TITLE:
          fetchColumnSuggestions(
            phrase,
            selectedDrilldown
          )(dispatch)
            .then((hits) => {
              const hit = hits.find(caseInsensitiveValue)
              setFilterValue(hit)(dispatch)
              clearColumnSuggestions()(dispatch)
            })
            .finally(() => {
              searchInFlight(false)(dispatch)
            })
          break
      }
    })
  }

export const cleanTreemapState =
  (selectedDrilldown?: Readonly<SelectedDrilldown>) =>
  (dispatch: Dispatch): void => {
    if (!selectedDrilldown) return

    clearSearchSuggestions()(dispatch)
    if (isEmpty(selectedDrilldown.column)) {
      clearColumnData()(dispatch)
      clearCommentCounts()(dispatch)
      clearColumnCounts()(dispatch)
      closePreviewDrawer()(dispatch)
    }
    if (isEmpty(selectedDrilldown.table)) {
      clearTableMetadata()(dispatch)
    }
  }
