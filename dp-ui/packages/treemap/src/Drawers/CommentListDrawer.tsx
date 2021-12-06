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
import { createStyles, Theme, withStyles } from '@material-ui/core'
import IconButton from '@material-ui/core/IconButton'
import ChevronLeftRoundedIcon from '@material-ui/icons/ChevronLeftRounded'
import Skeleton from '@material-ui/lab/Skeleton'
import { bindActionCreators, Dispatch } from '@reduxjs/toolkit'
import { groupBy, isEmpty, isUndefined, range, sortBy } from 'lodash'
import React, { Fragment } from 'react'
import { connect, ConnectedProps } from 'react-redux'
import { setSelectedDrilldown } from '../actions'
import { Comment } from '../comments/models/Comment'
import { CommentType } from '../comments/models/CommentType'
import CommentsService from '../comments/services/CommentsService'
import ColumnChip from '../components/ColumnChip'
import DatasetChip from '../components/DatasetChip'
import DisplayComment from '../components/DisplayComment'
import TableChip from '../components/TableChip'
import { StoreState } from '../index'
import CommonMetadata from '../models/CommonMetadata'
import SelectedDrilldown from '../drilldown/models/SelectedDrilldown'
import { updatePreviewDrawerDrilldown } from './actions'
import {
  clearCommentCounts,
  clearComments,
  fetchCommentCounts,
  setCommentLevel,
} from '../comments/actions'
import { getPreviewDrawerDrilldown } from './selectors'
import { ACTION_BTN_COLOR } from '../dpColors'
import { getSelectedDrilldown } from '../drilldown/selectors'

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

class CommentListDrawer extends React.PureComponent<Props, State> {
  service: CommentsService

  constructor(props: Props) {
    super(props)
    this.closeCommentsList = this.closeCommentsList.bind(this)
    this.service = new CommentsService()
  }

  closeCommentsList(
    drawerDrilldown: SelectedDrilldown,
    treemapDrilldown: SelectedDrilldown,
    event?: React.SyntheticEvent
  ): void {
    if (event) {
      event.preventDefault()
    }

    const updatedDrawerDrilldown = SelectedDrilldown.from(drawerDrilldown)
    updatedDrawerDrilldown.showComments = false
    this.props.updatePreviewDrawerDrilldown(updatedDrawerDrilldown)

    const updatedTreemapDrilldown = SelectedDrilldown.from(treemapDrilldown)
    updatedTreemapDrilldown.showComments = false
    this.props.setSelectedDrilldown(updatedTreemapDrilldown)

    // call to clear column specific values
    this.props.clearCommentCounts()
    this.props.fetchCommentCounts(updatedDrawerDrilldown)
  }

  openAddCommentDrawer(
    commentLevel: SelectedDrilldown,
    treemapDrilldown: SelectedDrilldown,
    event?: React.SyntheticEvent
  ): void {
    if (event) {
      event.preventDefault()
    }

    // const updatedDrawerDrilldown = SelectedDrilldown.from(drawerDrilldown)
    // updatedDrawerDrilldown.showAddComment = true
    // this.props.updatePreviewDrawerDrilldown(updatedDrawerDrilldown)
    this.props.setCommentLevel(commentLevel)
    const updatedTreemapDrilldown = SelectedDrilldown.from(treemapDrilldown)
    updatedTreemapDrilldown.showAddComment = true
    this.props.setSelectedDrilldown(updatedTreemapDrilldown)
  }

  generateHeader(): JSX.Element {
    const { classes, drawerDrilldown, selectedDrilldown } = this.props
    return (
      <div className={classes.header}>
        <span>
          <IconButton
            onClick={(event) =>
              this.closeCommentsList(drawerDrilldown, selectedDrilldown, event)
            }
            className={classes.actionBtn}
            aria-label="close comments list"
            component="span">
            <ChevronLeftRoundedIcon />
          </IconButton>
        </span>
        <span className={classes.header}>View All Comments & Questions</span>
      </div>
    )
  }

  generateEmptyPanel(): JSX.Element {
    return (
      <Fragment>
        <p>Error!</p>
      </Fragment>
    )
  }

