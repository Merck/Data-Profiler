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
import React, { useContext, useEffect } from 'react'
import { RowViewerContext } from './store'
import { DPContext } from '@dp-ui/lib'
import { initialize } from './actions'
import { CircularProgress } from '@material-ui/core'
import { Overlay } from 'react-portal-overlay'
import RowViewer from './RowViewer'
import { isEmpty } from 'lodash'
import { SET_SEARCH_COL_INFO } from './store'

const Wrapper = (props) => {
  const [state, dispatch, getState] = useContext(RowViewerContext)
  const { dataset, table, api, searchTerms, searchColInfo } = props
  // const { filter, hoveredValue } = props?.treemap
  // const search = searchTerms || filter?.value || hoveredValue?.value || []
  useEffect(() => {
    async function fetchData() {
      await initialize(
        dispatch,
        getState,
        api,
        dataset,
        table,
        searchTerms,
        searchColInfo
      )

      // if (!isEmpty(searchTerms) && !isEmpty(searchColInfo)) {
      //   dispatch({ type: SET_SEARCH_COL_INFO, searchColInfo, searchTerms })
      // }
    }
    fetchData()
  }, [dataset, table])

  useEffect(() => {
    const applyGlobalSearchTerms = () => {
      dispatch({ type: SET_SEARCH_COL_INFO, searchColInfo, searchTerms })
      // if (!isEmpty(searchTerms) && !isEmpty(searchColInfo)) {
    }
    applyGlobalSearchTerms()
  }, [searchTerms, searchColInfo])

  const { sortedColumns, types } = state

  if (isEmpty(types) || isEmpty(sortedColumns))
    return (
      <CircularProgress
        style={{
          position: 'absolute',
          left: 0,
          right: 0,
          top: 0,
          bottom: 0,
          margin: 'auto',
        }}
      />
    )
  return (
    <div key={`${dataset}${table}`}>
      <Overlay
        open={state.fullscreen}
        animation={{ duration: 0 }}
        defaultPortalStyles={{
          position: 'relative',
          backgroundColor: '#FFF',
          zIndex: 2147483649,
        }}>
        <RowViewer />
      </Overlay>
      {!state.fullscreen && (
        <div style={{ maxWidth: '94vw', margin: 'auto auto' }}>
          <RowViewer tableBodyHeightPx={props.tableBodyHeightPx} />
        </div>
      )}
    </div>
  )
}

export default DPContext(Wrapper)
