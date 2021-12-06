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
/**
 * Network got you down? Try new devoffline to develop offline
 *
 * sometimes you just want to fetch data locally while developing
 * eg, not have to wait 1+min(s) to fetch data and render updates
 *
 * NOTE: DO NOT CHECK THIS DATA INTO THE REPO
 * check in only a empty stub
 */

// http://localhost:9000/v1/tables/DocsTokenCSVs
export const offlineTablesEndpoint = () => {
  return {}
}

// http://localhost:9000/v1/columns/DocsTokenCSVs/Draft_Design_for_E0244-U1807_177678_final
export const offlineColumnsEndpoint = () => {
  return {}
}

// http://localhost:9000/samples/DocsTokenCSVs/Draft_Design_for_E0244-U1807_177678_final
export const offlineSamplesEndpoint = () => {
  return {}
}

export const devOfflineMapping = {
  'samples/DocsTokenCSVs/Draft_Design_for_E0244-U1807_177678_final':
    offlineSamplesEndpoint,
  'v1/columns/DocsTokenCSVs/Draft_Design_for_E0244-U1807_177678_final':
    offlineColumnsEndpoint,
  'v1/tables/DocsTokenCSVs': offlineTablesEndpoint,
}
export const devOffline = (uri: string): any => {
  console.log(
    'WARNING!!! fetching endpoint data locally, this is for development only!!!'
  )
  return {
    body: devOfflineMapping[uri] ? devOfflineMapping[uri]() : () => ({}),
  }
}
