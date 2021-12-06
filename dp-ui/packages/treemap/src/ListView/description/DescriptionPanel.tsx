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
import { createStyles, withStyles } from '@material-ui/core/styles'
import { bindActionCreators, Dispatch } from '@reduxjs/toolkit'
import { isEmpty } from 'lodash'
import React from 'react'
import { connect, ConnectedProps } from 'react-redux'
import { StoreState } from '../../index'
import ReorderCommonMetadata from '../../models/ReorderCommonMetadata'
import AddDescriptionForm from './AddDescriptionForm'
import { IconButton } from '@material-ui/core'
import { Edit } from '@material-ui/icons'

// props from parent
export interface OwnProps {
  classes: Record<string, any>
  datasetMetadata: Readonly<ReorderCommonMetadata>
}
// props from redux state
interface StateProps {
  dataprofiler: any
}
// props from redux actions
type DispatchProps = ConnectedProps<typeof connector>
type Props = StateProps & DispatchProps & OwnProps
interface State {
  showForm: boolean
}

class DescriptionPanel extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)

    this.state = {
      showForm: false,
    }

    this.toggleEdit = this.toggleEdit.bind(this)
  }

  toggleEdit = (event?: React.SyntheticEvent<any, Event>) => {
    if (event) {
      event.preventDefault()
    }

    this.setState({
      showForm: !this.state.showForm,
    })
  }

  render(): JSX.Element {
    const { showForm } = this.state
    const { classes, datasetMetadata, dataprofiler } = this.props
    const desc = datasetMetadata?.commonMetadata?.properties?.description
    const isAdmin = dataprofiler?.isAdmin
    return (
      <div className={classes.root}>
        {!isEmpty(desc) && !showForm && (
          <>
            <div style={{ display: 'flex', alignItems: 'center' }}>
              <h2 style={{ margin: 0 }}>Description</h2>
              {isAdmin && (
                <IconButton onClick={(e) => this.toggleEdit(e)}>
                  <Edit></Edit>
                </IconButton>
              )}
            </div>
            <div className={classes.description}>
              <div>{desc}</div>
            </div>
          </>
        )}
        {isEmpty(desc) || showForm ? (
          <AddDescriptionForm
            datasetMetadata={datasetMetadata}
            toggleEdit={this.toggleEdit}
          />
        ) : null}
      </div>
    )
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = createStyles((theme) => ({
  root: {
    background: '#eaebeb',
    borderRadius: '10px',
    padding: '18px 16px',
    width: '100%',
    display: 'flex',
    flexDirection: 'column',
    flex: 1,
  },
  description: {
    position: 'relative',
    flex: 1,
    '& > div': {
      position: 'absolute',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      overflowY: 'auto',
      paddingRight: '16px',
    },
    '& *::-webkit-scrollbar-track': {
      backgroundColor: 'transparent',
    },
  },
}))

const mapStateToProps = (state: StoreState) => ({})

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators({}, dispatch)

const connector = connect(mapStateToProps, mapDispatchToProps)
export default withStyles(styles)(DPContext(connector(DescriptionPanel)))
