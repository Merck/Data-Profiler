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
import {
  DatasetDeltaEnum,
  isColumnEnum,
  isDatasetEnum,
  isTableEnum,
  normalizedDeltaEnum,
} from '../../../models/delta/DatasetDeltaEnum'

describe('given a DatasetDeltaEnum', () => {
  let datasetDeltaEnum: DatasetDeltaEnum

  beforeEach(() => {
    datasetDeltaEnum = DatasetDeltaEnum.DATASET_VALUES_INCREASED
  })

  it('expect a dataset delta enum', () => {
    expect(datasetDeltaEnum).toBeTruthy()
  })

  it('expect to know if a delta enum is a dataset class', () => {
    expect(isDatasetEnum(DatasetDeltaEnum.NO_OP)).toBeFalsy()
    expect(
      isDatasetEnum(DatasetDeltaEnum.DATASET_VALUES_DECREASED)
    ).toBeTruthy()
    expect(
      isDatasetEnum(DatasetDeltaEnum.DATASET_VALUES_INCREASED)
    ).toBeTruthy()
  })

  it('expect to know if a delta enum is a table class', () => {
    expect(isTableEnum(DatasetDeltaEnum.NO_OP)).toBeFalsy()
    expect(isTableEnum(DatasetDeltaEnum.TABLE_ADDED)).toBeTruthy()
    expect(isTableEnum(DatasetDeltaEnum.TABLE_REMOVED)).toBeTruthy()
    expect(isTableEnum(DatasetDeltaEnum.TABLE_VALUES_DECREASED)).toBeTruthy()
    expect(isTableEnum(DatasetDeltaEnum.TABLE_VALUES_INCREASED)).toBeTruthy()
  })

  it('expect to know if a delta enum is a column class', () => {
    expect(isColumnEnum(DatasetDeltaEnum.NO_OP)).toBeFalsy()
    expect(isColumnEnum(DatasetDeltaEnum.COLUMN_ADDED)).toBeTruthy()
    expect(isColumnEnum(DatasetDeltaEnum.COLUMN_REMOVED)).toBeTruthy()
    expect(isColumnEnum(DatasetDeltaEnum.COLUMN_VALUES_DECREASED)).toBeTruthy()
    expect(isColumnEnum(DatasetDeltaEnum.COLUMN_VALUES_INCREASED)).toBeTruthy()
  })

  it.skip('expect a hooman friendly name for delta enum', () => {
    // expect(normalizedDeltaEnum('NO_OP')).toBe('')
    // expect(normalizedDeltaEnum(DatasetDeltaEnum.NO_OP)).toBe('')
    expect(normalizedDeltaEnum(DatasetDeltaEnum.DATASET_VALUES_INCREASED)).toBe(
      ''
    )
  })
})
