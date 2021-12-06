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
import { createStyles, Theme, withStyles } from '@material-ui/core/styles'
import { bindActionCreators, Dispatch } from '@reduxjs/toolkit'
import React from 'react'
import { connect, ConnectedProps } from 'react-redux'
import { updateSelectedViewWithRefresh } from '../compositeActions'
import { StoreState } from '../index'
import { setListViewDrilldown } from '../ListView/actions'
import SelectedDrilldown from '../drilldown/models/SelectedDrilldown'
import SelectedView, { SELECTED_VIEW_ENUM } from '../models/SelectedView'
import { genCommonDrawerStyles } from './drawerStyles'

// props from parent
export interface OwnProps {
  classes: Record<string, any>
  drawerDrilldown: Readonly<SelectedDrilldown>
}
// props from redux state
interface StateProps {}
// props from redux actions
type DispatchProps = ConnectedProps<typeof connector>
type Props = StateProps & DispatchProps & OwnProps
interface State {}

class JumpToListButton extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.handleJumpToListView = this.handleJumpToListView.bind(this)
  }

  handleJumpToListView(
    listViewDrilldown?: SelectedDrilldown,
    event?: React.SyntheticEvent
  ): void {
    if (event) {
      event.preventDefault()
    }

    const selectedView = new SelectedView(SELECTED_VIEW_ENUM?.LIST)
    this.props.updateSelectedViewWithRefresh(selectedView)
    this.props.setListViewDrilldown(listViewDrilldown, true)
  }

  destinationName(drilldown?: SelectedDrilldown): string {
    if (!drilldown) {
      return 'n/a'
    }

    if (drilldown.column) {
      return 'column'
    } else if (drilldown.table) {
      return 'table'
    } else {
      return 'dataset'
    }
  }

  render(): JSX.Element {
    const { classes, drawerDrilldown } = this.props
    const listViewDrilldown = { ...drawerDrilldown }
    const destination = this.destinationName(listViewDrilldown)
    return (
      <div className={classes.root}>
        go to {destination} in{' '}
        <a
          href="#"
          className={`${classes.jumpToView} ${classes.actionBtn}`}
          onClick={(e) => this.handleJumpToListView(listViewDrilldown, e)}>
          List View
        </a>
      </div>
    )
  }
}

const commonDrawerStyles = genCommonDrawerStyles()
// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = (theme: Theme) =>
  createStyles({
    root: {},
    jumpToView: {
      fontSize: '.9em',
      marginLeft: '10px',
      borderBottom: 'dashed 1px',
      cursor: 'pointer',
      textDecoration: 'none',
    },
    ...commonDrawerStyles,
  })

const mapStateToProps = (state: StoreState) => ({})

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators(
    {
      updateSelectedViewWithRefresh,
      setListViewDrilldown,
    },
    dispatch
  )

const connector = connect(mapStateToProps, mapDispatchToProps)
export default withStyles(styles)(connector(JumpToListButton))
