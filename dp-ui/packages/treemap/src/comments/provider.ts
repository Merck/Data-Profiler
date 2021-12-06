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
import { api } from '@dp-ui/lib'
import { encodeVariable } from '@dp-ui/lib/dist/helpers/strings'
import { isEmpty, isUndefined } from 'lodash'
import { Comment } from '../comments/models/Comment'
import SelectedDrilldown, {
  TREEMAP_VIEW_ENUM,
} from '../drilldown/models/SelectedDrilldown'
import { CommentCounts } from './models/CommentCounts'

export type COMMENT_TYPE = 'system' | 'tour' | ''

export const fetchCommentCounts = (
  drilldown?: SelectedDrilldown,
  commentType: COMMENT_TYPE = ''
): Promise<Partial<CommentCounts>> => {
  return new Promise((resolve) => {
    const emptyCounts = {
      count: 0,
    }
    if (isEmpty(drilldown) || isEmpty(drilldown.dataset)) {
      return resolve(emptyCounts)
    }

    let path
    let annotationType
    switch (drilldown.currentView()) {
      case TREEMAP_VIEW_ENUM.DATASET: {
        // /annotations/datasets/counts
        path = `datasets`
        break
      }
      case TREEMAP_VIEW_ENUM.SPECIFIC_COLUMN: {
        annotationType = 'column'
        path = `${annotationType}/${encodeVariable(
          drilldown.dataset
        )}/${encodeVariable(drilldown.table)}/${encodeVariable(
          drilldown.column
        )}`
        break
      }
      case TREEMAP_VIEW_ENUM.COLUMN: {
        annotationType = 'table'
        path = `${annotationType}/${encodeVariable(
          drilldown.dataset
        )}/${encodeVariable(drilldown.table)}`
        break
      }
      case TREEMAP_VIEW_ENUM.TABLE: {
        annotationType = 'dataset'
        path = `${annotationType}/${encodeVariable(drilldown.dataset)}`
        break
      }
      default: {
        console.warn('unknown view state')
        break
      }
    }
    if (isUndefined(path)) {
      return resolve(emptyCounts)
    }
    const typeParam = commentType != '' ? commentType + '/' : ''
    path = `${typeParam}${path}`
    const resource = `annotations/${path}/counts`
    return api()
      .get({ resource })
      .then((res) => {
        resolve(res.body as Partial<CommentCounts>)
      })
      .catch((err) => {
        console.log('Loading Failed')
        console.log(err)
        return resolve(emptyCounts)
      })
  })
}

/**
 * fetches comments counts starting at the dataset, table or column levels depending on the drilldown
 * once a level is selected, it aggregates all counts of comments under the given level
 * ie: dataset level, shows counts at dataset level plus its sub tables, and sub columns
 *
 * @param drilldown
 */
export const fetchCommentHierarchyCounts = (
  drilldown?: SelectedDrilldown,
  commentType: COMMENT_TYPE = ''
): Promise<Partial<CommentCounts>> => {
  return new Promise((resolve) => {
    const emptyCounts = {
      count: 0,
    }
    if (isEmpty(drilldown) || isEmpty(drilldown.dataset)) {
      return resolve(emptyCounts)
    }

    let path
    let annotationType
    switch (drilldown.currentView()) {
      case TREEMAP_VIEW_ENUM.SPECIFIC_COLUMN: {
        annotationType = 'column'
        break
      }
      case TREEMAP_VIEW_ENUM.COLUMN: {
        annotationType = 'table'
        path = `${annotationType}/${encodeVariable(
          drilldown.dataset
        )}/${encodeVariable(drilldown.table)}`
        break
      }
      case TREEMAP_VIEW_ENUM.TABLE: {
        annotationType = 'dataset'
        path = `${annotationType}/${encodeVariable(drilldown.dataset)}`
        break
      }
      default: {
        console.warn('unknown view state')
        break
      }
    }
    if (annotationType === 'column') {
      // this is a column level count, columns have no hierarchy
      //  so we just do a straight count
      return resolve(fetchCommentCounts(drilldown))
    }
    if (isUndefined(path)) {
      return resolve(emptyCounts)
    }
    const typeParam = commentType != '' ? commentType + '/' : ''
    path = `${typeParam}hierarchy/${path}`
    const resource = `annotations/${path}/counts`
    return api()
      .get({ resource })
      .then((res) => {
        resolve(res.body as Partial<CommentCounts>)
      })
      .catch((err) => {
        console.log('Loading Failed')
        console.log(err)
        return resolve(emptyCounts)
      })
  })
}

/**
 * fetches full comments at the dataset, table or column levels depending on the drilldown
 * @param drilldown
 */