  generateLoadingSkeleton(): JSX.Element {
    return (
      <div>
        {range(3).map((i) => {
          return (
            <div key={`skel-${i}`}>
              <Skeleton
                key={`${i}-1`}
                animation={false}
                width={50}
                height={24}></Skeleton>
              <Skeleton
                key={`${i}-2`}
                animation={false}
                width={50}
                height={24}></Skeleton>
              <Skeleton
                key={`${i}-3`}
                animation={false}
                width={75}
                height={24}></Skeleton>
              <Skeleton
                key={`${i}-4`}
                animation={false}
                width={75}
                height={24}></Skeleton>
            </div>
          )
        })}
        <hr />
      </div>
    )
  }

  generateEmptyComments(): JSX.Element {
    return (
      <Fragment>
        <p>No comments found!</p>
      </Fragment>
    )
  }

  generateAddComment(commentLevel?: SelectedDrilldown): JSX.Element {
    if (!commentLevel) {
      return <Fragment></Fragment>
    }
    const { classes, selectedDrilldown } = this.props
    return (
      <div className={classes.addCommentRow}>
        <a
          href="#"
          className={`${classes.addComment} ${classes.actionBtn}`}
          onClick={(e) =>
            this.openAddCommentDrawer(commentLevel, selectedDrilldown, e)
          }>
          + add comment
        </a>
      </div>
    )
  }

  generateDatasetComments(
    datasetName: string,
    comments: Readonly<Comment[]>,
    classes?: Record<string, any>
  ): JSX.Element {
    const sortedComments = this.service.sortCommentsDTG(comments || [])

    return (
      <div>
        <div>
          <DatasetChip></DatasetChip>
        </div>
        <div className={classes.commentSectionTitle}>{datasetName}</div>
        <div>
          {sortedComments.map((comment, i) => (
            <DisplayComment
              key={`display-ds-comment-${i}`}
              comment={comment}
              service={this.service}></DisplayComment>
          ))}
        </div>
        {this.generateAddComment(new SelectedDrilldown(datasetName))}
        <hr />
      </div>
    )
  }

  generateTableComments(
    drawerDrilldown: SelectedDrilldown,
    tableMeta: Readonly<CommonMetadata[]> = [],
    comments: Readonly<Comment[]>,
    classes?: Record<string, any>
  ): JSX.Element {
    const groups = groupBy(comments, 'table')
    const filteredMeta = isEmpty(drawerDrilldown.table)
      ? tableMeta
      : tableMeta.filter((el) => el.tableName === drawerDrilldown.table)
    const sortedMeta = sortBy(filteredMeta, 'tableName')

    if (isEmpty(sortedMeta)) {
      return <Fragment />
    }

    return (
      <Fragment>
        {sortedMeta.map((curMeta, k) => {
          const curKey = curMeta.tableName
          const curComments = groups[curKey] || []
          const sortedComments = this.service.sortCommentsDTG(curComments)
          const name = curKey
          return (
            <div key={`display-table-comment-group-${k}`}>
              <div>
                <TableChip></TableChip>
              </div>
              <div className={classes.commentSectionTitle}>{name}</div>
              <div>
                {sortedComments &&
                  sortedComments.map((comment, i) => (
                    <DisplayComment
                      key={`display-table-comment-${k}-${i}`}
                      comment={comment}
                      service={this.service}></DisplayComment>
                  ))}
              </div>
              {this.generateAddComment(
                new SelectedDrilldown(curMeta?.datasetName, curMeta?.tableName)
              )}
            </div>
          )
        })}
        <hr />
      </Fragment>
    )
  }

