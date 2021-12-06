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
import { isUndefined, upperFirst } from 'lodash'

export enum DatasetDeltaEnum {
  DATASET_RENAMED = 'DATASET_RENAMED',
  TABLE_ADDED = 'TABLE_ADDED',
  TABLE_REMOVED = 'TABLE_REMOVED',
  COLUMN_ADDED = 'COLUMN_ADDED',
  COLUMN_REMOVED = 'COLUMN_REMOVED',
  DATASET_VALUES_INCREASED = 'DATASET_VALUES_INCREASED',
  DATASET_VALUES_DECREASED = 'DATASET_VALUES_DECREASED',
  TABLE_VALUES_INCREASED = 'TABLE_VALUES_INCREASED',
  TABLE_VALUES_DECREASED = 'TABLE_VALUES_DECREASED',
  COLUMN_VALUES_INCREASED = 'COLUMN_VALUES_INCREASED',
  COLUMN_VALUES_DECREASED = 'COLUMN_VALUES_DECREASED',
  NO_OP = 'NO_OP',
}

const testForType = (
  deltaEnum: DatasetDeltaEnum,
  startsWith: string
): boolean => {
  if (!deltaEnum) {
    return false
  }

  const deltaType = DatasetDeltaEnum[deltaEnum]
  return !isUndefined(deltaType) && deltaType.startsWith(startsWith)
}

const isFilteredEnum = (deltaEnum: DatasetDeltaEnum): boolean =>
  deltaEnum === DatasetDeltaEnum.NO_OP
const isDatasetEnum = (deltaEnum: DatasetDeltaEnum): boolean =>
  testForType(deltaEnum, 'DATASET')
const isTableEnum = (deltaEnum: DatasetDeltaEnum): boolean =>
  testForType(deltaEnum, 'TABLE')
const isColumnEnum = (deltaEnum: DatasetDeltaEnum): boolean =>
  testForType(deltaEnum, 'COLUMN')
const normalizedDeltaEnum = (deltaEnum: DatasetDeltaEnum): string => {
  const val = DatasetDeltaEnum[deltaEnum]
  return upperFirst(val?.replaceAll('_', ' ').toLowerCase())
}

export {
  isFilteredEnum,
  isDatasetEnum,
  isTableEnum,
  isColumnEnum,
  normalizedDeltaEnum,
}
