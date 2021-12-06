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
import { Dispatch } from '@reduxjs/toolkit'
import { isEmpty, isUndefined } from 'lodash'
import { fetchAndSetColumnMetadata, fetchAndSetTableMetadata } from '../actions'
import { store } from '../index'
import ColumnCounts from '../models/ColumnCounts'
import CommonMetadata from '../models/CommonMetadata'
import MetadataHierarchy from '../models/MetadataHierarchy'
import MetadataResponse from '../models/MetadataResponse'
import SelectedDrilldown, {
  TREEMAP_VIEW_ENUM,
} from '../drilldown/models/SelectedDrilldown'
import * as provider from '../provider'
import MetadataAdapter from '../services/MetadataAdapter'
import { metadataGraphQLQuery } from '../services/MetadataAdapter'
import {
  CLEAR_COLCOUNTS_FETCH_SIZE,
  CLEAR_COLUMN_COUNTS,
  CLEAR_COLUMN_METADATA,
  CLEAR_DATASETS_METADATA,
  CLEAR_PREVIEW_DRAWER_DRILLDOWN,
  CLEAR_TABLE_METADATA,
  CLOSE_PREVIEW_DRAWER,
  DEFAULT_COL_COUNTS_FETCH_SIZE,
  OPEN_PREVIEW_DRAWER,
  SELECT_PREVIEW_DRAWER_DRILLDOWN,
  SET_COLCOUNTS_FETCH_SIZE,
  SET_COLUMN_COUNTS,
  SET_COLUMN_METADATA,
  SET_DATASETS_METADATA,
  SET_TABLE_METADATA,
  TOGGLE_PREVIEW_DRAWER,
  UPDATE_DATASET_METADATA,
} from './reducer'
import * as DRILLDOWN_ACTIONS from '../drilldown/reducer'

/**
 * toggle preview drawer
 */
export const togglePreviewDrawer = () => (dispatch) => {
  dispatch({
    type: TOGGLE_PREVIEW_DRAWER,
  })
}

/**
 * open preview drawer
 */
export const openPreviewDrawer = () => (dispatch) => {
  dispatch({
    type: OPEN_PREVIEW_DRAWER,
  })
}

/**
 * close preview drawer
 */
export const closePreviewDrawer = () => (dispatch) => {
  dispatch({
    type: CLOSE_PREVIEW_DRAWER,
  })
}

/**
 * select a preview drawer column
 */
export const updatePreviewDrawerDrilldown =
  (selectedDrilldown: SelectedDrilldown) => (dispatch) => {
    dispatch({
      type: SELECT_PREVIEW_DRAWER_DRILLDOWN,
      payload: selectedDrilldown.serialize(),
    })
    // dispatch({
    //   type: DRILLDOWN_ACTIONS.SET_DRILLDOWN,
    //   payload: selectedDrilldown.serialize(),
    // })
  }

/**
 * clear the preview drawer column
 */
export const clearPreviewDrawerColumn = () => (dispatch) => {
  dispatch({
    type: CLEAR_PREVIEW_DRAWER_DRILLDOWN,
  })
}

/**
 * set the preview list comments drawer table metadata
 */
export const setTableMetadata = (metadata?: CommonMetadata[]) => (dispatch) => {
  dispatch({
    type: SET_TABLE_METADATA,
    payload: metadata,
  })
}

/**
 * clear the preview list comments drawer table metadata
 */
export const clearTableMetadata = () => (dispatch) => {
  dispatch({
    type: CLEAR_TABLE_METADATA,
  })
}

/**
 * set the preview list comments drawer column metadata
 */
export const setColumnMetadata =
  (metadata?: CommonMetadata[]) => (dispatch) => {
    dispatch({
      type: SET_COLUMN_METADATA,
      payload: metadata,
    })
  }

/**
 * clear the preview list comments drawer column metadata
 */
export const clearColumnMetadata = () => (dispatch) => {
  dispatch({
    type: CLEAR_COLUMN_METADATA,
  })
}

