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
import { isEmpty, isUndefined } from 'lodash'
import DatasetDelta from '../../models/delta/DatasetDelta'
import {
  isColumnEnum,
  isDatasetEnum,
  isFilteredEnum,
  isTableEnum,
} from '../../models/delta/DatasetDeltaEnum'
import DatasetVersion from '../../models/delta/DatasetVersion'

export default class DatasetDeltaService {
  /**
   * is the given delta a known current version
   * based on the information given in datasetVersion
   *
   * @param datasetVersion
   * @param delta
   * @returns
   */
  isCurrentVersion(
    datasetVersion: Readonly<DatasetVersion>,
    delta: Readonly<DatasetDelta>
  ): boolean {
    if (
      isUndefined(datasetVersion) ||
      isUndefined(delta) ||
      isUndefined(delta.deltaEnum)
    ) {
      return false
    }

    const deltaEnum = delta.deltaEnum
    if (isDatasetEnum(deltaEnum)) {
      return this.isDefinedAndEqual(datasetVersion.version, delta.targetVersion)
    } else if (isTableEnum(deltaEnum)) {
      const tableVersion = datasetVersion.tables[delta.table]
      return this.isDefinedAndEqual(tableVersion?.version, delta.targetVersion)
    } else if (isColumnEnum(deltaEnum)) {
      const tableVersion = datasetVersion.tables[delta.table]
      const columnVersion = tableVersion.columns[delta.column]
      return this.isDefinedAndEqual(columnVersion, delta.targetVersion)
    } else if (isFilteredEnum(deltaEnum)) {
      console.log('filtering ' + deltaEnum + ' delta enum...')
    } else {
      console.log('skipping ' + deltaEnum + ' delta enum...')
    }
  }

  isDefinedAndEqual(currentVersion: string, deltaVersion: string): boolean {
    if (
      isUndefined(currentVersion) ||
      isEmpty(currentVersion) ||
      isUndefined(deltaVersion) ||
      isEmpty(currentVersion)
    ) {
      return false
    }
    return currentVersion === deltaVersion
  }
}
