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
import CommonMetadata from './models/CommonMetadata'
import SelectedView from './models/SelectedView'
import TreemapObject from './models/TreemapObject'
import AggregatedSearchResult from './MultiSearch/models/AggregatedSearchResult'
import RowViewerLaunch from './models/RowViewerLaunch'

// define action types
// export const SET_SELECTED_DRILLDOWN = 'treemap/set_selected_drilldown'
export const CLEAR_SELECTED_DRILLDOWN_METADATA =
  'treemap/clear_selected_drilldown_metadata'
export const SET_SELECTED_VIEW = 'treemap/set_selected_view'
export const CLEAR_SELECTED_VIEW = 'treemap/clear_selected_view'
export const SET_SELECTED_DATASET_TABLE_METADATA =
  'treemap/set_selected_dataset_table_metadata'
export const SET_SELECTED_TABLE_COLUMN_METADATA =
  'treemap/set_selected_table_column_metadata'
export const SET_FILTER = 'treemap/set_filter'
export const SET_HOVERED_VALUE = 'treemap/set_hovered_value'
export const SET_INITIAL_DATA = 'treemap/set_initial_data'
export const SET_SELECTED_COLUMN_DATA = 'treemap/set_selected_column_data'
export const OPEN_ROW_VIEWER = 'treemap/open_row_viewer'
export const CLOSE_ROW_VIEWER = 'treemap/close_row_viewer'
export const SET_SEARCH_IN_FLIGHT = 'treemap/set_search_in_flight'
interface Command {
  readonly type: string
  readonly payload?: any
}

interface ClearSelectedDrilldownMetadata extends Command {
  readonly type: typeof CLEAR_SELECTED_DRILLDOWN_METADATA
}

interface SetSelectedView extends Command {
  readonly type: typeof SET_SELECTED_VIEW
  readonly payload: Record<string, string>
}

interface ClearSelectedView extends Command {
  readonly type: typeof CLEAR_SELECTED_VIEW
}

interface SetSelectedDatasetTableMetadata extends Command {
  readonly type: typeof SET_SELECTED_DATASET_TABLE_METADATA
  readonly payload: Array<CommonMetadata>
}

interface SetSelectedTableColumnMetadata extends Command {
  readonly type: typeof SET_SELECTED_TABLE_COLUMN_METADATA
  readonly payload: Array<CommonMetadata>
}

interface SetFilter extends Command {
  readonly type: typeof SET_FILTER
  readonly payload: AggregatedSearchResult
}

interface SetHoveredValue extends Command {
  readonly type: typeof SET_HOVERED_VALUE
  readonly payload: AggregatedSearchResult
}

interface SetInitialData extends Command {
  readonly type: typeof SET_INITIAL_DATA
  readonly payload: Array<any>
}

interface SetSelectedColumnData extends Command {
  readonly type: typeof SET_SELECTED_COLUMN_DATA
  readonly payload: Array<TreemapObject>
}

interface OpenRowViewer extends Command {
  readonly type: typeof OPEN_ROW_VIEWER
  readonly payload: RowViewerLaunch
}

interface CloseRowViewer extends Command {
  readonly type: typeof CLOSE_ROW_VIEWER
}

interface SetSearchInFlight extends Command {
  readonly type: typeof SET_SEARCH_IN_FLIGHT
}

export type TreemapActionTypes =
  | SetSelectedDatasetTableMetadata
  | SetSelectedTableColumnMetadata
  | SetFilter
  | SetHoveredValue
  | SetInitialData
  | ClearSelectedDrilldownMetadata
  | SetSelectedColumnData
  | SetSelectedView
  | ClearSelectedView
  | OpenRowViewer
  | CloseRowViewer
  | SetSearchInFlight

export interface TreemapState {
  treemapData: Array<TreemapObject>
  selectedColumnData: Array<TreemapObject>
  selectedView: Record<string, string>
  selectedDatasetTableMetadata?: Array<CommonMetadata>
  selectedTableColumnMetadata?: Array<CommonMetadata>
  hoveredValue: AggregatedSearchResult
  filter: AggregatedSearchResult
  rowViewer: RowViewerLaunch
  isSearchInFlight: boolean
}

export const generateInitialState = (): TreemapState => {
  return {
    treemapData: null,
    selectedColumnData: null,
    selectedView: new SelectedView().serialize(),
    selectedDatasetTableMetadata: null,
    selectedTableColumnMetadata: null,
    hoveredValue: null,
    filter: null,
    rowViewer: {
      dataset: null,
      table: null,
      previousViewMode: null,
      searchTerms: null,
    },
    isSearchInFlight: false,
  }
}

const initialState = generateInitialState()

export default (
  state = initialState,
  action: TreemapActionTypes
): TreemapState => {
  switch (action.type) {
    case SET_INITIAL_DATA:
      return {
        ...state,
        treemapData: action.payload,
      }
    case CLEAR_SELECTED_DRILLDOWN_METADATA:
      return {
        ...state,
        selectedDatasetTableMetadata: null,
        selectedTableColumnMetadata: null,
        selectedColumnData: null,
      }
    case SET_SELECTED_VIEW:
      return {
        ...state,
        selectedView: action.payload,
      }
    case CLEAR_SELECTED_VIEW:
      return {
        ...state,
        selectedView: new SelectedView().serialize(),
      }
    case SET_SELECTED_DATASET_TABLE_METADATA:
      return {
        ...state,
        selectedDatasetTableMetadata: action.payload,
      }
    case SET_SELECTED_TABLE_COLUMN_METADATA:
      return {
        ...state,
        selectedTableColumnMetadata: action.payload,
      }
    case SET_SELECTED_COLUMN_DATA:
      return {
        ...state,
        selectedColumnData: action.payload,
      }
    case SET_HOVERED_VALUE:
      return {
        ...state,
        hoveredValue: action.payload,
      }
    case SET_FILTER:
      return {
        ...state,
        filter: action.payload,
      }
    case OPEN_ROW_VIEWER:
      return {
        ...state,
        rowViewer: action.payload,
      }
    case CLOSE_ROW_VIEWER:
      return {
        ...state,
        rowViewer: generateInitialState().rowViewer,
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
