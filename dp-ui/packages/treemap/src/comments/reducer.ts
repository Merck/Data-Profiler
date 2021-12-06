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
import { CommentCounts } from './models/CommentCounts'
import { Comment } from './models/Comment'
import SelectedDrilldown from '../drilldown/models/SelectedDrilldown'

// define action types
export const SET_COLUMN_COUNTS = 'comments/set_column_counts'
export const CLEAR_COLUMN_COUNTS = 'comments/clear_column_counts'
export const SET_COMMENT_COUNTS = 'comments/set_comment_counts'
export const CLEAR_COMMENT_COUNTS = 'comments/clear_comment_counts'
export const SET_COMMENTS = 'comments/set_comments'
export const CLEAR_COMMENTS = 'comments/clear_comments'
export const SET_COMMENT_LEVEL = 'comments/set_comment_level'
export const CLEAR_COMMENT_LEVEL = 'comments/set_comment_level'
export const SET_COMMENTS_IN_FLIGHT = 'comments/set_comments_in_flight'

interface Command {
  readonly type: string
  readonly payload?: any
}

interface SetCommentCounts extends Command {
  readonly type: typeof SET_COMMENT_COUNTS
  readonly payload: CommentCounts
}

interface ClearCommentCounts extends Command {
  readonly type: typeof CLEAR_COMMENT_COUNTS
}

interface SetComments extends Command {
  readonly type: typeof SET_COMMENTS
  readonly payload: Array<Comment>
}

interface ClearComments extends Command {
  readonly type: typeof CLEAR_COMMENTS
}

interface SetCommentLevel extends Command {
  readonly type: typeof SET_COMMENT_LEVEL
}

interface ClearCommentLevel extends Command {
  readonly type: typeof CLEAR_COMMENT_LEVEL
}

interface SetCommentsInFlight extends Command {
  readonly type: typeof SET_COMMENTS_IN_FLIGHT
}

export type CommentActionTypes =
  | SetCommentCounts
  | ClearCommentCounts
  | SetComments
  | ClearComments
  | SetCommentLevel
  | ClearCommentLevel
  | SetCommentsInFlight

export interface CommentsState {
  commentCounts: Partial<CommentCounts>
  comments: Array<Comment>
  commentLevel: Record<string, string>
  isInFlight?: boolean
}

export const generateInitialState = (): CommentsState => {
  return {
    commentCounts: undefined,
    comments: [],
    commentLevel: new SelectedDrilldown().serialize(),
  }
}

const initialState = generateInitialState()

export default (
  state = initialState,
  action: CommentActionTypes
): CommentsState => {
  switch (action.type) {
    case SET_COMMENT_COUNTS:
      return {
        ...state,
        commentCounts: action.payload,
      }
    case CLEAR_COMMENT_COUNTS:
      return {
        ...state,
        commentCounts: undefined,
      }
    case SET_COMMENTS:
      return {
        ...state,
        comments: [...action.payload],
      }
    case CLEAR_COMMENTS:
      return {
        ...state,
        comments: undefined,
      }
    case SET_COMMENT_LEVEL:
      return {
        ...state,
        commentLevel: action.payload,
      }
    case CLEAR_COMMENT_LEVEL:
      return {
        ...state,
        commentLevel: new SelectedDrilldown().serialize(),
      }
    case SET_COMMENTS_IN_FLIGHT:
      return {
        ...state,
        isInFlight: action.payload,
      }
    default:
      return state
  }
}
