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
import { api } from '@dp-ui/lib'
import { encodeVariable } from '@dp-ui/lib/dist/helpers/strings'
import { isEmpty } from 'lodash'
import { devOffline } from './devOffline'
import { DEV_OFFLINE } from '@dp-ui/parent/src/features'
import { objectKeysToArray } from './helpers/ObjectHelper'
import CommonMetadata from './models/CommonMetadata'
import SelectedDrilldown from './drilldown/models/SelectedDrilldown'
import SelectedView, { SELECTED_VIEW_ENUM } from './models/SelectedView'
import TreemapObject from './models/TreemapObject'
import AggregatedSearchResult from './MultiSearch/models/AggregatedSearchResult'
import * as TREEMAP_ACTIONS from './reducer'
import * as DRILLDOWN_ACTIONS from './drilldown/reducer'
import MetadataAdapter from './services/MetadataAdapter'
import TreemapAdapter from './services/TreemapAdapter'

/**
 * set the treemap data
 * @param treemapData
 */
export const setInitialData = (treemapData: TreemapObject) => (dispatch) => {
  dispatch({
    type: TREEMAP_ACTIONS.SET_INITIAL_DATA,
    payload: treemapData,
  })
}

/**
 * NOTE: this is a low level setting of the selected drilldown
 * if you would like to set selected drilldown and correctly load the various state
 * @see `updateSelectedDrilldownWithRefresh` in compositeActions.ts
 *
 * set and remember the currently selected dataset, happens on a dataset cell click
 * @param selectedDrilldown
 */
export const setSelectedDrilldown =
  (selectedDrilldown = new SelectedDrilldown()) =>
  (dispatch) => {
    const payload = SelectedDrilldown.of(selectedDrilldown).serialize()
    dispatch({
      type: DRILLDOWN_ACTIONS.SET_DRILLDOWN,
      payload: payload,
    })
  }

/**
 * set and remember the currently selected view
 * e.g. treemap view, list view, etc...
 * @param setSelectedView
 */
export const setSelectedView = (selectedView: SelectedView) => (dispatch) => {
  dispatch({
    type: TREEMAP_ACTIONS.SET_SELECTED_VIEW,
    payload: selectedView.serialize(),
  })
}

/**
 * set the hovered value, happens on a search result row hover
 * @param hoveredValue
 */
export const setHoveredValue =
  (hoveredValue: AggregatedSearchResult) => (dispatch) => {
    dispatch({
      type: TREEMAP_ACTIONS.SET_HOVERED_VALUE,
      payload: hoveredValue,
    })
  }

/**
 * set the currently selected search result, happens on search result row click
 * @param filter
 */
export const setFilterValue =
  (filter: AggregatedSearchResult) => (dispatch) => {
    dispatch({
      type: TREEMAP_ACTIONS.SET_FILTER,
      payload: filter,
    })
  }

/**
 * fetches the table metadata needed for the snackbar. Called in time on a dataset cell click
 * @param selectedDataset
 */
export const fetchAndSetTableMetadata =
  (selectedDataset: string) =>
  (dispatch): Promise<CommonMetadata[]> => {
    const dataset = (selectedDataset || '').trim()
    if (isEmpty(dataset)) {
      dispatch({
        type: TREEMAP_ACTIONS.SET_SELECTED_DATASET_TABLE_METADATA,
        payload: [],
      })
      return
    }
    //  GET v1/tables/:dataset endpoint
    const resource = `v1/tables/${encodeVariable(dataset)}`
    const promise = DEV_OFFLINE
      ? Promise.resolve(devOffline(resource))
      : api().get({ resource })
    return promise
      .then((res) => {
        const json = res.body
        // convert response json from a single keyed element to an array
        const selectedMetadata = objectKeysToArray<any>(json)
        const metadataAdapter = new MetadataAdapter()
        const commonFormatMetadata =
          metadataAdapter.tableMetadataToCommonFormat(selectedMetadata)
        dispatch({
          type: TREEMAP_ACTIONS.SET_SELECTED_DATASET_TABLE_METADATA,
          payload: commonFormatMetadata,
        })
        return commonFormatMetadata
      })
      .catch((err) => {
        console.warn(err)
        dispatch({
          type: TREEMAP_ACTIONS.SET_SELECTED_DATASET_TABLE_METADATA,
          payload: [],
        })
      })
  }

