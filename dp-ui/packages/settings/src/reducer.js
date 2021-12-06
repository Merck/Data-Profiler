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
import { sortBy } from 'lodash'
export const SET_ACTIVE_DATA_WHITELISTS = 'security/SET_ACTIVE_DATA_WHITELISTS'
export const SET_API_KEYS = 'security/SET_API_KEYS'
export const SET_NUM_TOTAL_USERS = 'security/SET_NUM_TOTAL_USERS'
export const SET_USERS_WITH_ATTRIBUTE = 'security/SET_USERS_WITH_ATTRIBUTE'

const initialState = {
  requireLoginAttributeForAccess: false,
  numTotalUsers: 0,
  attributes: {},
  activeDataWhitelists: [],
  activeSystemCapabilities: [
    // any changes in here, please remember to drop them in python_client/dataprofiler/api.py#create_skeleton_rou_user
    'system.admin',
    'system.download',
    'system.login',
    'system.make',
    'system.ui_add_tab',
    'system.ui_discover_tab',
    'system.ui_understand_tab',
    'system.ui_use_tab',
  ],
  apiKeys: [],
}

export default (state = initialState, action) => {
  switch (action.type) {
    case SET_NUM_TOTAL_USERS:
      return {
        ...state,
        numTotalUsers: action.numUsers,
      }

    case SET_ACTIVE_DATA_WHITELISTS:
      return {
        ...state,
        activeDataWhitelists: action.activeDataWhitelists,
      }

    case SET_USERS_WITH_ATTRIBUTE:
      return {
        ...state,
        attributes: {
          ...state.attributes,
          [action.attribute]: action.users,
        },
      }

    case SET_API_KEYS:
      return {
        ...state,
        apiKeys: Array.isArray(action.apiKeys)
          ? sortBy(action.apiKeys, ['username', 'token'])
          : [],
      }

    default:
      return state
  }
}
