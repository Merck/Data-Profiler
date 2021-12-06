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
import { Button, Paper } from '@material-ui/core'
import { createStyles, withStyles } from '@material-ui/core/styles'
import { Edit, Help } from '@material-ui/icons'
import WarningIcon from '@material-ui/icons/WarningSharp'
import { bindActionCreators, Dispatch } from '@reduxjs/toolkit'
import { isEmpty, isString } from 'lodash'
import React from 'react'
import { connect, ConnectedProps } from 'react-redux'
import { StoreState } from '../../index'
import ReorderCommonMetadata from '../../models/ReorderCommonMetadata'
import { setDatasetDescription } from '../actions'
import {
  updateDatasetMetadata,
} from '../../Drawers/actions'

// props from parent
export interface OwnProps {
  datasetMetadata: Readonly<ReorderCommonMetadata>
  classes: Record<string, any>
  toggleEdit: Function
}
// props from redux state
interface StateProps {
  dataprofiler: any
}
// props from redux actions
type DispatchProps = ConnectedProps<typeof connector>
type Props = StateProps & DispatchProps & OwnProps
interface State {
  description: string
  processing: boolean
  errMessage: string
}

class AddDescriptionForm extends React.Component<Props, State> {
  static CHAR_LIMIT = 5000

  constructor(props: Props) {
    super(props)

    this.state = {
      description:
        props.datasetMetadata?.commonMetadata?.properties?.description || '',
      processing: false,
      errMessage: '',
    }

    this.onSubmitInternal = this.onSubmitInternal.bind(this)
  }

  updateDescription = (description: string) => {
    this.setState({ description })
  }

  handleChange = (event: any) => {
    this.updateDescription(event.target.value)
  }

  validate = (description: string) => {
    if (!description) {
      this.setState({ errMessage: 'Description is empty!' })
      return ''
    }

    description = description.trim()
    if (description.length > AddDescriptionForm.CHAR_LIMIT) {
      description = ''
      this.setState({
        errMessage: `Description must be less than ${AddDescriptionForm.CHAR_LIMIT} characters!`,
      })
    }
    return description
  }

  onSubmitInternal = (event?: React.SyntheticEvent<any, Event>) => {
    if (event) {
      event.preventDefault()
    }

    const datasetMetadata = this.props.datasetMetadata
    const dataset = datasetMetadata.commonMetadata.datasetName
    const desc = this.validate(this.state.description)
    if (!desc) {
      return
    }

    this.setState((prevState) => {
      return {
        ...prevState,
        processing: true,
      }
    })
    // TODO: fix this; typescript does not understand the magic props to dispatch
    //  remove typing for now to remove compile error
    const promise: any = this.props.setDatasetDescription(dataset, desc)
    promise
      .then(() => {
        this.setState(
          (prevState) => {
            return {
              ...prevState,
              processing: false,
            }
          },
          () => {
            const curMeta = { ...datasetMetadata.commonMetadata }
            curMeta.properties = { ...curMeta.properties, description: desc }
            this.props.updateDatasetMetadata(curMeta)
            this.props.toggleEdit()
          }
        )
      })
      .catch((err) => {
        console.log('Failed to set description', err)
      })
  }

  render(): JSX.Element {
    const { classes, datasetMetadata } = this.props
    const { dataprofiler } = this.props
    const isAdmin = dataprofiler?.isAdmin
    const prevDescription =
      datasetMetadata?.commonMetadata?.properties?.description
    return (
      <div className={classes.root}>
        <div className={classes.emptyStateIconPanel}>
          <Help className={classes.emptyStateIcon}></Help>
        </div>
        <div>
          <h3 style={{ textAlign: 'center' }}>Add Description</h3>
          <p className={classes.descriptionEmpty}>
            Administrators can add a description to give users more context
            about the dataset
          </p>
        </div>
        {isAdmin && (
          <div>
            <span>
              {isString(this.state.errMessage) &&
                !isEmpty(this.state.errMessage) && (
                  <Paper className="error-box" elevation={1}>
                    <div className="error-title">
                      <h5 className="title">
                        <WarningIcon className="warning-icon" />
                        Save Failed!
                      </h5>
                    </div>
                    <p className="error-content">{this.state.errMessage}</p>
                  </Paper>
                )}
            </span>
            <textarea
              id="description-textarea"
              className={classes.textarea}
              rows={12}
              cols={80}
              onChange={this.handleChange}
              value={this.state.description}></textarea>
            <div style={{ display: 'flex' }}>
              <span className={classes.characterCounter}>
                {this.state.description.length}/{AddDescriptionForm.CHAR_LIMIT}{' '}
                characters
              </span>
              <span>
                {prevDescription && (
                  <Button
                    variant="contained"
                    onClick={(e) => this.props.toggleEdit()}>
                    Cancel
                  </Button>
                )}
                &nbsp;
                <Button
                  onClick={(e) => this.onSubmitInternal(e)}
                  color="primary"
                  variant="contained"
                  disabled={this.state.processing}>
                  <Edit></Edit>
                  {this.state.processing
                    ? 'Saving...'
                    : prevDescription
                    ? 'Apply'
                    : 'Add'}
                </Button>
              </span>
            </div>
          </div>
        )}
      </div>
    )
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = createStyles((theme) => ({
  root: {
    // height: 220,
    width: '100%',
    display: 'flex',
    flexDirection: 'column',
    '& div': {
      margin: 'auto auto',
    },
  },
  descriptionEmpty: {},
  emptyStateIconPanel: {},
  emptyStateIcon: {
    color: '#b3b3b3',
    fontSize: 77,
  },
  characterCounter: {
    flex: 1,
    fontSize: '.8em',
    color: '#b3b3b3',
  },
  textarea: {
    resize: 'none',
    width: '100%',
    maxWidth: '633px',
  },
}))

const mapStateToProps = (state: StoreState) => ({})

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators(
    {
      setDatasetDescription,
      updateDatasetMetadata,
    },
    dispatch
  )

const connector = connect(mapStateToProps, mapDispatchToProps)
export default withStyles(styles)(DPContext(connector(AddDescriptionForm)))
