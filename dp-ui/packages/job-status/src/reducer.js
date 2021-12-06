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
export const SET_JOBS = 'jobsStatus/SET_JOBS'
export const SET_SEARCH = 'jobsStatus/SET_SEARCH'
export const SET_USERNAME = 'jobsStatus/SET_USERNAME'

const initialState = {
  jobs: [],
  searchQuery: null,
  username: null,
}

export default (state = initialState, action) => {
  switch (action.type) {
    case SET_JOBS:
      return {
        ...state,
        jobs: action.jobs,
      }

    case SET_SEARCH:
      return {
        ...state,
        searchQuery: action.search,
      }

    case SET_USERNAME:
      return {
        ...state,
        username: action.username,
      }

    default:
      return state
  }
}
