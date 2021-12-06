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
import * as APP from '../reducers/app'
import * as USER_PREFERENCES from '../reducers/userPreferences'
import { SET_MAKE_PLUGINS_AVAILABLE } from '../reducers/makeDrawer'

export const changePetFilterThreshold = (petFilterThreshold) => (dispatch) => {
  dispatch({
    type: APP.CHANGE_PET_THRESHOLD,
    petFilterThreshold,
  })
}

export const finishAppInitialization = () => (dispatch) => {
  dispatch({
    type: APP.FINISH_INITIALIZATION,
  })
}

export const finishDeferredInitialization = (key) => (dispatch) => {
  dispatch({
    type: APP.FINISH_DEFERRED_INITIALIZATION,
    key,
  })
}

export const setTransformsAvailable = (transformsAvailable) => (dispatch) =>
  dispatch({
    type: APP.SET_TRANSFORMS_AVAILABLE,
    transformsAvailable,
  })

export const setMakePluginsAvailable = (makePluginsAvailable) => (dispatch) =>
  dispatch({
    type: SET_MAKE_PLUGINS_AVAILABLE,
    makePluginsAvailable,
  })

export const showErrorMessage = (message) => {
  const defaultMsg =
    'Please refresh the page and try again.'
  return (dispatch) => {
    dispatch({
      type: APP.SHOW_APPLICATION_MODAL,
      message: `${message} ${defaultMsg}`,
      title: 'Application Error',
    })
  }
}

export const showDialogMessage = (input) => (dispatch) => {
  if (typeof input === 'object') {
    dispatch({
      type: APP.SHOW_APPLICATION_MODAL,
      message: input.message,
      title: input.title,
    })
  } else if (typeof input === 'string') {
    dispatch({
      type: APP.SHOW_APPLICATION_MODAL,
      message: input,
      title: 'Application Notification',
    })
  } else {
    throw new Error('Bad Input to Show Dialog Message')
  }
}

export const closeApplicationModal = () => (dispatch) => {
  dispatch({
    type: APP.CLOSE_APPLICATION_MODAL,
  })
}

export const showApplicationSnackbar = (snackbarMessage) => (dispatch) => {
  dispatch({
    type: APP.SHOW_APPLICATION_SNACKBAR,
    snackbarMessage,
  })
}

export const closeApplicationSnackbar = () => (dispatch) => {
  dispatch({
    type: APP.CLOSE_APPLICATION_SNACKBAR,
  })
}

export const changeUserPreference = (key, value) => (dispatch) => {
  dispatch({
    type: USER_PREFERENCES.CHANGE_PREFERENCE,
    key,
    value,
  })
}

export const toggleElementFormattingScheme = (format) => (dispatch) => {
  dispatch({
    type: USER_PREFERENCES.CHANGE_ELEMENT_FORMATTING_SCHEME,
    format,
  })
}
