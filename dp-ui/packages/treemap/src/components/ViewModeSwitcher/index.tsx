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
import Button from '@material-ui/core/Button'
import { createStyles, Theme, withStyles } from '@material-ui/core/styles'
import ListOutlinedIcon from '@material-ui/icons/ListOutlined'
import ViewQuiltOutlinedIcon from '@material-ui/icons/ViewQuiltOutlined'
import RowViewer from '@material-ui/icons/TableChart'
import { bindActionCreators, Dispatch } from '@reduxjs/toolkit'
import React from 'react'
import { connect, ConnectedProps } from 'react-redux'
import { SELECTION_GREEN } from '../../dpColors'
import { StoreState } from '../../index'
import { getSelectedView } from '../../MultiSearch/selectors'
import SelectedView, { SELECTED_VIEW_ENUM } from '../../models/SelectedView'
import {
  updateSelectedViewWithRefresh,
  updateSelectedDrilldownWithRefresh,
} from '../../compositeActions'
import {
  clearAllListViewData,
  setListViewDrilldown,
} from '../../ListView/actions'
import { isUndefined } from 'lodash'
import Tooltip from '@material-ui/core/Tooltip'
import SelectedDrilldown from '../../drilldown/models/SelectedDrilldown'

// props from parent
export interface OwnProps {
  classes: Record<string, any>
}
// props from redux state
interface StateProps {}
// props from redux actions
type DispatchProps = ConnectedProps<typeof connector>
type Props = StateProps & DispatchProps & OwnProps
interface State {}

class ViewModeSwitcher extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
  }

  enableView(view: SELECTED_VIEW_ENUM, event?: React.SyntheticEvent): void {
    if (event) {
      event.preventDefault()
    }

    const selectedView = new SelectedView(view)
    if (selectedView.currentView() !== SELECTED_VIEW_ENUM.LIST) {
      // switching away from list view, clear its state
      this.props.clearAllListViewData()
    }
    this.props.updateSelectedViewWithRefresh(selectedView)
    this.props.updateSelectedDrilldownWithRefresh(
      SelectedDrilldown.of(this.props.drilldown)
    )

    // if drilldown is at least table level, make sure data tab is selected in list view
    if (
      selectedView.currentView() === SELECTED_VIEW_ENUM.LIST &&
      this.props.drilldown?.table
    ) {
      this.props.setListViewDrilldown(
        SelectedDrilldown.of(this.props.drilldown),
        true
      )
    }
  }

  render(): JSX.Element {
    const { classes, selectedView } = this.props
    const curView = selectedView?.currentView()
    const isTreemapView =
      isUndefined(curView) || curView === SELECTED_VIEW_ENUM.TREEMAP
    const isListView = curView === SELECTED_VIEW_ENUM.LIST
    const isRowViewer = curView === SELECTED_VIEW_ENUM.ROW_VIEWER
    const btnSize = 'small'
    // style={{ marginRight: '12px' }}
    return (
      <div className={classes.row}>
        <Tooltip title="list view" placement="bottom">
          <Button
            variant="outlined"
            className={`${classes.btn} ${
              isListView ? classes.selectedBtn : classes.actionBtn
            }`}
            onClick={(event) => this.enableView(SELECTED_VIEW_ENUM.LIST, event)}
            aria-label="enable list view"
            component="span">
            <ListOutlinedIcon fontSize={btnSize}></ListOutlinedIcon>
          </Button>
        </Tooltip>
        <Tooltip title="treemap view" placement="bottom">
          <Button
            variant="outlined"
            className={`${classes.btn} ${
              isTreemapView ? classes.selectedBtn : classes.actionBtn
            }`}
            onClick={(event) =>
              this.enableView(SELECTED_VIEW_ENUM.TREEMAP, event)
            }
            aria-label="enable treemap view"
            component="span">
            <ViewQuiltOutlinedIcon fontSize={btnSize}></ViewQuiltOutlinedIcon>
          </Button>
        </Tooltip>
        {false && (
          <Button
            variant="outlined"
            className={`${
              isRowViewer ? classes.selectedBtn : classes.actionBtn
            }`}
            onClick={(event) =>
              this.enableView(SELECTED_VIEW_ENUM.ROW_VIEWER, event)
            }
            aria-label="enable rowview"
            component="span">
            <RowViewer />
          </Button>
        )}
      </div>
    )
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = (theme: Theme) =>
  createStyles({
    root: {},
    row: {
      flex: 0,
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      '& > *': {
        margin: theme.spacing(0.5),
      },
      marginRight: 24,
    },
    btn: {
      minWidth: 0,
      width: 38,
    },
    selectedBtn: {
      background: SELECTION_GREEN,
      color: 'rgba(255, 255, 255, .82)',
      '&:hover': {
        background: SELECTION_GREEN,
      },
    },
    actionBtn: {
      color: SELECTION_GREEN,
    },
  })

const mapStateToProps = (state: StoreState) => ({
  selectedView: getSelectedView(state),
  drilldown: state.drilldown,
})

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators(
    {
      updateSelectedViewWithRefresh,
      updateSelectedDrilldownWithRefresh,
      clearAllListViewData,
      setListViewDrilldown,
    },
    dispatch
  )

const connector = connect(mapStateToProps, mapDispatchToProps)
export default withStyles(styles)(connector(ViewModeSwitcher))
