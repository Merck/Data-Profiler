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
import { CircularProgress } from '@material-ui/core'
import { get, has } from 'lodash'
import React from 'react'
import { connect } from 'react-redux'
import { compose } from 'redux'
import { getSelectedDrilldown } from './drilldown/selectors'
import { getSelectedView } from './MultiSearch/selectors'

const withLoadingSpinner = (C) => (props) => {
  let isLoading = false
  const hasDatasetsInitialized = has(props, 'dataprofiler.state.app.datasets')
  const isDatasetsInitializing = !Boolean(hasDatasetsInitialized)
  const tableMetadataPath = 'treemap.selectedDatasetTableMetadata'
  const columnDataPath1 = 'treemap.selectedTableColumnMetadata'
  const columnDataPath2 = 'treemap.selectedColumnData'
  const selectedDrilldown = getSelectedDrilldown(props.state)
  const isTableViewAndMissingTableMetadata =
    has(selectedDrilldown, 'dataset') &&
    selectedDrilldown.isTableView() &&
    has(props.state, tableMetadataPath) &&
    !Boolean(get(props.state, tableMetadataPath))
  const isColumnViewAndMissingColumnMetadata =
    has(selectedDrilldown, 'table') &&
    selectedDrilldown.isColumnView() &&
    has(props.state, columnDataPath1) &&
    has(props.state, columnDataPath2) &&
    !Boolean(get(props.state, columnDataPath1)) &&
    !Boolean(get(props.state, columnDataPath2))
  const selectedView = getSelectedView(props.state)?.view
  if (
    isDatasetsInitializing ||
    (selectedView === 'treemap' &&
      (isTableViewAndMissingTableMetadata ||
        isColumnViewAndMissingColumnMetadata))
  ) {
    isLoading = true
  }
  return isLoading ? <CircularProgress /> : <C {...props} />
}

const mapStateToProps = (state) => ({ state })

export default compose(connect(mapStateToProps), withLoadingSpinner)