  generateColumnComments(
    drawerDrilldown: SelectedDrilldown,
    columnMeta: Readonly<CommonMetadata[]> = [],
    comments: Readonly<Comment[]>,
    classes?: Record<string, any>
  ): JSX.Element {
    const makeTitle = (table: string, column: string) => `${table} > ${column}`
    const groups = groupBy(comments, 'column')
    const filteredMeta = isEmpty(drawerDrilldown.column)
      ? columnMeta
      : columnMeta.filter(
          (el) =>
            el.datasetName === drawerDrilldown.dataset &&
            el.tableName === drawerDrilldown.table &&
            el.columnName === drawerDrilldown.column
        )
    const sortedMeta = sortBy(filteredMeta, (el) =>
      makeTitle(el.tableName, el.columnName)
    )

    if (isEmpty(sortedMeta)) {
      return <Fragment />
    }

    return (
      <Fragment>
        {sortedMeta.map((curMeta, k) => {
          const curKey = curMeta.columnName
          const curComments = groups[curKey] || []
          const sortedComments = this.service.sortCommentsDTG(curComments)
          const name = makeTitle(curMeta.tableName, curMeta.columnName)
          return (
            <div key={`display-column-comment-group-${k}`}>
              <div>
                <ColumnChip></ColumnChip>
              </div>
              <div className={classes.commentSectionTitle}>{name}</div>
              <div>
                {sortedComments &&
                  sortedComments.map((comment, i) => (
                    <DisplayComment
                      key={`display-column-comment-${k}-${i}`}
                      comment={comment}
                      service={this.service}></DisplayComment>
                  ))}
              </div>
              {this.generateAddComment(
                new SelectedDrilldown(
                  curMeta?.datasetName,
                  curMeta?.tableName,
                  curMeta?.columnName
                )
              )}
            </div>
          )
        })}
        <hr />
      </Fragment>
    )
  }

  render(): JSX.Element {
    const {
      isOpen,
      drawerDrilldown,
      comments,
      classes,
      tableMeta,
      columnMeta,
    } = this.props

    if (!isOpen || !drawerDrilldown || drawerDrilldown.isEmpty()) {
      return this.generateEmptyPanel()
    }
    if (
      isUndefined(comments) ||
      isUndefined(tableMeta) ||
      isUndefined(columnMeta)
    ) {
      this.generateLoadingSkeleton()
    }
    if (isEmpty(comments)) {
      this.generateEmptyComments()
    }

    const groups = groupBy(comments, 'annotationType')
    const datasetComments = groups[CommentType.DATASET.toString()]
    const tableComments = groups[CommentType.TABLE.toString()]
    const columnComments = groups[CommentType.COLUMN.toString()]
    return (
      <div>
        {this.generateHeader()}
        <div>
          {!drawerDrilldown.isMoreSpecificThanTableView() &&
            this.generateDatasetComments(
              drawerDrilldown.dataset,
              datasetComments,
              classes
            )}
        </div>
        <div>
          {!drawerDrilldown.isSpecificColumnView() &&
            this.generateTableComments(
              drawerDrilldown,
              tableMeta,
              tableComments,
              classes
            )}
        </div>
        <div>
          {this.generateColumnComments(
            drawerDrilldown,
            columnMeta,
            columnComments,
            classes
          )}
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
    commentSectionTitle: {
      fontWeight: 'bolder',
      marginBottom: '24px',
      marginTop: '8px',
    },
    addCommentRow: {
      marginBottom: '24px',
    },
    addComment: {
      marginLeft: '10px',
      fontSize: '.9em',
      borderBottom: 'dashed 1px',
      cursor: 'pointer',
      textDecoration: 'none',
    },
    actionBtn: {
      color: ACTION_BTN_COLOR,
    },
  })

const mapStateToProps = (state: StoreState) => ({
  selectedDrilldown: getSelectedDrilldown(state),
  isOpen: state.previewdrawer.isOpen,
  drawerDrilldown: getPreviewDrawerDrilldown(state),
  comments: state.comments.comments,
  tableMeta: state.previewdrawer.tableMeta,
  columnMeta: state.previewdrawer.columnMeta,
})

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators(
    {
      updatePreviewDrawerDrilldown,
      setSelectedDrilldown,
      clearComments,
      setCommentLevel,
      clearCommentCounts,
      fetchCommentCounts,
    },
    dispatch
  )

const connector = connect(mapStateToProps, mapDispatchToProps)
export default withStyles(styles)(connector(CommentListDrawer))
