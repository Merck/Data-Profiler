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
import {
  Checkbox,
  createStyles,
  FormControlLabel,
  FormGroup,
  Paper,
  Theme,
  withStyles,
} from '@material-ui/core'
import Button from '@material-ui/core/Button'
import WarningIcon from '@material-ui/icons/WarningSharp'
import { bindActionCreators, Dispatch } from '@reduxjs/toolkit'
import { isEmpty, isString } from 'lodash'
import React from 'react'
import { connect, ConnectedProps } from 'react-redux'
import { setSelectedDrilldown } from '../actions'
import '../assets/AddCommentDrawer.css'
import {
  fetchCommentsAtAllLevels,
  clearCommentLevel,
  addCommentToDataset,
  addCommentToTable,
  addCommentToColumn,
} from '../comments/actions'
import { Comment } from '../comments/models/Comment'
import { StoreState } from '../index'
import CommonMetadata from '../models/CommonMetadata'
import SelectedDrilldown, {
  TREEMAP_VIEW_ENUM,
} from '../drilldown/models/SelectedDrilldown'
import MetadataService from '../services/MetadataService'
import { updatePreviewDrawerDrilldown } from './actions'
import { getCommentLevel, getPreviewDrawerDrilldown } from './selectors'
import { getSelectedDrilldown } from '../drilldown/selectors'

interface State {
  comment: string
  isDataTour: boolean
  processing: boolean
  errMessage: string
}

// props from parent
export interface OwnProps {
  classes: Record<string, any>
}
// props from redux state
interface StateProps {}
// props from redux actions
type DispatchProps = ConnectedProps<typeof connector>
type Props = StateProps & DispatchProps & OwnProps

class AddCommentDrawer extends React.Component<Props, State> {
  static CHAR_LIMIT = 512
  metadataService: MetadataService

  constructor(props: any) {
    super(props)

    const { comment, isDataTour } = props
    this.state = {
      comment: comment || '',
      isDataTour: isDataTour || false,
      processing: false,
      errMessage: '',
    }

    this.metadataService = new MetadataService()
    this.closeAddCommentsDrawer = this.closeAddCommentsDrawer.bind(this)
    this.handleDataTourChange = this.handleDataTourChange.bind(this)
    this.handleCommentChange = this.handleCommentChange.bind(this)
  }

  closeAddCommentsDrawer(
    drawerDrilldown: SelectedDrilldown,
    treemapDrilldown: SelectedDrilldown,
    event?: React.SyntheticEvent
  ): void {
    if (event) {
      event.preventDefault()
    }

    this.props.clearCommentLevel()
    this.props.fetchCommentsAtAllLevels(drawerDrilldown)
    const updatedTreemapDrilldown = SelectedDrilldown.from(treemapDrilldown)
    updatedTreemapDrilldown.showAddComment = false
    this.props.setSelectedDrilldown(updatedTreemapDrilldown)
  }

  handleCommentChange = (
    event: React.ChangeEvent<HTMLTextAreaElement>
  ): void => {
    if (event) {
      event.preventDefault()
    }
    const comment = event.target.value
    this.setState((prevState) => {
      return {
        ...prevState,
        comment,
      }
    })
  }

  handleDataTourChange = (event: React.ChangeEvent<HTMLInputElement>): void => {
    if (event) {
      event.preventDefault()
    }
    const isDataTour = event.target.checked
    this.setState((prevState) => {
      return {
        ...prevState,
        isDataTour,
      }
    })
  }

  validateComment = (comment: string) => {
    if (!comment) {
      this.setState({ errMessage: 'Comment is empty!' })
      return ''
    }

    // TODO: check for stuff; length, remove bad characters
    comment = comment.trim()
    if (comment.length > AddCommentDrawer.CHAR_LIMIT) {
      comment = ''
      this.setState({
        errMessage: `Comment must be less than ${AddCommentDrawer.CHAR_LIMIT} characters!`,
      })
    }
    return comment
  }

  onSubmitInternal = (
    commentLevel: SelectedDrilldown,
    event?: React.SyntheticEvent<any, Event>
  ) => {
    if (event) {
      event.preventDefault()
    }

    const { selectedDrilldown, drawerDrilldown } = this.props
    const comment = this.validateComment(this.state.comment)
    if (!comment) {
      return
    }
    const isDataTour = this.state.isDataTour || false

    let promise
    let metadata: CommonMetadata[]
    const { dataset, table, column } = commentLevel
    switch (commentLevel.currentView()) {
      case TREEMAP_VIEW_ENUM.SPECIFIC_COLUMN: {
        metadata = this.props.columnMetadata
        promise = this.addAnnotationForColumn(
          dataset,
          table,
          column,
          comment,
          isDataTour
        )
        break
      }
      case TREEMAP_VIEW_ENUM.COLUMN: {
        metadata = this.props.tableMetadata
        promise = this.addAnnotationForTable(
          dataset,
          table,
          comment,
          isDataTour
        )
        break
      }
      case TREEMAP_VIEW_ENUM.TABLE: {
        metadata = this.props.tableMetadata
        promise = this.addAnnotationForDataset(dataset, comment, isDataTour)
        break
      }
      default: {
        const err = 'unknown view state. skipping save step and moving on...'
        console.warn(err)
        promise = Promise.reject(new Error(err))
      }
    }

    promise.then((resp) =>
      this.closeAddCommentsDrawer(drawerDrilldown, selectedDrilldown)
    )
  }

