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

import ColumnCounts from '../models/ColumnCounts'
import CommonMetadata from '../models/CommonMetadata'
import SelectedDrilldown from '../drilldown/models/SelectedDrilldown'

// define action types
export const TOGGLE_PREVIEW_DRAWER = 'previewdrawer/toggle'
export const OPEN_PREVIEW_DRAWER = 'previewdrawer/open'
export const CLOSE_PREVIEW_DRAWER = 'previewdrawer/close'
export const SELECT_PREVIEW_DRAWER_DRILLDOWN = 'previewdrawer/select_drilldown'
export const CLEAR_PREVIEW_DRAWER_DRILLDOWN = 'previewdrawer/clear_drilldown'
// // for preview meta drawers
export const SET_COLUMN_COUNTS = 'previewdrawer/set_column_counts'
export const CLEAR_COLUMN_COUNTS = 'previewdrawer/clear_column_counts'
// for preview datasets meta drawers
export const SET_DATASETS_METADATA = 'previewdrawer/set_datasets_metadata'
export const CLEAR_DATASETS_METADATA = 'previewdrawer/clear_datasets_metadata'
export const UPDATE_DATASET_METADATA = 'previewdrawer/update_dataset_metadata'
// for table and column drilldown drawers
export const SET_TABLE_METADATA = 'previewdrawer/set_table_metadata'
export const CLEAR_TABLE_METADATA = 'previewdrawer/clear_table_metadata'
export const SET_COLUMN_METADATA = 'previewdrawer/set_column_metadata'
export const CLEAR_COLUMN_METADATA = 'previewdrawer/clear_column_metadata'
export const SET_COLCOUNTS_FETCH_SIZE = 'previewdrawer/set_colcounts_fetch_size'
export const CLEAR_COLCOUNTS_FETCH_SIZE =
  'previewdrawer/clear_colcounts_fetch_size'

interface Command {
  readonly type: string
  readonly payload?: any
}

interface TogglePreviewDrawer extends Command {
  readonly type: typeof TOGGLE_PREVIEW_DRAWER
}

interface OpenPreviewDrawer extends Command {
  readonly type: typeof OPEN_PREVIEW_DRAWER
}

interface ClosePreviewDrawer extends Command {
  readonly type: typeof CLOSE_PREVIEW_DRAWER
}

interface SelectPreviewDrawerDrilldown extends Command {
  readonly type: typeof SELECT_PREVIEW_DRAWER_DRILLDOWN
  readonly payload: Record<string, string>
}

interface ClearPreviewDrawerDrilldown extends Command {
  readonly type: typeof CLEAR_PREVIEW_DRAWER_DRILLDOWN
}

interface SetColumnCounts extends Command {
  readonly type: typeof SET_COLUMN_COUNTS
  readonly payload: ColumnCounts
}

interface ClearColumnCounts extends Command {
  readonly type: typeof CLEAR_COLUMN_COUNTS
}

interface SetTableMetadata extends Command {
  readonly type: typeof SET_TABLE_METADATA
}

interface ClearTableMetadata extends Command {
  readonly type: typeof CLEAR_TABLE_METADATA
}

interface SetColumnMetadata extends Command {
  readonly type: typeof SET_COLUMN_METADATA
}

interface ClearColumnMetadata extends Command {
  readonly type: typeof CLEAR_COLUMN_METADATA
}

interface SetDatasetsMetadata extends Command {
  readonly type: typeof SET_DATASETS_METADATA
}

interface ClearDatasetsMetadata extends Command {
  readonly type: typeof CLEAR_DATASETS_METADATA
}

interface UpdateDatasetMetadata extends Command {
  readonly type: typeof UPDATE_DATASET_METADATA
}

interface SetColCountsFetchSize extends Command {
  readonly type: typeof SET_COLCOUNTS_FETCH_SIZE
  readonly payload: number
}

