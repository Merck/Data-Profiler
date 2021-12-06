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
import { isEmpty } from 'lodash'
import AggregatedSearchResult from '../models/AggregatedSearchResult'
import AggregratedSearchResultElement from '../models/AggregatedSearchResultElement'
import SearchResult from '../models/SearchResult'
import SearchResultElement from '../models/SearchResultElement'
import AggregatedSearchResultHelper from '../models/AggregatedSearchResultHelper'

/**
 * Class to hold various transformations to display results in the
 * autosuggest box in the ui
 */
export default class MultisearchAdapter {
  generateId(): string {
    return AggregatedSearchResultHelper.generateId()
  }

  emptyResult(): AggregatedSearchResult {
    return AggregatedSearchResultHelper.emptyResult()
  }

  errorResult(msg: string): AggregatedSearchResult {
    return AggregatedSearchResultHelper.errorResult(msg)
  }

  /**
   * transforms a single multisearch rest result
   * into the aggregated view for the ui
   * @param result
   */
  endpointToAggregatedResult(
    result: Readonly<SearchResult>
  ): AggregatedSearchResult {
    if (result?.error) {
      return this.errorResult(result.error)
    }
    return {
      id: this.generateId(),
      value: result.value.reduce((prev, cur) => `${prev} ${cur}`),
      count: result.count,
      elements: this.convertAll(result.elements),
    }
  }

  /**
   * transforms an array of multisearch rest results
   * into the aggregated view for the ui
   * @param results
   */
  endpointToAggegratedResults(
    results: Readonly<SearchResult[]>
  ): AggregatedSearchResult[] {
    if (isEmpty(results)) return []
    return results.map((el) => this.endpointToAggregatedResult(el))
  }

  /**
   * transform an array of rest search result elements into a form for the ui
   * @param elements
   */
  convertAll(
    elements: Readonly<SearchResultElement[]>
  ): AggregratedSearchResultElement[] {
    if (isEmpty(elements)) return []
    return elements.map((el) => this.convert(el))
  }

  /**
   * transform a single rest search result element into a form for the ui
   * basically flattens the count and values arrays of a search result element
   * @param element
   */
  convert(
    element: Readonly<SearchResultElement>
  ): AggregratedSearchResultElement {
    const values = element.value
    const counts = element.count
    const value = values.reduce((prev, cur) => `${prev} ${cur}`)
    const count = counts.reduce((prev, cur) => prev + cur, 0)
    return {
      id: this.generateId(),
      value,
      count,
      column: element.column,
      dataset: element.dataset,
      table: element.table,
      tableId: element.tableId,
    }
  }
}
