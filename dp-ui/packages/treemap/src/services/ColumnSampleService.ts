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
import { isEmpty, orderBy } from 'lodash'
import ColumnSample from '../models/ColumnSample'

export class ColumnSampleService {
  /**
   *
   * @param dataset
   * @param table
   * @param column
   * @param samples
   * @param min
   */
  filterAndRerank(
    dataset: string,
    table: string,
    column: string,
    samples?: Readonly<ColumnSample[]>,
    min = 21
  ): Readonly<ColumnSample[]> {
    if (!samples || isEmpty(samples)) {
      return []
    }

    const filtered = samples?.filter((el) => {
      return (
        el?.dataset === dataset && el?.table === table && el?.column === column
      )
    })
    return orderBy(filtered, 'count', 'desc')
  }
}
