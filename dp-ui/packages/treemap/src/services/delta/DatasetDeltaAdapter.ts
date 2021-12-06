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
import { isEmpty, isUndefined, orderBy } from 'lodash'
import CompleteDelta from '../../models/delta/CompleteDelta'
import DatasetDeltaService from './DatasetDeltaService'

export default class DatasetDeltaAdapter {
  convert(json: Readonly<string>): Partial<CompleteDelta> {
    let completeDelta = {} as Partial<CompleteDelta>
    if (isUndefined(json) || isEmpty(json)) {
      return completeDelta
    }

    try {
      completeDelta = JSON.parse(json)
    } catch (e) {
      try {
        completeDelta = JSON.parse(JSON.stringify(json))
      } catch (e) {
        console.log(e)
      }
    }

    const { lastKnownVersions } = completeDelta
    const service = new DatasetDeltaService()
    completeDelta.deltas = completeDelta?.deltas.map((delta) => {
      return {
        ...delta,
        isCurrentVersion: service.isCurrentVersion(lastKnownVersions, delta),
      }
    })
    completeDelta.deltas = orderBy(
      completeDelta?.deltas,
      'datasetUpdatedOnMillis',
      'desc'
    )
    return completeDelta
  }
}
