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
import AggregatedSearchResult from '../../MultiSearch/models/AggregatedSearchResult'
import NameSearchAdapter from '../../MultiSearch/services/NameSearchAdapter'
import MetadataResultStub from './MetadataResult.stub'

export default class AggregratedSearchResultStub {
  static genSample(): AggregatedSearchResult {
    return {} as AggregatedSearchResult
  }

  static genSampleWithTitleHit(): AggregatedSearchResult {
    return {} as AggregatedSearchResult
  }

  static genSampleWithMultipleTitleHit(): AggregatedSearchResult {
    return {} as AggregatedSearchResult
  }

  static genDatasetHits(
    drilldown?: SelectedDrilldown
  ): AggregatedSearchResult[] {
    const arr = MetadataResultStub.genDatasetResults(drilldown)
    const namesearchAdapter = new NameSearchAdapter()
    return namesearchAdapter.groupAndConvertAll(arr, 'dataset')
  }

  static genTableHits(drilldown?: SelectedDrilldown): AggregatedSearchResult[] {
    const arr = MetadataResultStub.genTableResults(drilldown)
    const namesearchAdapter = new NameSearchAdapter()
    return namesearchAdapter.groupAndConvertAll(arr, 'table')
  }

  static genColumnHits(
    drilldown?: SelectedDrilldown
  ): AggregatedSearchResult[] {
    const arr = MetadataResultStub.genColumnResults(drilldown)
    const namesearchAdapter = new NameSearchAdapter()
    return namesearchAdapter.groupAndConvertAll(arr, 'column')
  }
}
