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
import React from 'react'
import { Button } from '@material-ui/core'
import RowViewerIcon from '@material-ui/icons/TableChart'
import BackIcon from '@material-ui/icons/ArrowBack'
import { createStyles, makeStyles } from '@material-ui/core/styles'
import { bindActionCreators, Dispatch } from '@reduxjs/toolkit'
import { connect } from 'react-redux'
import { openRowViewer, closeRowViewer } from '../../actions'
import { SELECTED_VIEW_ENUM } from '../../models/SelectedView'
import { getSelectedView, getSearchTerms } from '../../MultiSearch/selectors'
import { capitalize } from 'lodash'

const styles = () =>
  createStyles({
    rowViewerLaunchButton: {
      margin: 8,
      // marginLeft: 0,
      textTransform: 'unset',
    },
    rowViewerLaunchButtonIcon: {
      marginRight: 8,
      width: '0.75em',
      height: '0.75em',
    },
  })

const useStyles = makeStyles(styles)

const RowViewerLaunchButton = (props) => {
  const classes = useStyles()

  if (props.selectedView?.view === SELECTED_VIEW_ENUM.ROW_VIEWER) {
    return (
      <Button
        onClick={() => props.closeRowViewer(props.rowViewer.previousViewMode)}
        className={classes.rowViewerLaunchButton}
        color="primary"
        size="small"
        variant="outlined">
        <BackIcon className={classes.rowViewerLaunchButtonIcon} />{' '}
        {`back to ${capitalize(props.rowViewer.previousViewMode)} `}
      </Button>
    )
  }

  return (
    <Button
      onClick={() =>
        props.openRowViewer(
          props.dataset,
          props.table,
          props.launchedFrom,
          props.searchTerms
        )
      }
      className={classes.rowViewerLaunchButton}
      color="primary"
      size="small"
      variant="contained">
      Row Viewer
    </Button>
  )
}

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators(
    {
      openRowViewer,
      closeRowViewer,
    },
    dispatch
  )

const mapStateToProps = (state) => ({
  rowViewer: state.treemap.rowViewer,
  selectedView: getSelectedView(state),
  searchTerms: getSearchTerms(state),
})

const connector = connect(mapStateToProps, mapDispatchToProps)

export default connector(RowViewerLaunchButton)
