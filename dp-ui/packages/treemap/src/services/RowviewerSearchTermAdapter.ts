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
import SearchColumnInfo from '../models/rowviewer/SearchColumnInfo'
import AggregatedSearchResult from '../MultiSearch/models/AggregatedSearchResult'

export default class RowviewerSearchTermAdapter {
  convertAll(
    dataset: string,
    table: string,
    filter: AggregatedSearchResult
  ): SearchColumnInfo {
    if (!dataset || !table || !filter) {
      return
    }

    const elements = filter.elements.filter(
      (el) =>
        el.dataset.trim().toLowerCase() === dataset.trim().toLowerCase() &&
        el.table.trim().toLowerCase() === table.trim().toLowerCase()
    )
    const uniqColumns = new Set(
      elements.map((el) => el.column.trim().toLowerCase())
    )
    const searchColumnInfo: SearchColumnInfo = Array.from(uniqColumns).reduce(
      (memo, column) => {
        const uniqValues = new Set(
          elements.map((el) => el.value.trim().toLowerCase())
        )
        Array.from(uniqValues).map((value) => {
          const count = elements
            .filter((el) => el.column.trim().toLowerCase() === column)
            .filter((el) => el.value === value)
            .map((el) => el.count)
            .reduce((prev, cur) => prev + cur, 0)
          if (!memo[column]) {
            memo[column] = []
          }
          const searchColumn = {
            [value]: count,
          }
          memo[column] = [memo[column], searchColumn]
        })
        return memo
      },
      {}
    )
    return searchColumnInfo
  }
}
