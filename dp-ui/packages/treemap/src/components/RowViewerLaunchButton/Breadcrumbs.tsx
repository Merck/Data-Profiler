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
import { Button } from '@material-ui/core'
import { createStyles, makeStyles } from '@material-ui/core/styles'
import { bindActionCreators, Dispatch } from '@reduxjs/toolkit'
import React from 'react'
import { connect } from 'react-redux'
import DatasetChip from '../../components/DatasetChip'

import TableChip from '../../components/TableChip'

import { SELECTED_VIEW_ENUM } from '../../models/SelectedView'
import { closeRowViewer } from '../../actions'
import RowViewerLaunchButton from '../RowViewerLaunchButton'

const styles = () =>
  createStyles({
    breadcrumbWrapper: {
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      background: '#fff',
      marginRight: '8px',
      marginTop: '12px',
      height: '48px',
    },
    breadCrumbBar: {
      display: 'flex',
      alignItems: 'center',
      flex: 1,
    },
    rowViewerLaunchButton: {
      margin: 8,
      textTransform: 'lowercase',
    },
    rowViewerLaunchButtonIcon: {
      marginRight: 8,
      width: '0.75em',
      height: '0.75em',
    },
    separator: {
      margin: '0 10px',
    },
  })

const useStyles = makeStyles(styles)

const Breadcrumbs = (props) => {
  const classes = useStyles()
  const close = () => {
    props.closeRowViewer(props.rowViewer.previousViewMode)
  }
  return (
    <div className={classes.breadcrumbWrapper}>
      <div className={classes.breadCrumbBar}>
        <Button onClick={close}>All Datasets</Button>
        {props.rowViewer.dataset && (
          <span key={props.rowViewer.dataset}>
            <span className={classes.separator}>{'>'}</span>
            <Button onClick={close}>{props.rowViewer.dataset}</Button>
            {!props.rowViewer.table && <DatasetChip></DatasetChip>}
          </span>
        )}
        {props.rowViewer.table && (
          <span key={props.rowViewer.table}>
            <span className={classes.separator}>{'>'}</span>
            <Button onClick={close}>{props.rowViewer.table}</Button>
            <TableChip></TableChip>
          </span>
        )}
      </div>
      {props.rowViewer.table && (
        <RowViewerLaunchButton
          dataset={props.rowViewer.dataset}
          table={props.rowViewer.table}
          launchedFrom={SELECTED_VIEW_ENUM.LIST}
        />
      )}
    </div>
  )
}

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators(
    {
      closeRowViewer,
    },
    dispatch
  )

const mapStateToProps = (state) => ({
  rowViewer: state.treemap.rowViewer,
})

const connector = connect(mapStateToProps, mapDispatchToProps)

export default connector(Breadcrumbs)
