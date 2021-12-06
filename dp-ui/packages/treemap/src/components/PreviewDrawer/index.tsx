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
import ClickAwayListener from '@material-ui/core/ClickAwayListener'
import IconButton from '@material-ui/core/IconButton'
import Paper from '@material-ui/core/Paper'
import Slide from '@material-ui/core/Slide'
import { createStyles, Theme, withStyles } from '@material-ui/core/styles'
import ChevronLeftRoundedIcon from '@material-ui/icons/ChevronLeftRounded'
import ChevronRightRoundedIcon from '@material-ui/icons/ChevronRightRounded'
import { bindActionCreators, Dispatch } from '@reduxjs/toolkit'
import React, { Fragment } from 'react'
import { connect, ConnectedProps } from 'react-redux'
import { ACTION_BTN_COLOR } from '../../dpColors'
import { closePreviewDrawer, openPreviewDrawer } from '../../Drawers/actions'
import { getSelectedDrilldown } from '../../drilldown/selectors'
import { StoreState } from '../../index'

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

class PreviewDrawer extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.handleEscKey = this.handleEscKey.bind(this)
    this.handleClickAway = this.handleClickAway.bind(this)
  }

  componentDidMount(): void {
    document.addEventListener('keydown', this.handleEscKey, false)
  }

  componentWillUnmount(): void {
    document.removeEventListener('keydown', this.handleEscKey, false)
  }

  handleEscKey(event?: KeyboardEvent): void {
    if (event.keyCode === 27) {
      this.closeDrawer()
    }
  }

  handleClickAway(event?: React.MouseEvent<Document, MouseEvent>): void {
    if (event) {
      event.preventDefault()
    }

    this.closeDrawer()
  }

  handleOnClose(event?: React.SyntheticEvent): void {
    if (event) {
      event.preventDefault()
    }

    this.closeDrawer()
  }

  closeDrawer(): void {
    this.props.closePreviewDrawer()
  }

  openDrawer(): void {
    this.props.openPreviewDrawer()
  }

  toggleDrawer(isOpen: boolean, event: React.SyntheticEvent): void {
    if (event) {
      event.preventDefault()
    }

    isOpen ? this.closeDrawer() : this.openDrawer()
  }

  generatePanel(): JSX.Element {
    const { classes, isOpen } = this.props
    return (
      <Fragment>
        {isOpen && (
          <Slide direction="left" in={isOpen}>
            <Paper elevation={2} style={{ borderRadius: '24px 0px 0px 24px' }}>
              <div className={classes.previewContainer}>
                <ClickAwayListener
                  disableReactTree={true}
                  onClickAway={this.handleClickAway}>
                  <div className={classes.panel}>{this.props.children}</div>
                </ClickAwayListener>
              </div>
            </Paper>
          </Slide>
        )}
      </Fragment>
    )
  }

  generateOpenCloseTab(): JSX.Element {
    const { classes, isOpen } = this.props
    return (
      <div className={classes.toggleDrawer}>
        <IconButton
          className={classes.actionBtn}
          onClick={(event) => this.toggleDrawer(isOpen, event)}
          aria-label="toggle metadata drawer"
          component="span">
          {isOpen && <ChevronRightRoundedIcon />}
          {!isOpen && <ChevronLeftRoundedIcon />}
        </IconButton>
      </div>
    )
  }

  render(): JSX.Element {
    const { isOpen, selectedDrilldown } = this.props
    const shouldShowTab = selectedDrilldown
    return (
      <Fragment>
        {shouldShowTab && this.generateOpenCloseTab()}
        {isOpen && this.generatePanel()}
      </Fragment>
    )
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
// width: 'calc(100vw - 68px)',
// marginLeft: 'auto',
// background: 'rgb(240,242,243)',
const styles = (theme: Theme) =>
  createStyles({
    root: {},
    toggleDrawer: {
      background: '#fff',
      width: '32px',
      height: '64px',
      flex: '0 0 auto',
      margin: 'auto auto',
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      borderRadius: '32px 0px 0px 32px',
      borderRight: '1px solid #c0c0c0',
    },
    previewContainer: {
      width: '35vw',
      minWidth: '450px',
      height: '100%',
      background: '#fff',
      overflowY: 'auto',
      flex: '1 1 auto',
      borderRadius: '24px 0px 0px 24px',
    },
    panel: {
      display: 'flex',
      padding: '8px 32px 32px 32px',
      flexFlow: 'column',
      flex: '1 1 auto',
      height: '100%',
      boxSizing: 'border-box',
    },
    actionBtn: {
      color: ACTION_BTN_COLOR,
    },
  })

const mapStateToProps = (state: StoreState) => ({
  isOpen: state.previewdrawer.isOpen,
  selectedDrilldown: getSelectedDrilldown(state),
})

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators(
    {
      openPreviewDrawer,
      closePreviewDrawer,
    },
    dispatch
  )

const connector = connect(mapStateToProps, mapDispatchToProps)
export default withStyles(styles)(connector(PreviewDrawer))