/**
 * fetches the column metadata needed for the snackbar. Called in time on a table cell click
 * @param selectedDataset
 * @param selectedTable
 */
export const fetchAndSetColumnMetadata =
  (selectedDataset: string, selectedTable: string) =>
  (dispatch): Promise<CommonMetadata[]> => {
    console.log('test')
    console.log('test')
    const dataset = (selectedDataset || '').trim()
    const table = (selectedTable || '').trim()
    if (isEmpty(dataset)) {
      dispatch({
        type: TREEMAP_ACTIONS.SET_SELECTED_TABLE_COLUMN_METADATA,
        payload: [],
      })
      return
    }
    //  GET v1/columns/:dataset/:table endpoint
    const resource = `v1/columns/${encodeVariable(dataset)}/${encodeVariable(
      table
    )}`
    const promise = DEV_OFFLINE
      ? Promise.resolve(devOffline(resource))
      : api().get({ resource })
    return promise
      .then((res) => {
        const json = res.body
        // convert response json from a single keyed element to an array
        const columnData = objectKeysToArray<any>(json)
        const metadataAdapter = new MetadataAdapter()
        const commonFormatMetadata =
          metadataAdapter.columnMetadataToCommonFormat(columnData)
        const columnAdapter = new TreemapAdapter()
        const selectedColumnData =
          columnAdapter.columnDataToTreemapObject(columnData)
        dispatch({
          type: TREEMAP_ACTIONS.SET_SELECTED_COLUMN_DATA,
          payload: selectedColumnData,
        })
        dispatch({
          type: TREEMAP_ACTIONS.SET_SELECTED_TABLE_COLUMN_METADATA,
          payload: commonFormatMetadata,
        })
        return commonFormatMetadata
      })
      .catch((err) => {
        console.warn(err)
        dispatch({
          type: TREEMAP_ACTIONS.SET_SELECTED_TABLE_COLUMN_METADATA,
          payload: [],
        })
      })
  }

export const clearSelectedView =
  () =>
  (dispatch): void => {
    dispatch({
      type: TREEMAP_ACTIONS.CLEAR_SELECTED_VIEW,
    })
  }

export const clearSelectedDrilldownMetadata =
  () =>
  (dispatch): void => {
    dispatch({
      type: TREEMAP_ACTIONS.CLEAR_SELECTED_DRILLDOWN_METADATA,
    })
  }

export const clearColumnData =
  () =>
  (dispatch): void => {
    dispatch({
      type: TREEMAP_ACTIONS.SET_SELECTED_TABLE_COLUMN_METADATA,
      payload: undefined,
    })
    dispatch({
      type: TREEMAP_ACTIONS.SET_SELECTED_COLUMN_DATA,
      payload: undefined,
    })
  }

export const clearTableMetadata =
  () =>
  (dispatch): void => {
    dispatch({
      type: TREEMAP_ACTIONS.SET_SELECTED_DATASET_TABLE_METADATA,
      payload: undefined,
    })
  }

export const openRowViewer =
  (
    dataset: string,
    table: string,
    previousViewMode: SELECTED_VIEW_ENUM,
    searchTerms: string[]
  ) =>
  (dispatch): void => {
    dispatch({
      type: TREEMAP_ACTIONS.OPEN_ROW_VIEWER,
      payload: { dataset, table, previousViewMode, searchTerms },
    })
    dispatch(setSelectedView(new SelectedView(SELECTED_VIEW_ENUM.ROW_VIEWER)))
  }

export const closeRowViewer =
  (viewMode: SELECTED_VIEW_ENUM) =>
  (dispatch): void => {
    dispatch(setSelectedView(new SelectedView(viewMode)))
    dispatch({
      type: TREEMAP_ACTIONS.CLOSE_ROW_VIEWER,
    })
  }

export const searchInFlight =
  (isInFlight = true) =>
  (dispatch): void => {
    dispatch({
      type: TREEMAP_ACTIONS.SET_SEARCH_IN_FLIGHT,
      payload: isInFlight,
    })
  }