interface ClearColCountsFetchSize extends Command {
  readonly type: typeof CLEAR_COLCOUNTS_FETCH_SIZE
}

export type PreviewDrawerActionTypes =
  | TogglePreviewDrawer
  | OpenPreviewDrawer
  | ClosePreviewDrawer
  | SelectPreviewDrawerDrilldown
  | ClearPreviewDrawerDrilldown
  | SetColumnCounts
  | ClearColumnCounts
  | SetTableMetadata
  | ClearTableMetadata
  | SetColumnMetadata
  | ClearColumnMetadata
  | SetDatasetsMetadata
  | ClearDatasetsMetadata
  | UpdateDatasetMetadata
  | SetColCountsFetchSize
  | ClearColCountsFetchSize

// define state reducer
export interface PreviewDrawerState {
  // for all drawers
  isOpen: boolean
  drawerSelectedDrilldown: Record<string, string>
  columnCounts: ColumnCounts
  // for dataset preview meta drawer
  datasetsMeta: Array<CommonMetadata>
  tableMeta: Array<CommonMetadata>
  columnMeta: Array<CommonMetadata>
  colCountsFetchSize: number
}

export const DEFAULT_COL_COUNTS_FETCH_SIZE = 300
export const generateInitialState = (): PreviewDrawerState => {
  return {
    isOpen: false,
    columnCounts: undefined,
    tableMeta: undefined,
    columnMeta: undefined,
    datasetsMeta: undefined,
    drawerSelectedDrilldown: new SelectedDrilldown().serialize(),
    colCountsFetchSize: DEFAULT_COL_COUNTS_FETCH_SIZE,
  }
}

const initialState = generateInitialState()

export default (
  state: PreviewDrawerState = initialState,
  action: PreviewDrawerActionTypes
): PreviewDrawerState => {
  switch (action.type) {
    case TOGGLE_PREVIEW_DRAWER:
      return {
        ...state,
        isOpen: !state.isOpen,
      }
    case OPEN_PREVIEW_DRAWER:
      return {
        ...state,
        isOpen: true,
      }
    case CLOSE_PREVIEW_DRAWER:
      return {
        ...state,
        isOpen: false,
      }
    case SELECT_PREVIEW_DRAWER_DRILLDOWN:
      return {
        ...state,
        drawerSelectedDrilldown: action.payload,
      }
    case CLEAR_PREVIEW_DRAWER_DRILLDOWN:
      return {
        ...state,
        drawerSelectedDrilldown: new SelectedDrilldown().serialize(),
      }
    case SET_COLUMN_COUNTS:
      return {
        ...state,
        columnCounts: action.payload,
      }
    case CLEAR_COLUMN_COUNTS:
      return {
        ...state,
        columnCounts: undefined,
      }
    case SET_TABLE_METADATA:
      return {
        ...state,
        tableMeta: [...action.payload],
      }
    case CLEAR_TABLE_METADATA:
      return {
        ...state,
        tableMeta: undefined,
      }
    case SET_COLUMN_METADATA:
      return {
        ...state,
        columnMeta: [...action.payload],
      }
    case CLEAR_COLUMN_METADATA:
      return {
        ...state,
        columnMeta: undefined,
      }
    case SET_DATASETS_METADATA:
      return {
        ...state,
        datasetsMeta: [...action.payload],
      }
    case CLEAR_DATASETS_METADATA:
      return {
        ...state,
        datasetsMeta: undefined,
      }
    case UPDATE_DATASET_METADATA:
      const newMeta = [...state.datasetsMeta] // Copy for immutable
      for (const dsMetaidx in newMeta) {
        if (newMeta[dsMetaidx].datasetName === action.payload.datasetName) {
          newMeta[dsMetaidx] = { ...action.payload } // Update copy
          break
        }
      }
      return {
        ...state,
        datasetsMeta: [...newMeta],
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
    default:
      return state
  }
}
