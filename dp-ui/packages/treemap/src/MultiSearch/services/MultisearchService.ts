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
import { isEmpty, isNil, sortBy } from 'lodash'
import { optionalFilterIfDefined } from '../../helpers/ObjectHelper'
import SelectedDrilldown from '../../drilldown/models/SelectedDrilldown'
import AggregatedSearchResult from '../models/AggregatedSearchResult'
import AggregratedSearchResultElement from '../models/AggregatedSearchResultElement'
import SearchResultElement from '../models/SearchResultElement'
import AggregatedSearchResultHelper from '../models/AggregatedSearchResultHelper'

/**
 * Service class to transform, search,filter, and sort results of a multisearch
 * mostly scoped to those methods used in the autosuggest box for the ui
 *
 * @see services/AggregatedSearchResultService for other non autosuggest box methods
 */
export default class MultiSearchService {
  generateId(): string {
    return AggregatedSearchResultHelper.generateId()
  }

  emptyResult(): AggregatedSearchResult {
    return AggregatedSearchResultHelper.emptyResult()
  }

  /**
   * sort results by their top level count
   * returns the topN or all
   * @param results
   * @param topN
   *  optional param, topN of the results or all the results
   */
  topDisplaySearchResultsByValueCounts(
    results: AggregatedSearchResult[],
    topN = results.length
  ): AggregatedSearchResult[] {
    if (isEmpty(results)) return []
    const safeTopN = topN <= results.length ? topN : results.length
    const topResults = results
      // sort
      .sort((a, b) => b.count - a.count)
      // take topN
      .slice(0, safeTopN)
    return topResults
  }

  /**
   * sort results by their sum of element counts
   * and returns the topN or all
   * @param results
   * @param topN
   *  optional param, topN of the results or all the results
   */
  topDisplaySearchResultsBySummedElementCounts(
    results: AggregatedSearchResult[],
    topN = results.length
  ): AggregatedSearchResult[] {
    if (isEmpty(results)) return []
    const safeTopN = topN <= results.length ? topN : results.length
    const topResults = results
      // sort
      .sort((a, b) => {
        const aElementCount = a.elements
          .map((el) => el.count)
          .reduce((prev, cur) => prev + cur)
        const bElementCount = b.elements
          .map((el) => el.count)
          .reduce((prev, cur) => prev + cur)
        return bElementCount - aElementCount
      })
      // take topN
      .slice(0, safeTopN)
    return topResults
  }

  /**
   * sort results by their max elements
   * and returns the topN or all
   * @param results
   * @param topN
   *  optional param, topN of the results or all the results
   */
  topDisplaySearchResultsByMaxElementCounts(
    results: AggregatedSearchResult[],
    topN = results.length
  ): AggregatedSearchResult[] {
    if (isEmpty(results)) return []
    const safeTopN = topN <= results.length ? topN : results.length
    const topResults = results
      // sort
      .sort((a, b) => {
        const aMaxCount = a.elements
          .map((el) => el.count)
          .reduce((prev, cur) => prev + cur, -Infinity)
        const bMaxCount = b.elements
          .map((el) => el.count)
          .reduce((prev, cur) => prev + cur, -Infinity)
        return bMaxCount - aMaxCount
      })
      // take topN
      .slice(0, safeTopN)
    return topResults
  }

  /**
   * sort results by their sum of element counts
   * and returns the topN or all
   * @param results
   * @param topN
   *  optional param, topN of the results or all the results
   */
  topSearchElements(
    results: AggregatedSearchResult[],
    topN = results.length
  ): AggregratedSearchResultElement[] {
    if (isEmpty(results)) return []
    const topResults = results
      .flatMap((result) => result.elements)
      .sort((a, b) => b.count - a.count)
    const safeTopN = topN <= topResults.length ? topN : topResults.length
    return topResults.slice(0, safeTopN)
  }

  /**
   * filters and sums an aggregated search result
   * @param results
   * @param selected
   */
  filterAndSum(
    results: Readonly<AggregatedSearchResult>,
    selected: SelectedDrilldown = new SelectedDrilldown()
  ): AggregatedSearchResult {
    if (!results) {
      return results
    }
    // pick out elements that match the current selected preview table and column
    // then do a sort as descending
    const elements = sortBy(
      this.optionalFilterElements(results.elements, selected),
      (el) => el.count * -1
    )
    const value = results.value
    const count = elements
      .map((el) => el.count)
      .reduce((prev, cur) => prev + cur, 0)
    return {
      id: this.generateId(),
      value,
      count,
      elements: [...elements],
    }
  }

  /**
   * sums all the aggregated elements
   * displays as the top drop down element in the ui
   * represents all the value results aggregated as a single object
   * @param results
   * @param value
   */
  sumAllResults(
    results: Readonly<AggregatedSearchResult[]>,
    value: Readonly<string> = ''
  ): AggregatedSearchResult {
    results = results?.filter((el) => !isNil(el))
    if (isEmpty(results)) {
      return this.emptyResult()
    }
    const allElements = results
      ?.filter((el) => !isNil(el))
      .filter((el) => el && el.elements)
      .flatMap((el) => el.elements)
    const count = allElements
      ?.map((el) => el.count)
      .reduce((prev, cur) => prev + cur, 0)
    return {
      id: this.generateId(),
      value,
      count,
      elements: [...allElements],
    }
  }

  /**
   * filter elements if a filter is defined, otherwise pass the elements through
   * @param results
   */
  optionalFilterElements<
    T extends SearchResultElement | AggregratedSearchResultElement
  >(
    elements: Array<T>,
    filter: SelectedDrilldown = new SelectedDrilldown()
  ): Array<T> {
    if (isEmpty(elements)) return []
    if (filter.isEmpty()) {
      return elements
    }
    return elements.filter((el) => {
      const isDatasetPass = optionalFilterIfDefined(el.dataset, filter.dataset)
      const isTablePass = optionalFilterIfDefined(el.table, filter.table)
      const isColumnPass = optionalFilterIfDefined(el.column, filter.column)
      return isDatasetPass && isTablePass && isColumnPass
    })
  }
}
