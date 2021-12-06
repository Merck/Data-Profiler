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
import { isBoolean, isEmpty } from 'lodash'
import {
  fetchAndSetColumnMetadata,
  fetchAndSetTableMetadata,
  setSelectedDrilldown,
} from '../actions'
import { store } from '../index'
import ColumnCounts from '../models/ColumnCounts'
import SelectedDrilldown from '../drilldown/models/SelectedDrilldown'
import { fetchColumnCounts, fetchColumnSampleValues } from '../provider'
import * as descriptionProvider from './provider'
import * as LISTVIEW_ACTIONS from './reducer'
import * as DRILLDOWN_ACTIONS from '../drilldown/reducer'
import { fetchCommentsAtAllLevels } from '../comments/actions'
import * as commentsProvider from '../comments/provider'

export const setListViewDrilldown =
  (
    drilldown?: Readonly<SelectedDrilldown>,
    shouldShowDataTab: Readonly<boolean> = false
  ) =>
  (dispatch: Dispatch): void => {
    if (!drilldown) {
      return
    }

    if (shouldShowDataTab) {
      setTabIndex('data')(dispatch)
    }
    // expand the card, show skeleton while data is loading
    // dispatch({
    //   type: DRILLDOWN_ACTIONS.SET_DRILLDOWN,
    //   payload: drilldown,
    // })
    setSelectedDrilldown(drilldown)(dispatch)

    // request data for card expansion, if the data does not already exist
    const { dataset, table, column } = drilldown

    if (dataset) {
      ensureFetchTableMeta(drilldown, dispatch)
      // comments are only needed in the delta/activity tab for the moment
      fetchCommentsAtAllLevels(SelectedDrilldown.of({ ...drilldown }))(dispatch)
    }

    if (dataset && table) {
      ensureFetchColumnMeta(drilldown, dispatch)
      ensureFetchSamples(drilldown, dispatch)
    }

    if (dataset && table && column) {
      ensureFetchColumnCounts(drilldown)(dispatch)
    }
  }

const ensureFetchTableMeta = (
  drilldown: Readonly<SelectedDrilldown>,
  dispatch: Dispatch
) => {
  const tableMetadata = store?.getState()?.treemap?.selectedDatasetTableMetadata
  fetchWhenNeeded(
    tableMetadata,
    (el) => el?.datasetName === drilldown?.dataset,
    () => {
      isFetchingSamples()(dispatch)
      fetchAndSetTableMetadata(drilldown?.dataset)(dispatch)
    }
  )
}

const ensureFetchColumnMeta = (
  drilldown: Readonly<SelectedDrilldown>,
  dispatch: Dispatch
) => {
  // fetch column level details
  const columnMetadata = store?.getState()?.treemap?.selectedTableColumnMetadata
  fetchWhenNeeded(
    columnMetadata,
    (el) =>
      el?.datasetName === drilldown?.dataset &&
      el?.tableName === drilldown?.table,
    () => {
      isFetchingSamples()(dispatch)
      fetchAndSetColumnMetadata(drilldown?.dataset, drilldown?.table)(dispatch)
    }
  )
}

const ensureFetchSamples = (
  drilldown: Readonly<SelectedDrilldown>,
  dispatch: Dispatch
) => {
  // fetch column samples for every column
  const samples = store?.getState()?.listview?.samples
  const fetchingSamples = store?.getState()?.listview?.isFetchingSamples
  const fetchingSamplesInProgress =
    isBoolean(fetchingSamples) && fetchingSamples === true
  const fetchSamples = () => {
    isFetchingSamples()(dispatch)
    fetchColumnSampleValues({
      dataset: drilldown?.dataset,
      table: drilldown?.table,
    }).then((results) => {
      dispatch({
        type: LISTVIEW_ACTIONS.SET_SAMPLES,
        payload: results,
      })
    })
  }

  const testNeedFetch = (el) =>
    !fetchingSamplesInProgress ||
    (el?.dataset === drilldown?.dataset && el?.table === drilldown?.table)
  fetchWhenNeeded(samples, testNeedFetch, fetchSamples)
}

const ensureFetchColumnCounts =
  (drilldown: Readonly<SelectedDrilldown>) =>
  (dispatch: Dispatch): void => {
    const { dataset, table, column } = drilldown
    // fetch colcounts value for every column
    const colCounts = store?.getState()?.listview.colCounts
    const testNeedFetch = (el: ColumnCounts) =>
      el?.dataset === dataset && el?.table === table && el?.column === column
    if (!colCounts || isEmpty(colCounts.values) || !testNeedFetch(colCounts)) {
      fetchColumnCounts(
        SelectedDrilldown.of({
          dataset,
          table,
          column,
        })
      ).then((results) => {
        dispatch({
          type: LISTVIEW_ACTIONS.SET_COLCOUNTS,
          payload: results,
        })
      })
    }
  }

type ElementCheck<T> = (data?: Readonly<T>) => boolean
type DataFetch = () => void
const fetchWhenNeeded = <T>(
  data: Readonly<T[]>,
  check: ElementCheck<T>,
  fetch: DataFetch
): void => {
  if (!data || isEmpty(data) || !data?.some(check)) {
    // no data found or wrong data found
    //  fetch and update data
    fetch()
  }
}

export const clearAllListViewData =
  () =>
  (dispatch: Dispatch): void => {
    resetIsInitialLoad()(dispatch)
    clearListViewDrilldown()(dispatch)
    clearFetchingSamples()(dispatch)
    clearColumnFetchSize()(dispatch)
    clearColumnCounts()(dispatch)
    clearColumnSamples()(dispatch)
    // clearTabIndex()(dispatch)
  }

