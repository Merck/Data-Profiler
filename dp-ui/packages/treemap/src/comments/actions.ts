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
import { isEmpty } from 'lodash'
import SelectedDrilldown from '../drilldown/models/SelectedDrilldown'
import * as commentsProvider from './provider'
import {
  CLEAR_COMMENTS,
  CLEAR_COMMENT_COUNTS,
  CLEAR_COMMENT_LEVEL,
  SET_COMMENTS,
  SET_COMMENT_COUNTS,
  SET_COMMENT_LEVEL,
  SET_COMMENTS_IN_FLIGHT,
} from './reducer'
import { Comment } from './models/Comment'
import { CommentCounts } from './models/CommentCounts'

/**
 * add a comment to a dataset
 */
export const addCommentToDataset =
  (dataset: string, note: string, isDataTour: boolean) => (dispatch) => {
    return commentsProvider.addCommentToDataset(dataset, note, isDataTour)
  }

/**
 * add a comment to a table
 */
export const addCommentToTable =
  (dataset: string, table: string, note: string, isDataTour: boolean) =>
  (dispatch) => {
    return commentsProvider.addCommentToTable(dataset, table, note, isDataTour)
  }

/**
 * add a comment to a column
 */
export const addCommentToColumn =
  (
    dataset: string,
    table: string,
    column: string,
    note: string,
    isDataTour: boolean
  ) =>
  (dispatch) => {
    return commentsProvider.addCommentToColumn(
      dataset,
      table,
      column,
      note,
      isDataTour
    )
  }

/**
 * set the preview drawer comments
 */
export const setCommentLevel =
  (drilldown?: SelectedDrilldown) => (dispatch) => {
    dispatch({
      type: SET_COMMENT_LEVEL,
      payload: drilldown.serialize(),
    })
  }

/**
 * clear the preview drawer comments
 */
export const clearCommentLevel = () => (dispatch) => {
  dispatch({
    type: CLEAR_COMMENT_LEVEL,
  })
}

/**
 * clear the preview drawer comment counts values
 */
export const clearCommentCounts = () => (dispatch) => {
  dispatch({
    type: CLEAR_COMMENT_COUNTS,
  })
}

/**
 * set the preview drawer comment counts
 */
export const setCommentCounts =
  (commentCounts: Partial<CommentCounts>) => (dispatch) => {
    dispatch({
      type: SET_COMMENT_COUNTS,
      payload: commentCounts,
    })
  }

/**
 * fetch the preview drawer comment counts
 */
export const fetchCommentCounts =
  (selectedDrilldown?: SelectedDrilldown) => (dispatch) => {
    clearCommentCounts()(dispatch)
    commentsProvider
      .fetchCommentHierarchyCounts(selectedDrilldown)
      .then((values) => setCommentCounts(values)(dispatch))
      .catch((err) => clearCommentCounts()(dispatch))
  }

/**
 * clear the preview drawer comments
 */
export const clearComments = () => (dispatch) => {
  dispatch({
    type: CLEAR_COMMENTS,
  })
}

/**
 * set the preview drawer comments
 */
export const setComments = (comments: Array<Comment>) => (dispatch) => {
  dispatch({
    type: SET_COMMENTS,
    payload: comments,
  })
  dispatch({
    type: SET_COMMENTS_IN_FLIGHT,
    payload: false,
  })
}

/**
 * fetch the preview drawer comments
 */
export const fetchComments =
  (selectedDrilldown?: SelectedDrilldown) => (dispatch) => {
    clearComments()(dispatch)
    commentsProvider
      .fetchComments(selectedDrilldown)
      .then((values) => setComments(values)(dispatch))
      .catch((err) => clearComments()(dispatch))
  }

/**
 * fetch the preview drawer comments at every level
 */
export const fetchCommentsAtAllLevels =
  (selectedDrilldown?: SelectedDrilldown) => (dispatch) => {
    dispatch({
      type: SET_COMMENTS_IN_FLIGHT,
      payload: true,
    })
    clearComments()(dispatch)
    if (!selectedDrilldown || isEmpty(selectedDrilldown.dataset)) {
      return setComments([])(dispatch)
    }

    commentsProvider
      .fetchHierarchyComments(selectedDrilldown)
      .then((values) => setComments(values)(dispatch))
      .catch((err) => clearComments()(dispatch))
  }

export const commentInFlight =
  (isInFlight = true) =>
  (dispatch): void => {
    dispatch({
      type: SET_COMMENTS_IN_FLIGHT,
      payload: isInFlight,
    })
  }
