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
import { isEmpty } from 'lodash'
import { setOptionalParamIfExists } from '../helpers/ParamsHelper'
import SelectedDrilldown from '../drilldown/models/SelectedDrilldown'
import MetadataResult from './models/MetadataResult'
import SearchResult from './models/SearchResult'

const DEFAULT_BEGINS_WITH = false
const NAMESEARCH_LIMIT = 1000
const MULTISEARCH_LIMIT = 8000
const MULTISEARCH_TIMEOUT_MILLIS = 330000

const multiSearch = (
  tokens: Array<string>,
  selectedDrilldown?: SelectedDrilldown
): Promise<Array<SearchResult>> => {
  if (isEmpty(tokens) || !selectedDrilldown) {
    return Promise.resolve([])
  }

  const postRequest = {
    resource: `multi-search`,
    timeout: MULTISEARCH_TIMEOUT_MILLIS,
    postObject: {
      term: tokens,
      limit: MULTISEARCH_LIMIT,
    },
  }
  setOptionalParamIfExists(
    postRequest.postObject,
    'dataset',
    selectedDrilldown.dataset
  )
  setOptionalParamIfExists(
    postRequest.postObject,
    'table',
    selectedDrilldown.table
  )
  return api()
    .post(postRequest)
    .then((res) => res.body as Array<SearchResult>)
    .catch((err) => {
      console.error('multi-search', err)
    })
}

const searchColumnNames = (
  filterPhrases: Array<string>,
  selectedDrilldown?: SelectedDrilldown
): Promise<Array<Partial<MetadataResult>>> => {
  if (isEmpty(filterPhrases)) {
    return Promise.resolve([])
  }

  return new Promise((resolve) => {
    const terms = filterPhrases.map((el) => el.toLowerCase().trim())
    const params = {
      term: [...terms],
      begins_with: DEFAULT_BEGINS_WITH,
      limit: NAMESEARCH_LIMIT,
    }
    setOptionalParamIfExists(params, 'dataset', selectedDrilldown.dataset)
    setOptionalParamIfExists(params, 'table', selectedDrilldown.table)
    setOptionalParamIfExists(params, 'column', selectedDrilldown.column)
    return api()
      .post({ resource: 'columns/search', postObject: params })
      .then((res) => resolve(res.body as Array<MetadataResult>))
      .catch((err) => {
        console.error('columns/search', err)
        resolve([{ error: 'Failed to fetch columns' }])
      })
  })
}

const searchTableNames = (
  filterPhrases: Array<string>,
  selectedDrilldown?: SelectedDrilldown
): Promise<Array<Partial<MetadataResult>>> => {
  if (isEmpty(filterPhrases)) {
    return Promise.resolve([])
  }

  return new Promise((resolve) => {
    const terms = filterPhrases.map((el) => el.toLowerCase().trim())
    const params = {
      term: [...terms],
      begins_with: DEFAULT_BEGINS_WITH,
      limit: NAMESEARCH_LIMIT,
    }
    setOptionalParamIfExists(params, 'dataset', selectedDrilldown.dataset)
    setOptionalParamIfExists(params, 'table', selectedDrilldown.table)
    return api()
      .post({ resource: 'tables/search', postObject: params })
      .then((res) => resolve(res.body as Array<MetadataResult>))
      .catch((err) => {
        console.error('tables/search', err)
        resolve([{ error: 'Failed to fetch tables' }])
      })
  })
}

const searchDatasetNames = (
  filterPhrases: Array<string>,
  selectedDrilldown?: SelectedDrilldown
): Promise<Array<Partial<MetadataResult>>> => {
  if (isEmpty(filterPhrases)) {
    return Promise.resolve([])
  }

  return new Promise((resolve) => {
    const terms = filterPhrases.map((el) => el.toLowerCase().trim())
    const params = {
      term: [...terms],
      begins_with: DEFAULT_BEGINS_WITH,
      limit: NAMESEARCH_LIMIT,
    }
    setOptionalParamIfExists(params, 'dataset', selectedDrilldown.dataset)
    return api()
      .post({ resource: 'datasets/search', postObject: params })
      .then((res) => resolve(res.body as Array<MetadataResult>))
      .catch((err) => {
        console.error('datasets/search', err)
        resolve([{ error: 'Failed to fetch datasets' }])
      })
  })
}

export { multiSearch, searchColumnNames, searchTableNames, searchDatasetNames }
