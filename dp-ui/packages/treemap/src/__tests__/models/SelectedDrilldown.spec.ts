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
import SelectedDrilldown from '../../drilldown/models/SelectedDrilldown'

describe('given a SelectedDrilldown', () => {
  let selectedDrilldown: SelectedDrilldown

  beforeEach(() => {
    selectedDrilldown = new SelectedDrilldown()
  })

  it('expect the default view to be all datasets view', () => {
    expect(selectedDrilldown).toBeTruthy()
    expect(selectedDrilldown.isDatasetView()).toBeTruthy()
    expect(selectedDrilldown.isTableView()).toBeFalsy()
    expect(selectedDrilldown.isColumnView()).toBeFalsy()
    expect(selectedDrilldown.currentView()).toEqual('dataset')
  })

  it('and a selected dataset, expect the view to be table view', () => {
    expect(selectedDrilldown).toBeTruthy()
    selectedDrilldown.dataset = 'dataset1'
    expect(selectedDrilldown.isDatasetView()).toBeFalsy()
    expect(selectedDrilldown.isTableView()).toBeTruthy()
    expect(selectedDrilldown.isColumnView()).toBeFalsy()
    expect(selectedDrilldown.currentView()).toEqual('table')
  })

  it('and a selected dataset+table, expect the view to be column view', () => {
    expect(selectedDrilldown).toBeTruthy()
    selectedDrilldown.dataset = 'dataset1'
    selectedDrilldown.table = 'table1'
    expect(selectedDrilldown.isDatasetView()).toBeFalsy()
    expect(selectedDrilldown.isTableView()).toBeFalsy()
    expect(selectedDrilldown.isColumnView()).toBeTruthy()
    expect(selectedDrilldown.currentView()).toEqual('column')
  })

  it('expect a factory method to take an object', () => {
    const obj = {
      dataset: 'ds1',
      table: 't1',
      column: 'c1',
    }
    const drilldown = SelectedDrilldown.of(obj)
    expect(drilldown).toBeTruthy()
    expect(drilldown.dataset).toEqual('ds1')
    expect(drilldown.table).toEqual('t1')
    expect(drilldown.column).toEqual('c1')
    expect(drilldown.isDatasetView()).toBeFalsy()
    expect(drilldown.isTableView()).toBeFalsy()
    expect(drilldown.isColumnView()).toBeFalsy()
    expect(drilldown.isSpecificColumnView()).toBeTruthy()
    expect(drilldown.currentView()).toEqual('specific column')
  })

  it('expect a factory method to take a existing selectedDrilldown', () => {
    selectedDrilldown.dataset = 'ds1'
    selectedDrilldown.table = 't1'
    selectedDrilldown.column = 'c1'
    const drilldown = SelectedDrilldown.from(selectedDrilldown)
    expect(drilldown).toBeTruthy()
    expect(drilldown.dataset).toEqual('ds1')
    expect(drilldown.table).toEqual('t1')
    expect(drilldown.column).toEqual('c1')
    expect(drilldown.isDatasetView()).toBeFalsy()
    expect(drilldown.isTableView()).toBeFalsy()
    expect(drilldown.isColumnView()).toBeFalsy()
    expect(drilldown.isSpecificColumnView()).toBeTruthy()
    expect(drilldown.currentView()).toEqual('specific column')
  })

  it('expect to know when it is empty', () => {
    const drilldown = new SelectedDrilldown()
    expect(drilldown).toBeTruthy()
    expect(drilldown.isEmpty()).toBeTruthy()
  })

  it('expect to know when it is not empty', () => {
    const drilldown = new SelectedDrilldown('ds1')
    expect(drilldown).toBeTruthy()
    expect(drilldown.isEmpty()).toBeFalsy()
  })

  it('expect to know how to serialize', () => {
    const drilldown = new SelectedDrilldown('ds1', 't1', 'c5')
    expect(drilldown).toBeTruthy()
    expect(drilldown.isEmpty()).toBeFalsy()
    const serialized = drilldown.serialize()
    expect(serialized).toBeTruthy()
    expect(serialized['dataset']).toEqual('ds1')
    expect(serialized['table']).toEqual('t1')
    expect(serialized['column']).toEqual('c5')
  })

  it('expect to know how to serialize', () => {
    const drilldown = new SelectedDrilldown('ds1', 't1', 'c5')
    expect(drilldown).toBeTruthy()
    expect(drilldown.isEmpty()).toBeFalsy()
    const serialized = drilldown.serialize()
    const roundTrip = SelectedDrilldown.of(serialized)
    expect(roundTrip).toBeTruthy()
    expect(roundTrip.isSpecificColumnView).toBeTruthy()
    expect(roundTrip.isSpecificColumnView()).toBeTruthy()
    expect(roundTrip.isDatasetView).toBeTruthy()
    expect(roundTrip.isDatasetView()).toBeFalsy()
  })

  it('expect to know how to compare as equals', () => {
    const d1 = new SelectedDrilldown('ds1', 't1', 'c5')
    const d2 = new SelectedDrilldown('ds1', 't1', 'c5')
    expect(d1.equals(d2)).toBeTruthy()
  })

  it('expect to know how to compare as not equals', () => {
    const d1 = new SelectedDrilldown('ds1', 't1', 'c5')
    const d2 = new SelectedDrilldown('dsX', 'tX', 'cX')
    expect(d1.equals(d2)).toBeFalsy()
  })
})
