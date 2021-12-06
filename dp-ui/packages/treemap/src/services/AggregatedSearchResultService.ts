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
import { legendColor } from 'd3-svg-legend'
import { isEmpty, isNil, isUndefined, uniqBy } from 'lodash'
import { optionalFilterIfDefined } from '../helpers/ObjectHelper'
import AggregatedSearchResult from '../MultiSearch/models/AggregatedSearchResult'
import AggregratedSearchResultElement from '../MultiSearch/models/AggregatedSearchResultElement'

/**
 * Service class contains methods to transform, filter, and sum aggregated search results
 * Mostly used for components outside the multisearch autosuggest box
 *
 * @see Multisearch/services/MultisearchService for other more autosuggest box specific methods
 */
export default class AggregratedSearchResultService {
  /**
   * calculate the number of unique datasets found in the result set
   * @param result
   */
  calcNumDatasets(result: Readonly<AggregatedSearchResult>): number {
    if (!result || isEmpty(result.elements)) {
      return 0
    }

    const elements = result.elements
    return uniqBy(elements, (el) => el.dataset).length || 0
  }

  /**
   * calculate the number of unique tables found in the result set
   * @param result
   * @param dataset - filter on dataset name
   */
  calcNumTables(
    result: Readonly<AggregatedSearchResult>,
    dataset?: string
  ): number {
    if (!result || isEmpty(result.elements)) {
      return 0
    }

    const elements = result.elements
    const tables = elements
      .filter((el) => el && el.dataset && el.table)
      .filter((el) => optionalFilterIfDefined(el.dataset, dataset))
    return uniqBy(tables, (el) => `${el.dataset} ${el.table}`).length || 0
  }

  /**
   * calculate the number of unique columns found in the result set
   * @param result
   * @param dataset - filter on dataset name
   * @param table - filter on table name
   */
  calcNumColumns(
    result: Readonly<AggregatedSearchResult>,
    dataset?: string,
    table?: string
  ): number {
    if (!result || isEmpty(result.elements)) {
      return 0
    }

    const elements = result.elements
    const columns = elements
      .filter((el) => el && el.dataset && el.table && el.column)
      .filter(
        (el) =>
          optionalFilterIfDefined(el.dataset, dataset) &&
          optionalFilterIfDefined(el.table, table)
      )
    return (
      uniqBy(columns, (el) => `${el.dataset} ${el.table} ${el.column}`)
        .length || 0
    )
  }

  /**
   * calculate the sum of hits for the given filter critera
   * to sum at the dataset level only supply a dataset
   * to sum at the table level supply a dataset, and table
   * to sum at the column level supply all three
   *
   * this function does not guard against acciently failing to pass
   *  say an empty dataset name with populated table and columns
   *
   * this function will also filter out title hits
   *  as not to incorrectly count towards value hits
   * @param hits
   * @param dataset - filter on dataset name
   * @param table - filter on table name
   * @param column - filter on all three
   */
  calcValueHitCount(
    hits: Readonly<AggregatedSearchResult>,
    dataset?: string,
    table?: string,
    column?: string
  ): number {
    if (!hits || isEmpty(hits?.elements)) {
      return 0
    }

    const elements = hits?.elements
      .filter((el) => !isUndefined(el))
      .filter((el) => !el?.isTitleResult)
      .filter((el) => {
        const datasetCheckOrPass =
          !isUndefined(dataset) && !isEmpty(dataset)
            ? el.dataset === dataset
            : true
        const tableCheckOrPass =
          !isUndefined(table) && !isEmpty(table) ? el.table === table : true
        const columnCheckOrPass =
          !isUndefined(column) && !isEmpty(column) ? el.column === column : true
        return datasetCheckOrPass && tableCheckOrPass && columnCheckOrPass
      })
    return elements.length > 0
      ? elements.reduce((prev, cur) => prev + (cur?.count || 0), 0)
      : 0
  }

  /**
   * TODO: update, if we ever support multiple search terms/chips
   *
   * determines if a given element is a title search hit or not
   *
   * @param element
   * @return boolean
   *  true if a title search hit, otherwise false
   */
  isTitleSearchElement(
    element: Readonly<AggregratedSearchResultElement>
  ): boolean {
    if (!element) {
      return false
    }

    return element?.isTitleResult || false
  }

  /**
   * TODO: update, if we ever support multiple search terms/chips
   *
   * @param results
   * @return boolean
   *  true if any items are a title search hit, otherwise false
   */
  hasTitleHit(results: Readonly<AggregatedSearchResult>): boolean {
    if (!results || isEmpty(results?.elements)) {
      return false
    }

    return results.elements.some((el) => this.isTitleSearchElement(el))
  }

  /**
   * filter to return none title count hits
   *
   * TODO: update, if we ever support multiple search terms/chips
   *
   * @param results
   * @return AggregatedSearchResult
   */
  filterNoneTitleHits(
    results: Readonly<AggregatedSearchResult>
  ): AggregatedSearchResult {
    if (!results || !results.elements) {
      return results
    }
    const check = (el) => !this.isTitleSearchElement(el)
    return this.filterTitleHits(results, check)
  }

  /**
   * filter to return just title count hits
   *
   * TODO: update, if we ever support multiple search terms/chips
   *
   * @param results
   * @return AggregatedSearchResult
   */
  filterForTitleHits(
    results: Readonly<AggregatedSearchResult>
  ): AggregatedSearchResult {
    if (!results || !results.elements) {
      return results
    }
    const check = (el) => this.isTitleSearchElement(el)
    return this.filterTitleHits(results, check)
  }

  /**
   * filter based on title hits
   *
   * TODO: update, if we ever support multiple search terms/chips
   *
   * @param results
   * @param doCheck
   * @return AggregatedSearchResult
   */
  filterTitleHits(
    results: Readonly<AggregatedSearchResult>,
    doCheck: (element: Readonly<AggregratedSearchResultElement>) => boolean
  ): AggregatedSearchResult {
    if (!results || !results.elements) {
      return results
    }

    const filteredElements = results.elements
      .filter((el) => !isNil(el))
      .filter((el) => doCheck(el))
    const count = filteredElements
      ?.map((el) => el.count)
      .reduce((prev, cur) => prev + cur, 0)
    return {
      ...results,
      count,
      elements: [...filteredElements],
    }
  }
}