/**
 * TODO: Remove this function if it is no longer used
 * the fetchMetadataHiearchy is more effcient anyways and probably gives the same data?
 *
 * @deprecated
 * @param rootDrilldown
 * @returns
 */
export const fetchMetadataAtAllLevels =
  (rootDrilldown: SelectedDrilldown) => (dispatch) => {
    if (!rootDrilldown || isEmpty(rootDrilldown.dataset)) {
      clearTableMetadata()(dispatch)
      clearColumnMetadata()(dispatch)
      return
    }

    const fetchForAllLevels = (dataset: string, table: string) => {
      fetchAndSetTableMetadata(dataset)(dispatch).then((tableMetadata) => {
        let tableLevelMeta = tableMetadata
        if (!isUndefined(table) && !isEmpty(table)) {
          tableLevelMeta = [
            tableMetadata.find(
              (el) => el.datasetName === dataset && el.tableName === table
            ),
          ]
        }
        setTableMetadata(tableLevelMeta)(dispatch)

        const allColumnMetadata: CommonMetadata[] = []
        const fetchColumnMetaPromises = tableLevelMeta.map((tableMetaDatum) => {
          return fetchAndSetColumnMetadata(
            tableMetaDatum.datasetName,
            tableMetaDatum.tableName
          )(dispatch).then((columnMetadata) =>
            allColumnMetadata.push(...columnMetadata)
          )
        })
        Promise.all(fetchColumnMetaPromises).then(() =>
          setColumnMetadata(allColumnMetadata)(dispatch)
        )
      })
    }

    // starting at given drilldown, get metadata for its subelements
    // eg: starting at a given dataset, get metadata for all subtables and subcolumns
    switch (rootDrilldown.currentView()) {
      case TREEMAP_VIEW_ENUM.TABLE:
        fetchForAllLevels(rootDrilldown.dataset, rootDrilldown.table)
        break
      case TREEMAP_VIEW_ENUM.COLUMN:
        fetchForAllLevels(rootDrilldown.dataset, rootDrilldown.table)
        break
      case TREEMAP_VIEW_ENUM.SPECIFIC_COLUMN:
        fetchForAllLevels(rootDrilldown.dataset, rootDrilldown.table)
        break
      default:
        clearTableMetadata()(dispatch)
        clearColumnMetadata()(dispatch)
        console.log(
          'unknown/unsupported drilldown view while fetching metadata'
        )
    }
  }

export const fetchMetadataHierarchy =
  (drilldown: SelectedDrilldown) => (dispatch) => {
    clearTableMetadata()(dispatch)
    clearColumnMetadata()(dispatch)
    if (!drilldown || isEmpty(drilldown.dataset)) {
      return
    }

    const dataset = drilldown.dataset
    provider
      .fetchMetadataHierarchy(dataset)
      .then((metadata: MetadataHierarchy) => {
        const { tableMetadata, columnMetadata } = metadata
        setTableMetadataFromEndpointResp(tableMetadata)(dispatch)
        setColumnMetadataFromEndpointResp(columnMetadata)(dispatch)
      })
      .catch((err) => {
        clearTableMetadata()(dispatch)
        clearColumnMetadata()(dispatch)
      })
  }

/**
 * TODO: move datasets metadata out of previewdrawer into a common place
 *  between previewdrawer and list view
 *
 * sets the datasets metadata needed for the preview drawer
 * @param metadata
 */
export const setDatasetsMetadata =
  (metadata: CommonMetadata[]) =>
  (dispatch): void => {
    dispatch({
      type: SET_DATASETS_METADATA,
      payload: metadata,
    })
  }

export const clearDatasetsMetadata =
  () =>
  (dispatch): void => {
    dispatch({
      type: CLEAR_DATASETS_METADATA,
    })
  }

export const updateDatasetMetadata =
  (metadata: CommonMetadata) =>
  (dispatch): void => {
    dispatch({
      type: UPDATE_DATASET_METADATA,
      payload: metadata,
    })
  }

/**
 * fetches the datasets metadata needed for the preview drawer
 */