export const clearListViewDrilldown =
  () =>
  (dispatch: Dispatch): void => {
    dispatch({
      type: DRILLDOWN_ACTIONS.CLEAR_DRILLDOWN,
    })
  }

export const isFetchingSamples =
  () =>
  (dispatch: Dispatch): void => {
    dispatch({
      type: LISTVIEW_ACTIONS.IS_FETCHING_SAMPLES,
    })
  }

export const clearFetchingSamples =
  () =>
  (dispatch: Dispatch): void => {
    dispatch({
      type: LISTVIEW_ACTIONS.CLEAR_FETCHING_SAMPLES,
    })
  }

export const clearColumnCounts =
  () =>
  (dispatch: Dispatch): void => {
    dispatch({
      type: LISTVIEW_ACTIONS.CLEAR_COLCOUNTS,
    })
  }

export const clearColumnSamples =
  () =>
  (dispatch: Dispatch): void => {
    dispatch({
      type: LISTVIEW_ACTIONS.CLEAR_SAMPLES,
    })
  }

export const clearColumnFetchSize =
  () =>
  (dispatch: Dispatch): void => {
    dispatch({
      type: LISTVIEW_ACTIONS.CLEAR_COLCOUNTS_FETCH_SIZE,
    })
  }

export const setColumnFetchSize =
  (fetchSize: number) =>
  (dispatch: Dispatch): void => {
    dispatch({
      type: LISTVIEW_ACTIONS.SET_COLCOUNTS_FETCH_SIZE,
      payload: fetchSize,
    })
  }

export const setIsInitialLoad =
  (isInitialLoad = true) =>
  (dispatch: Dispatch): void => {
    dispatch({
      type: LISTVIEW_ACTIONS.SET_IS_INITIAL_LOAD,
      payload: isInitialLoad,
    })
  }

export const resetIsInitialLoad =
  () =>
  (dispatch: Dispatch): void => {
    dispatch({
      type: LISTVIEW_ACTIONS.RESET_IS_INITIAL_LOAD,
    })
  }

export const refreshColCounts =
  (drilldown: SelectedDrilldown) =>
  (dispatch: Dispatch): void => {
    const colCountsFetchSize = store?.getState()?.listview?.colCountsFetchSize
    const fetchSize =
      colCountsFetchSize + LISTVIEW_ACTIONS.DEFAULT_COL_COUNTS_FETCH_SIZE
    refreshColCountsWithFetchSize(drilldown, fetchSize)(dispatch)
  }

export const refreshColCountsWithFetchSize =
  (drilldown: SelectedDrilldown, fetchSize: number) =>
  (dispatch: Dispatch): void => {
    setColumnFetchSize(fetchSize)(dispatch)
    fetchColumnCounts(drilldown, fetchSize).then((results) => {
      dispatch({
        type: LISTVIEW_ACTIONS.SET_COLCOUNTS,
        payload: results,
      })
    })
  }

export const setTabIndex =
  (tabIndex: string) =>
  (dispatch: Dispatch): void => {
    dispatch({
      type: LISTVIEW_ACTIONS.SET_TAB_INDEX,
      payload: tabIndex,
    })
  }

export const clearTabIndex =
  () =>
  (dispatch: Dispatch): void => {
    dispatch({
      type: LISTVIEW_ACTIONS.CLEAR_TAB_INDEX,
    })
  }

export const setFetchingProperties =
  (dataset: string) =>
  (dispatch: Dispatch): void => {
    dispatch({
      type: LISTVIEW_ACTIONS.SET_FETCHING_PROPERTIES,
      payload: dataset,
    })
  }

export const getDatasetProperties =
  (datasetName?: string) => (dispatch: Dispatch) => {
    return descriptionProvider.getDatasetProperties(datasetName)
  }

export const getDatasetDescription =
  (datasetName?: string) => (dispatch: Dispatch) => {
    return descriptionProvider.getDatasetDescription(datasetName)
  }

export const setDatasetDescription =
  (datasetName: string, description: string) => (dispatch: Dispatch) => {
    return descriptionProvider.setDatasetDescription(datasetName, description)
  }

export const getMetadataProperties = (datasetName: string) => {
  return descriptionProvider.getMetadataProperties(datasetName)
}

export const fetchQualityTabComments =
  (drilldown: Readonly<SelectedDrilldown>, limit?: number) =>
  (dispatch: Dispatch): void => {
    dispatch({
      type: LISTVIEW_ACTIONS.SET_QUALITY_TAB_COMMENTS_IN_FLIGHT,
      payload: true,
    })

    commentsProvider
      .fetchCommentHierarchyCounts(drilldown, 'tour')
      .then((response) => {
        dispatch({
          type: LISTVIEW_ACTIONS.SET_QUALITY_TAB_COMMENTS_COUNT,
          payload: response?.count,
        })

        commentsProvider
          .fetchHierarchyComments(drilldown, 'tour', limit)
          .then((values) => {
            dispatch({
              type: LISTVIEW_ACTIONS.SET_QUALITY_TAB_COMMENTS,
              payload: values,
            })
            dispatch({
              type: LISTVIEW_ACTIONS.SET_QUALITY_TAB_COMMENTS_IN_FLIGHT,
              payload: false,
            })
          })
          .catch((err) => console.log(err))
      })
      .catch((err) => console.log(err))
  }

export const clearQualityTabComments =
  () =>
  (dispatch: Dispatch): void => {
    dispatch({
      type: LISTVIEW_ACTIONS.SET_QUALITY_TAB_COMMENTS_COUNT,
      payload: null,
    })
    dispatch({
      type: LISTVIEW_ACTIONS.SET_QUALITY_TAB_COMMENTS,
      payload: [],
    })
  }
