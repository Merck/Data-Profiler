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
import React, { createContext } from 'react'
import { omit, isEmpty } from 'lodash'
import useThunkReducer from '@dp-ui/lib/dist/helpers/useThunkReducer'
export const SET_DATA = 'rows/SET_DATA'
export const START_FETCH = 'rows/START_FETCH'
export const SET_FILTER = 'rows/SET_FILTER'
export const SET_METADATA = 'rows/SET_METADATA'
export const TOGGLE_FULLSCREEN = 'rows/TOGGLE_FULLSCREEN'
export const SET_OFFICE_AUTH = 'rows/SET_OFFICE_AUTH'
export const SET_SEARCH_COL_INFO = 'rows/SET_SEARCH_COL_INFO'

export const RowViewerContext = createContext()

const initialState = {
  office365AuthToken: null,
  fullscreen: false,
  totalRows: null,
  types: null,
  dataset: null,
  table: null,
  count: null,
  endLocation: null,
  rows: null,
  columns: null,
  filters: {},
  sortedColumns: null,
  columnWidths: {},
  allDataLoaded: false,
  loading: false,
  initialized: false,
  searchColInfo: {},
  searchTerms: null,
}

const reducer = (state, action) => {
  switch (action.type) {
    case SET_DATA:
      return {
        ...state,
        dataset: action.dataset,
        table: action.table,
        count: action.count,
        endLocation: action.endLocation,
        rows: action.append ? [...state.rows, ...action.rows] : action.rows,
        sortedColumns: action.sortedColumns,
        allDataLoaded: action.allDataLoaded,
        loading: false,
        initialized: true,
      }

    case TOGGLE_FULLSCREEN:
      return {
        ...state,
        fullscreen: !state.fullscreen,
      }

    case SET_FILTER:
      if (isEmpty(action.filter)) {
        return {
          ...state,
          filters: omit(state.filters, action.column),
          endLocation: null,
        }
      }
      return {
        ...state,
        filters: {
          ...state.filters,
          [action.column]: action.filter.map((e) => e.value),
        },
      }

    case START_FETCH:
      return {
        ...state,
        loading: true,
      }

    case SET_OFFICE_AUTH:
      return {
        ...state,
        office365AuthToken: action.office365AuthToken,
      }

    case SET_METADATA:
      return {
        ...state,
        dataset: action.dataset,
        table: action.table,
        types: action.types,
        totalRows: action.totalRows,
      }

    case SET_SEARCH_COL_INFO:
      return {
        ...state,
        searchColInfo: action.searchColInfo,
        searchTerms: action.searchTerms,
      }

    default:
      throw new Error()
  }
}

export const RowViewerContextProvider = (props) => {
  const [state, dispatch, getState] = useThunkReducer(reducer, initialState)

  return (
    <RowViewerContext.Provider value={[state, dispatch, getState]}>
      {props.children}
    </RowViewerContext.Provider>
  )
}