  addAnnotation = (action: any): Promise<Comment> => {
    const p = new Promise<Comment>((resolve, reject) => {
      this.setState(
        {
          processing: true,
        },
        () => {
          action
            .then((resp) => {
              this.setState({
                processing: false,
                errMessage: '',
              })
              resolve(resp)
            })
            .catch((err) => {
              const msg = err.message || 'Unknown error occurred'
              this.setState({
                processing: false,
                errMessage: msg,
              })
              reject(err)
            })
        }
      )
    })
    return p
  }

  addAnnotationForDataset = (
    datasetName: string,
    comment: string,
    isDataTour: boolean
  ): Promise<Comment> => {
    const promise = this.props.addCommentToDataset(
      datasetName,
      comment,
      isDataTour
    )
    return this.addAnnotation(promise)
  }

  addAnnotationForTable = (
    datasetName: string,
    table: string,
    comment: string,
    isDataTour: boolean
  ): Promise<Comment> => {
    const promise = this.props.addCommentToTable(
      datasetName,
      table,
      comment,
      isDataTour
    )
    return this.addAnnotation(promise)
  }

  addAnnotationForColumn = (
    datasetName: string,
    table: string,
    column: string,
    comment: string,
    isDataTour: boolean
  ): Promise<Comment> => {
    const promise = this.props.addCommentToColumn(
      datasetName,
      table,
      column,
      comment,
      isDataTour
    )
    return this.addAnnotation(promise)
  }

  renderSaveButton = () => {
    const { commentLevel } = this.props
    return (
      <Button
        onClick={(e) => this.onSubmitInternal(commentLevel, e)}
        color="primary"
        variant="contained"
        disabled={this.state.processing}>
        {this.state.processing ? 'Saving...' : 'Save Changes'}
      </Button>
    )
  }

  render() {
    const { classes, selectedDrilldown, drawerDrilldown, commentLevel } =
      this.props
    return (
      <div>
        <div className={classes.header}>Add Comment</div>
        <div>
          {isString(this.state.errMessage) && !isEmpty(this.state.errMessage) && (
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
          <div>
            <span className="content-origin">
              {commentLevel?.dataset}&nbsp;{commentLevel?.table}&nbsp;
              {commentLevel?.column}
            </span>
          </div>
          <textarea
            id="comment-textarea"
            rows={4}
            cols={50}
            className="comment-textarea"
            onChange={this.handleCommentChange}
            value={this.state.comment}></textarea>
          <div className="comment-counter">
            {this.state.comment.length}/{AddCommentDrawer.CHAR_LIMIT} characters
          </div>
          <p></p>
          <FormGroup row>
            <FormControlLabel
              control={
                <Checkbox
                  checked={this.state.isDataTour}
                  onChange={this.handleDataTourChange}
                  name="isDataTour"
                  color="primary"
                />
              }
              label="Use In DataTour"
            />
          </FormGroup>
          <div>
            <span className="content-warning">
              Note: The Author, Date and Note will be visible to everyone who
              has access to this data.
            </span>
          </div>
        </div>
        <div className={classes.actionRow}>
          <Button
            onClick={(e) =>
              this.closeAddCommentsDrawer(drawerDrilldown, selectedDrilldown, e)
            }
            color="default">
            Cancel
          </Button>
          {this.renderSaveButton()}
        </div>
      </div>
    )
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = (theme: Theme) =>
  createStyles({
    root: {},
    header: {
      color: 'rgba(0,0,0,.82);',
      fontWeight: 500,
      fontSize: '1.1em',
      marginBottom: '12px',
    },
    actionRow: {
      display: 'flex',
      '& *': {
        margin: '5px 5px 5px 0px',
      },
    },
  })

const mapStateToProps = (state: StoreState) => ({
  selectedDrilldown: getSelectedDrilldown(state),
  isOpen: state.previewdrawer.isOpen,
  drawerDrilldown: getPreviewDrawerDrilldown(state),
  commentLevel: getCommentLevel(state),
  tableMetadata: state.previewdrawer.tableMeta,
  columnMetadata: state.previewdrawer.columnMeta,
})

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators(
    {
      addCommentToDataset,
      addCommentToTable,
      addCommentToColumn,
      updatePreviewDrawerDrilldown,
      setSelectedDrilldown,
      clearCommentLevel,
      fetchCommentsAtAllLevels,
    },
    dispatch
  )

const connector = connect(mapStateToProps, mapDispatchToProps)
export default withStyles(styles)(connector(AddCommentDrawer))
