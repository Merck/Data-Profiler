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
import CompleteDelta from '../../../models/delta/CompleteDelta'
import DatasetDeltaService from '../../../services/delta/DatasetDeltaService'
import DatasetDeltaStub from '../../mocks/DatasetDelta.stub'

describe('given a dataset delta service', () => {
  let completeDelta: CompleteDelta
  let service: DatasetDeltaService

  beforeEach(() => {
    completeDelta = DatasetDeltaStub.genSample()
    service = new DatasetDeltaService()
  })

  test('then expect sane test data', () => {
    expect(completeDelta).toBeTruthy()
  })

  test('then expect an adapter object', () => {
    expect(service).toBeTruthy()
  })

  test('then expect to detect is a dataset delta type is current', () => {
    const { lastKnownVersions, deltas } = completeDelta
    const isCurrent = service.isCurrentVersion(lastKnownVersions, deltas[0])
    expect(isCurrent).toBeTruthy()
  })

  test('then expect to detect if a table delta type is current', () => {
    const { lastKnownVersions, deltas } = completeDelta
    const isCurrent = service.isCurrentVersion(lastKnownVersions, deltas[0])
    expect(isCurrent).toBeTruthy()
  })

  test('then expect to detect if a column delta type is current', () => {
    const { lastKnownVersions, deltas } = completeDelta
    const isCurrent = service.isCurrentVersion(lastKnownVersions, deltas[0])
    expect(isCurrent).toBeTruthy()
  })
})
