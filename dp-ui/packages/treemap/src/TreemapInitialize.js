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
import { DPContext } from '@dp-ui/lib'
import { CircularProgress } from '@material-ui/core'
import { isEmpty } from 'lodash'
import React, { useEffect, useState } from 'react'
import { connect } from 'react-redux'
import { bindActionCreators } from 'redux'
import { setInitialData } from './actions'
import { setDatasetsFromEndpointResp } from './Drawers/actions'
import { getSelectedView } from './MultiSearch/selectors'
import { getSelectedDrilldown } from './drilldown/selectors'
import { updateSelectedViewWithRefresh } from './compositeActions'

function Initialize(props) {
  const [initialized, setInitalized] = useState(!isEmpty(props.treemapData))
  const { api } = props

  useEffect(() => {
    async function initialize() {
      const fetchTreemap = api
        .get({ resource: 'treemap', timeout: 3600000 })
        .then((res) =>
          res.body.children.map((c) => ({
            ...c,
            size: undefined,
            totalSize: c.size,
          }))
        )
        .then((treemapData) => props.setInitialData({ children: treemapData }))
      const promises = [fetchTreemap]
      if (props?.dataprofiler.state.app.datasets) {
        props.setDatasetsFromEndpointResp(
          props?.dataprofiler.state.app.datasets
        )
      } else {
        const fetchPageRefresh = props.updateSelectedViewWithRefresh(
          props?.selectedView
        )
        promises.push(fetchPageRefresh)
      }
      await Promise.all(promises)
      setInitalized(true)
    }
    if (!initialized) {
      initialize()
    }
  }, [initialized, props?.dataprofiler.state.app.datasets])

  if (!initialized) return <CircularProgress />

  return props.children
}

const mapStateToProps = (state) => ({
  treemapData: state.treemapData,
  selectedDrilldown: getSelectedDrilldown(state),
  selectedView: getSelectedView(state),
})

const mapDispatchToProps = (dispatch) =>
  bindActionCreators(
    {
      setInitialData,
      updateSelectedViewWithRefresh,
      setDatasetsFromEndpointResp,
    },
    dispatch
  )

const TreemapInitialize = connect(
  mapStateToProps,
  mapDispatchToProps
)(DPContext(Initialize))

export default (InputComponent) => (props) =>
  (
    <TreemapInitialize {...props}>
      <InputComponent {...props} />
    </TreemapInitialize>
  )