export const fetchComments = (
  drilldown?: SelectedDrilldown,
  commentType: COMMENT_TYPE = ''
): Promise<Comment[]> => {
  return new Promise((resolve) => {
    if (isEmpty(drilldown) || isEmpty(drilldown.dataset)) {
      return resolve([])
    }

    let path = undefined
    switch (drilldown.currentView()) {
      case TREEMAP_VIEW_ENUM.SPECIFIC_COLUMN: {
        path = `dataset/${encodeVariable(
          drilldown.dataset
        )}/table/${encodeVariable(drilldown.table)}/column/${encodeVariable(
          drilldown.column
        )}`
        break
      }
      case TREEMAP_VIEW_ENUM.COLUMN: {
        path = `dataset/${encodeVariable(
          drilldown.dataset
        )}/table/${encodeVariable(drilldown.table)}`
        break
      }
      case TREEMAP_VIEW_ENUM.TABLE: {
        path = `dataset/${drilldown.dataset}`
        break
      }
      default: {
        console.warn('unknown view state')
        break
      }
    }
    if (isUndefined(path)) {
      return resolve([])
    }
    const typeParam = commentType != '' ? commentType + '/' : ''
    path = `${typeParam}${path}`
    const resource = `annotations/${path}`
    return api()
      .get({ resource })
      .then((res) => {
        resolve(res.body as Comment[])
      })
      .catch((err) => {
        console.log('Loading Failed')
        console.log(err)
        resolve([])
      })
  })
}

/**
 * fetches full comments at the dataset, table or column levels depending on the drilldown
 * will return the complete hierarchy of comments from the given level and lower
 * eg. query a dataset will show all comments at also the table and column levels
 * @param drilldown
 */
export const fetchHierarchyComments = (
  drilldown?: SelectedDrilldown,
  commentType: COMMENT_TYPE = '',
  limit?: number
): Promise<Array<Comment>> => {
  return new Promise((resolve) => {
    if (isEmpty(drilldown) || isEmpty(drilldown.dataset)) {
      return resolve([])
    }

    let path = undefined
    switch (drilldown.currentView()) {
      case TREEMAP_VIEW_ENUM.TABLE: {
        path = `hierarchy/dataset/${encodeVariable(drilldown.dataset)}`
        break
      }
      case TREEMAP_VIEW_ENUM.COLUMN: {
        path = `hierarchy/dataset/${encodeVariable(
          drilldown.dataset
        )}/table/${encodeVariable(drilldown.table)}`
        break
      }
      case TREEMAP_VIEW_ENUM.SPECIFIC_COLUMN: {
        path = `dataset/${encodeVariable(
          drilldown.dataset
        )}/table/${encodeVariable(drilldown.table)}/column/${encodeVariable(
          drilldown.column
        )}`
        break
      }
      default: {
        console.warn('unknown view state')
        break
      }
    }
    if (isUndefined(path)) {
      return resolve([])
    }
    const typeParam = commentType != '' ? commentType + '/' : ''
    const limitParam = !isUndefined(limit) ? `?max=${limit}` : ''
    path = `${typeParam}${path}${limitParam}`
    const resource = `annotations/${path}`
    return api()
      .get({ resource })
      .then((res) => {
        resolve(res.body as Array<Comment>)
      })
      .catch((err) => {
        console.log('Loading Failed')
        console.log(err)
        resolve([])
      })
  })
}

export const addCommentToDataset = (
  dataset: string,
  note: string,
  isDataTour: boolean
): Promise<Comment> => {
  const params = {
    dataset,
    note,
    annotationType: 'DATASET',
    isDataTour,
  }
  const postBody = {
    resource: 'annotations',
    postObject: params,
  }
  // const p = Promise.reject(new Error('fail'))
  return api()
    .post(postBody)
    .then((resp) => resp.body as Comment)
}

export const addCommentToTable = (
  dataset: string,
  table: string,
  note: string,
  isDataTour: boolean
): Promise<Comment> => {
  const params = {
    dataset,
    table,
    note,
    annotationType: 'TABLE',
    isDataTour,
  }
  const postBody = {
    resource: 'annotations',
    postObject: params,
  }
  return api()
    .post(postBody)
    .then((resp) => resp.body as Comment)
}

export const addCommentToColumn = (
  dataset: string,
  table: string,
  column: string,
  note: string,
  isDataTour: boolean
): Promise<Comment> => {
  const params = {
    dataset,
    table,
    column,
    note,
    annotationType: 'COLUMN',
    isDataTour,
  }
  const postBody = {
    resource: 'annotations',
    postObject: params,
  }
  return api()
    .post(postBody)
    .then((resp) => resp.body as Comment)
}

/**
 * delete annotation
 * @param {*} dataset
 */
export const deleteDatasetComment =
  (dataset: string, id: string) => (): Promise<any> => {
    // annotations/ds1/t1/c1/abc26590-2541-4ba6-938f-773182b6c637
    return api()
      .delete({ resource: `annotations/${encodeVariable(dataset)}/${id}` })
      .then((resp) => resp.body)
  }

/**
 * delete annotation
 * @param {*} dataset
 */
export const deleteTableComment = (
  dataset: string,
  table: string,
  id: string
): Promise<any> => {
  // annotations/ds1/t1/c1/abc26590-2541-4ba6-938f-773182b6c637
  return api()
    .delete({
      resource: `annotations/${encodeVariable(dataset)}/${encodeVariable(
        table
      )}/${id}`,
    })
    .then((resp) => resp.body)
}

/**
 * delete annotation
 * @param {*} dataset
 */
export const deleteColumnComment = (
  dataset: string,
  table: string,
  column: string,
  id: string
): Promise<any> => {
  return api()
    .delete({
      resource: `annotations/${encodeVariable(dataset)}/${encodeVariable(
        table
      )}/${encodeVariable(column)}/${id}`,
    })
    .then((resp) => resp.body)
}
