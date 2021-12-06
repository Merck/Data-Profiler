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
import { createSelector } from 'reselect'
import { orderBy, take, filter, isEmpty, pick } from 'lodash'

const TRUNCATE_NUM = 250
const SEARCHABLE_FIELDS = [
  'id',
  'type',
  'status',
  'datasetName',
  'creatingUser',
  'details',
]
const FAILED_JOB_STATUS = ['error', 'dead']

const getState = (state) => state
const getSearch = (state) => state.searchQuery

const getActualJobs = createSelector(getState, (j) => (j.jobs ? j.jobs : []))

const allJobs = createSelector(getActualJobs, (j) =>
  orderBy(j, 'createdAt', 'desc')
)

export const getAllJobs = createSelector(allJobs, (j) => take(j, TRUNCATE_NUM))

export const getSearchedJobs = createSelector(
  allJobs,
  getSearch,
  (j, search) => {
    if (!search || isEmpty(search)) {
      return take(j, TRUNCATE_NUM)
    }
    const found = filter(
      j,
      (el) =>
        JSON.stringify(Object.values(pick(el, SEARCHABLE_FIELDS)))
          .toLowerCase()
          .indexOf(search.toLowerCase().trim()) > -1
    )
    return take(found, TRUNCATE_NUM)
  }
)

export const getCurrentUsersJobs = createSelector(getAllJobs, (j) => {
  let username

  try {
    username = JSON.parse(window.localStorage.getItem('session')).username
  } catch {}

  return username ? filter(j, (info) => info.creatingUser === username) : []
})

export const getOnlyFailedJobs = createSelector(getAllJobs, (j) => {
  return filter(j, (info) => FAILED_JOB_STATUS.indexOf(info.status) !== -1)
})
