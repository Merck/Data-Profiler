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
import {
  SET_DATA,
  START_FETCH,
  SET_FILTER,
  SET_METADATA,
  TOGGLE_FULLSCREEN,
} from './store'
import { max } from 'lodash'
import { encodeVariable } from '@dp-ui/lib/dist/helpers/strings'

const LIMIT = 500

export const startFetch = (dispatch) => {
  dispatch({
    type: START_FETCH,
  })
}

export const loadRows = (
  dispatch,
  state,
  api,
  append = false,
  number = LIMIT
) => {
  const { filters, endLocation, dataset, table } = state
  if (!(append && !endLocation)) {
    startFetch(dispatch)
    api
      .post({
        resource: 'data/rows',
        postObject: {
          dataset,
          table,
          filters,
          api_filter: false,
          pageSize: number,
          limit: number,
          startLocation: append ? endLocation : undefined,
        },
      })
      .then((res) =>
        dispatch({
          type: SET_DATA,
          append,
          dataset,
          table,
          allDataLoaded: !Boolean(res.body.endLocation),
          ...res.body,
        })
      )
  }
}

export const initialize = async (
  dispatch,
  getState,
  api,
  dataset,
  table,
  searchTerms,
  searchColInfo
) => {
  await api
    .get({
      resource: `v1/columns/${encodeVariable(dataset)}/${encodeVariable(
        table
      )}`,
    })
    .then((res) => {
      dispatch({
        type: SET_METADATA,
        dataset,
        table,
        types: Object.keys(res.body).reduce(
          (acc, col) => ({ ...acc, [col]: res.body[col].data_type }),
          {}
        ),
        totalRows: max(
          Object.keys(res.body).map((e) => res.body[e].num_values)
        ),
      })
    })

  // if (!isEmpty(searchTerms) && !isEmpty(searchColInfo)) {
  //   dispatch({ type: SET_SEARCH_COL_INFO, searchColInfo, searchTerms })
  // }
  await loadRows(dispatch, getState(), api)
}

export const setFilter = async (dispatch, getState, column, filter, api) => {
  await dispatch({
    type: SET_FILTER,
    column,
    filter,
  })
  loadRows(dispatch, getState(), api)
}

export const toggleFullscreen = (dispatch) => {
  dispatch({
    type: TOGGLE_FULLSCREEN,
  })
}

export const setRowDataFilter = console.error
export const setRowViewerClickIdx = console.error
