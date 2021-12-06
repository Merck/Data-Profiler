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
import ColumnCounts from '../models/ColumnCounts'
import ValueCount from '../models/ValueCount'

export type ColumnCountEndpointShape = { n: string; c: number }

export default class ColumnCountsAdapter {
  endpointToColumnCounts(
    values: Readonly<Array<ColumnCountEndpointShape>>,
    dataset = '',
    table = '',
    column = ''
  ): ColumnCounts {
    if (!values) {
      return {
        dataset,
        table,
        column,
        values: [],
      }
    }

    return this.convertAll(values, dataset, table, column)
  }

  convertAll(
    endpointData: Readonly<Array<ColumnCountEndpointShape>>,
    dataset = '',
    table = '',
    column = ''
  ): ColumnCounts {
    if (!endpointData || !(endpointData.length > 0)) {
      return {
        dataset,
        table,
        column,
        values: [],
      }
    }
    const values = endpointData.map((el) => this.convert(el))
    return {
      dataset,
      table,
      column,
      values,
    }
  }

  convert(el: ColumnCountEndpointShape): ValueCount {
    const count = el.c || 0
    const value = (el.n || '').trim()
    return {
      value,
      count,
    }
  }
}
