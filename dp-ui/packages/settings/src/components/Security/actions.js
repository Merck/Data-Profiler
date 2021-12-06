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
import { sortBy } from 'lodash'
import * as SECURITY from '../../reducer'
import { store } from '../../index'

export const getUsersForAttribute = (attribute) => (dispatch) => {
  const query = `{usersWithAttribute(value:"${attribute}"){username}}`
  api()
    .post({ resource: 'rules_of_use', postObject: { query } })
    .then((res) => {
      dispatch({
        type: SECURITY.SET_USERS_WITH_ATTRIBUTE,
        attribute,
        users: res.body.data.usersWithAttribute,
      })
    })
}

export const getTotalNumberUsers = () => (dispatch) => {
  const { requireLoginAttributeForAccess } = store.getState()
  if (requireLoginAttributeForAccess) {
    const query = `{usersWithAttribute(value:"system.login"){username}}`
    api()
      .post({ resource: 'rules_of_use', postObject: { query } })
      .then((res) => {
        dispatch({
          type: SECURITY.SET_NUM_TOTAL_USERS,
          numUsers: res.body.data.usersWithAttribute.length,
        })
      })
  } else {
    const query = `{numUsers}`
    api()
      .post({ resource: 'rules_of_use', postObject: { query } })
      .then((res) => {
        dispatch({
          type: SECURITY.SET_NUM_TOTAL_USERS,
          numUsers: res.body.data.numUsers,
        })
      })
  }
}

export const loadWhitelists = () => (dispatch) => {
  const query = `{attributesActive}`
  api()
    .post({ resource: 'rules_of_use', postObject: { query } })
    .then((res) => {
      dispatch({
        type: SECURITY.SET_ACTIVE_DATA_WHITELISTS,
        activeDataWhitelists: res.body.data.attributesActive.filter(
          (el) =>
            el.toLowerCase().startsWith('list.') &&
            el.indexOf('&') === -1 &&
            el.indexOf('|') === -1
        ),
      })
    })
}

const userSerialization = `username first_name last_name position`
const attributeSerialization = `${userSerialization} attributes {id is_active value }`

export const searchForUsers = (searchQuery) =>
  new Promise((resolve) => {
    const query = `{usersLike(username:"${searchQuery}"){${userSerialization}}}`
    api()
      .post({ resource: 'rules_of_use', postObject: { query } })
      .then((res) => {
        resolve(sortBy(res.body.data.usersLike, 'username'))
      })
  })

export const searchUsersWithAttribute = (attribute) =>
  new Promise((resolve) => {
    const query = `{usersWithAttribute(value:"${attribute}"){${userSerialization}}}`
    api()
      .post({ resource: 'rules_of_use', postObject: { query } })
      .then((res) => {
        resolve(res.body.data.usersWithAttribute)
      })
  })

export const createUpdateUser = (username, attribute) =>
  new Promise((resolve) => {
    const query = `mutation{createUpdateUser(username:"${username}",attributes:["${attribute}"]){${attributeSerialization}}}`
    api()
      .post({ resource: 'rules_of_use', postObject: { query } })
      .then((res) => {
        resolve(res.body.data.createUpdateUser)
      })
  })

export const removeAttributesFromUser = (username, attribute) =>
  new Promise((resolve) => {
    const query = `mutation{removeAttributesFromUser(username:"${username}",attributes:["${attribute}"]){${attributeSerialization}}}`
    api()
      .post({ resource: 'rules_of_use', postObject: { query } })
      .then((res) => {
        resolve(res.body.data.removeAttributesFromUser)
      })
  })

const deleteApiToken = (token) => api().delete({ resource: `apiKeys/${token}` })
const createApiToken = (username) =>
  api().post({ resource: `apiKeys/${username}` })

export const getUser = (username) =>
  new Promise((resolve) => {
    const query = `{user(username:"${username}"){${attributeSerialization}}}`
    api()
      .post({ resource: 'rules_of_use', postObject: { query } })
      .then((res) => {
        resolve(res.body.data.user)
      })
  })

export const getApiKeys = () => (dispatch) => {
  api()
    .get({ resource: 'apiKeys' })
    .then((res) => {
      dispatch({
        type: SECURITY.SET_API_KEYS,
        apiKeys: Array.isArray(res.body) ? res.body : [],
      })
    })
}

export const regenerateApiKeyForUser =
  (deleteToken, username) => async (dispatch) => {
    await deleteApiToken(deleteToken)
    await createApiToken(username)
    dispatch(getApiKeys())
  }

export const createNewApiKeyForUser = (username) => async (dispatch) => {
  await createApiToken(username)
  dispatch(getApiKeys())
}

export const deleteApiKeyFromUser = (token) => async (dispatch) => {
  await deleteApiToken(token)
  dispatch(getApiKeys())
}
