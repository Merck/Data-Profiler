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
import { objectKeysToFlatArray } from '../helpers/ObjectHelper'
import ColumnSample from '../models/ColumnSample'

export default class ColumnSampleAdapter {
  endpointToColumnSamples(
    samples: Readonly<Record<string, Array<{ value: string; count: number }>>>,
    dataset = '',
    table = ''
  ): Array<ColumnSample> {
    if (!samples) {
      return []
    }

    const arr = objectKeysToFlatArray(samples, true, 'column')
    return this.convertAll(arr, dataset, table)
  }

  convertAll(
    samples: Readonly<Array<Record<string, any>>>,
    dataset = '',
    table = ''
  ): Array<ColumnSample> {
    if (!samples || !(samples.length > 0)) {
      return []
    }
    return samples.map((el) => this.convert(dataset, table, el))
  }

  convert(dataset = '', table = '', el: Record<string, any>): ColumnSample {
    const column = el.column || ''
    const count = el.count || 0
    const value = el.value || ''
    return {
      dataset,
      table,
      column,
      value,
      count,
    }
  }
}
