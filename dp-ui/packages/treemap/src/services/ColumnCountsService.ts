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
import { groupBy, isEmpty, orderBy, uniq, uniqBy } from 'lodash'
import ColumnCounts from '../models/ColumnCounts'
import SelectedDrilldown from '../drilldown/models/SelectedDrilldown'
import ValueCount from '../models/ValueCount'
import AggregatedSearchResult from '../MultiSearch/models/AggregatedSearchResult'

export enum RankEnum {
  HAS_LOCAL_AND_GLOBAL_MATCH,
  HAS_LOCAL_MATCH_ONLY,
  HAS_GLOBAL_MATCH_ONLY,
  NO_MATCH,
}

export class ColumnCountsService {
  /**
   * partition and rerank the list,
   * the order is;
   *  local AND global matches
   *  local matches only
   *  global matches only
   *  then nonmatches
   * with a secondary value sort
   * @param columnCounts
   * @param matches
   */
  rerank(
    columnCounts: Readonly<ColumnCounts>,
    localMatches?: Readonly<string[]>,
    globalMatches?: Readonly<string[]>
  ): Record<keyof typeof RankEnum, ColumnCounts> {
    const normalizedLocalMatches = localMatches
      ?.filter((el) => !isEmpty(el))
      ?.map((el) => el?.trim()?.toLowerCase())
    const normalizedGlobalMatches = globalMatches
      ?.filter((el) => !isEmpty(el))
      ?.map((el) => el?.trim()?.toLowerCase())
    const emptyMatchTerms =
      isEmpty(normalizedLocalMatches) && isEmpty(normalizedGlobalMatches)
    if (isEmpty(columnCounts?.values) || emptyMatchTerms) {
      return {
        HAS_LOCAL_AND_GLOBAL_MATCH: {
          ...columnCounts,
          values: [],
        },
        HAS_LOCAL_MATCH_ONLY: {
          ...columnCounts,
          values: [],
        },
        HAS_GLOBAL_MATCH_ONLY: {
          ...columnCounts,
          values: [],
        },
        NO_MATCH: {
          ...columnCounts,
        },
      }
    }
    const valuePartitions = groupBy(columnCounts.values, (el) => {
      const normalizedTerm = el.value.trim().toLowerCase()
      const hasLocalMatch = normalizedLocalMatches.some(
        (match) => normalizedTerm.indexOf(match) > -1
      )
      const hasGlobalMatch = normalizedGlobalMatches.some(
        (match) => normalizedTerm.indexOf(match) > -1
      )
      if (hasLocalMatch && hasGlobalMatch) {
        return RankEnum.HAS_LOCAL_AND_GLOBAL_MATCH
      } else if (hasLocalMatch && !hasGlobalMatch) {
        return RankEnum.HAS_LOCAL_MATCH_ONLY
      } else if (hasGlobalMatch && !hasLocalMatch) {
        return RankEnum.HAS_GLOBAL_MATCH_ONLY
      } else {
        return RankEnum.NO_MATCH
      }
    })
    const localAndGlobalMatched = {
      ...columnCounts,
      values: orderBy(
        valuePartitions[RankEnum.HAS_LOCAL_AND_GLOBAL_MATCH],
        'count',
        'desc'
      ),
    }
    const localMatched = {
      ...columnCounts,
      values: orderBy(
        valuePartitions[RankEnum.HAS_LOCAL_MATCH_ONLY],
        'count',
        'desc'
      ),
    }
    const globalMatched = {
      ...columnCounts,
      values: orderBy(
        valuePartitions[RankEnum.HAS_GLOBAL_MATCH_ONLY],
        'count',
        'desc'
      ),
    }
    const nonMatched = {
      ...columnCounts,
      values: orderBy(valuePartitions[RankEnum.NO_MATCH], 'count', 'desc'),
    }

    return {
      HAS_LOCAL_AND_GLOBAL_MATCH: localAndGlobalMatched,
      HAS_LOCAL_MATCH_ONLY: localMatched,
      HAS_GLOBAL_MATCH_ONLY: globalMatched,
      NO_MATCH: nonMatched,
    }
  }

  /**
   *
   * @param colCounts
   * @param distributionFilter
   */
  filter(
    colCounts: Readonly<ColumnCounts>,
    filter?: Readonly<string>
  ): Readonly<ColumnCounts> {
    const filterValue = filter || ''
    const filtered = !isEmpty(filterValue)
      ? colCounts?.values?.filter(
          (el) => el?.value?.toLowerCase()?.indexOf(filterValue) > -1
        )
      : colCounts?.values
    return {
      ...colCounts,
      values: [...filtered],
    }
  }

  /**
   * column counts merged and uniqued with search hits
   *
   * @param hits
   * @param drilldown
   */
  uniqWithSearchResults(
    colCounts: Readonly<ColumnCounts>,
    searchResultHits: Readonly<AggregatedSearchResult>,
    drilldown?: SelectedDrilldown
  ): ColumnCounts {
    const uniqValues = orderBy(
      uniqBy(
        [
          ...colCounts.values,
          ...this.searchResultsToRankedValueCounts(searchResultHits, drilldown),
        ],
        'value'
      ),
      'count',
      'desc'
    )
    return {
      ...colCounts,
      values: uniqValues,
    }
  }

  /**
   * turn aggregated search result hits to column count format
   *
   * @param hits
   */
  searchResultsToRankedValueCounts(
    hits: Readonly<AggregatedSearchResult>,
    drilldown: SelectedDrilldown = new SelectedDrilldown()
  ): ValueCount[] {
    if (!hits) {
      return []
    }

    const { dataset, table, column } = drilldown
    const hitsFound =
      hits?.elements
        ?.filter(
          (el) =>
            el.dataset === dataset && el.table === table && el.column === column
        )
        ?.map((el) => {
          return {
            value: el.value,
            count: el.count,
          }
        }) || []
    return orderBy(hitsFound, 'count', 'desc')
  }

  /**
   * remove local search terms that are exact duplicate for any global terms
   *  showing in the ui duplicate search terms looks wrong
   * @param localTerms
   * @param globalTerms
   * @return string[]
   */
  dedupLocalSearchTerms(
    localTerms: Readonly<string[]> = [],
    globalTerms: Readonly<string[]> = []
  ): string[] {
    if (isEmpty(localTerms) || isEmpty(globalTerms)) {
      return [...localTerms]
    }

    const uniqGlobal = new Set(globalTerms.map((el) => el.toLowerCase()))
    return localTerms?.filter((el) => !uniqGlobal.has(el.toLowerCase()))
  }
}
