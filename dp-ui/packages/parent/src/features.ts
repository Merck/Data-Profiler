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
const fetchSessionInfo = () => {
  return JSON.parse(localStorage.getItem('session') || '{}')
}

const isProdLike = (session: Readonly<Record<string, any>>) => {
  if (!session) return false

  return isProd(session) || isPreview(session) || isBeta(session)
}

const isProd = (session: Readonly<Record<string, any>>) => {
  if (!session) return false

  const { clusterName } = session
  return clusterName?.indexOf('production') > 0
}

const isPreview = (session: Readonly<Record<string, any>>) => {
  if (!session) return false

  const { clusterName } = session
  return clusterName?.indexOf('preview') > 0
}

const isBeta = (session: Readonly<Record<string, any>>) => {
  if (!session) return false

  const { clusterName } = session
  return clusterName?.indexOf('beta') > 0
}

const shouldDisplayDeltaTab = (session: Readonly<Record<string, any>>) =>
  !isProd(session) && !isPreview(session)

const shouldDisplayPerformanceTab = (session: Readonly<Record<string, any>>) =>
  !isProd(session) && !isPreview(session)

const shouldDisplayPerformancePlaceholders = (
  session: Readonly<Record<string, any>>
) => !isProdLike(session)

const shouldDisplayQualityTab = (session: Readonly<Record<string, any>>) =>
  !isProd(session) && !isPreview(session)

const shouldDisplayCardActions = (session: Readonly<Record<string, any>>) =>
  !isProdLike(session)

const DP_SESSION_INFO = fetchSessionInfo()
const TITLE_CLICK_FEATURE_FLAG = true
const LISTVIEW_TABS_FEATURE_FLAG = false
const PERFORMANCE_PLACEHOLDERS_FEATURE_FLAG =
  shouldDisplayPerformancePlaceholders(DP_SESSION_INFO)
const DATASET_DELTA_TAB_FEATURE_FLAG = shouldDisplayDeltaTab(DP_SESSION_INFO)
const QUALITY_TAB_FEATURE_FLAG = shouldDisplayQualityTab(DP_SESSION_INFO)
const CARD_ACTIONS_FEATURE_FLAG = true
// switch to develop offline
// set to true to use offline data
const DEV_OFFLINE = false

export {
  DEV_OFFLINE,
  TITLE_CLICK_FEATURE_FLAG,
  LISTVIEW_TABS_FEATURE_FLAG,
  PERFORMANCE_PLACEHOLDERS_FEATURE_FLAG,
  DATASET_DELTA_TAB_FEATURE_FLAG,
  QUALITY_TAB_FEATURE_FLAG,
  CARD_ACTIONS_FEATURE_FLAG,
}