export const fetchAndSetDatasetsMetadata =
  () =>
  (dispatch): Promise<CommonMetadata[]> => {
    return api()
      .post({
        resource: 'v2/datasets/graphql',
        postObject: metadataGraphQLQuery,
      })
      .then((res) => setDatasetsFromEndpointResp(res.body)(dispatch))
      .catch((err) => {
        console.warn(err)
        dispatch({
          type: SET_DATASETS_METADATA,
          payload: [],
        })
      })
  }

export const setDatasetsFromEndpointResp =
  (json: Record<string, Array<MetadataResponse>>) =>
  (dispatch): CommonMetadata[] => {
    const metadataAdapter = new MetadataAdapter()
    const commonFormatMetadata =
      metadataAdapter.datasetMetadataResponseToCommonFormat(json)
    setDatasetsMetadata(commonFormatMetadata)(dispatch)
    return commonFormatMetadata
  }

export const setTableMetadataFromEndpointResp =
  (json: Record<string, MetadataResponse>) =>
  (dispatch): CommonMetadata[] => {
    const metadataAdapter = new MetadataAdapter()
    const commonFormatMetadata =
      metadataAdapter.tableMetadataResponseToCommonFormat(json)
    setTableMetadata(commonFormatMetadata)(dispatch)
    return commonFormatMetadata
  }

export const setColumnMetadataFromEndpointResp =
  (json: Record<string, MetadataResponse>) =>
  (dispatch): CommonMetadata[] => {
    const metadataAdapter = new MetadataAdapter()
    const commonFormatMetadata =
      metadataAdapter.columnMetadataResponseToCommonFormat(json)
    setColumnMetadata(commonFormatMetadata)(dispatch)
    return commonFormatMetadata
  }

export const clearColumnFetchSize =
  () =>
  (dispatch: Dispatch): void => {
    dispatch({
      type: CLEAR_COLCOUNTS_FETCH_SIZE,
    })
  }

export const setColumnFetchSize =
  (fetchSize: number) =>
  (dispatch: Dispatch): void => {
    dispatch({
      type: SET_COLCOUNTS_FETCH_SIZE,
      payload: fetchSize,
    })
  }

export const refreshColCounts =
  (drilldown: SelectedDrilldown) =>
  (dispatch: Dispatch): void => {
    const colCountsFetchSize =
      store?.getState()?.previewdrawer?.colCountsFetchSize
    const fetchSize = colCountsFetchSize + DEFAULT_COL_COUNTS_FETCH_SIZE
    refreshColCountsWithFetchSize(drilldown, fetchSize)(dispatch)
  }

export const refreshColCountsWithFetchSize =
  (drilldown: SelectedDrilldown, fetchSize: number) =>
  (dispatch: Dispatch): void => {
    setColumnFetchSize(fetchSize)(dispatch)
    provider.fetchColumnCounts(drilldown, fetchSize).then((results) => {
      dispatch({
        type: SET_COLUMN_COUNTS,
        payload: results,
      })
    })
  }

/**
 * clear the preview drawer column counts values
 */
export const clearColumnCounts = () => (dispatch) => {
  dispatch({
    type: CLEAR_COLUMN_COUNTS,
  })
  // createAction(CLEAR_COLUMN_COUNTS)()
}

/**
 * set the preview drawer column counts
 */
export const setColumnCounts = (colCounts: ColumnCounts) => (dispatch) => {
  dispatch({
    type: SET_COLUMN_COUNTS,
    payload: colCounts,
  })
}

/**
 * fetch the preview drawer column counts
 */
export const fetchColumnCounts =
  (selectedDrilldown?: SelectedDrilldown) => (dispatch) => {
    dispatch(clearColumnCounts())
    provider
      .fetchColumnCounts(selectedDrilldown)
      .then((values) => {
        dispatch({
          type: SET_COLUMN_COUNTS,
          payload: values,
        })
      })
      .catch((err) =>
        dispatch({
          type: CLEAR_COLUMN_COUNTS,
        })
      )
  }
