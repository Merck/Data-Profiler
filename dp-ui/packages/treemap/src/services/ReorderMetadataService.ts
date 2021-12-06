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
import { isEmpty, isUndefined } from 'lodash'
import CommonMetadata from '../models/CommonMetadata'
import ReorderCommonMetadata from '../models/ReorderCommonMetadata'
import AggregatedSearchResult from '../MultiSearch/models/AggregatedSearchResult'
import AggregratedSearchResultElement from '../MultiSearch/models/AggregatedSearchResultElement'
import { DEFAULT_ORDER_BY, ORDER_BY_ENUM } from '../orderby/models/OrderBy'
import ReorderMetadataComparators from './ReorderMetadataComparators'

/**
 * Operations and helper methods for ReorderCommonMetadata classes
 * Mostly doing resorting and filtering for List View
 */
export default class ReorderMetadataService {
  globalOrderByMap: {
    [s in ORDER_BY_ENUM]: <T extends ReorderCommonMetadata>(
      a: T,
      b: T
    ) => number
  } = Object.assign({})

  constructor() {
    this.globalOrderByMap[ORDER_BY_ENUM.TITLE_ASC] =
      ReorderMetadataComparators.compareByDatasetTitle('asc')
    this.globalOrderByMap[ORDER_BY_ENUM.TITLE_DESC] =
      ReorderMetadataComparators.compareByDatasetTitle('desc')
    this.globalOrderByMap[ORDER_BY_ENUM.VALUE_ASC] =
      ReorderMetadataComparators.compareByValue('asc')
    this.globalOrderByMap[ORDER_BY_ENUM.VALUE_DESC] =
      ReorderMetadataComparators.compareByValue('desc')
    this.globalOrderByMap[ORDER_BY_ENUM.LOADED_ON_ASC] =
      ReorderMetadataComparators.compareByLoadedOn('asc')
    this.globalOrderByMap[ORDER_BY_ENUM.LOADED_ON_DESC] =
      ReorderMetadataComparators.compareByLoadedOn('desc')
  }

  /**
   * Reorder metadata for List View; we would like as a default (no explicit sort selection)
   * if no search hits;
   *  then order by global order by for nonhits, eg: alphabetical by title or num values,etc
   *
   * if search hits;
   *  then order by number of hits in values descending
   *  then order by index of hits in title ascending
   *  then order by global user selection for nonhits, eg: alphabetical by title or num values,etc
   *
   * @param metadata
   * @param filter
   */
  reorderDatasets(
    metadata: Readonly<CommonMetadata[]>,
    hits: Readonly<AggregatedSearchResult>,
    userSelecterOrderBy: ORDER_BY_ENUM = DEFAULT_ORDER_BY
  ): Array<ReorderCommonMetadata> {
    if (!metadata) {
      return []
    }
    if (!hits) {
      return this.emptyHitsDefaultSort(metadata, userSelecterOrderBy)
    }

    const elements = hits.elements || []
    const uniqDatasets = new Set(elements.map((el) => el.dataset.toLowerCase()))
    if (uniqDatasets.size === 0) {
      return this.emptyHitsDefaultSort(metadata, userSelecterOrderBy)
    }

    const match = (hits.value || '').toLowerCase()
    const enchanced = metadata.map((el) => {
      const dataset = el.datasetName.toLowerCase()
      const hasSearchHit = uniqDatasets.has(dataset)
      let numHits = 0
      if (hasSearchHit) {
        numHits = this.calculateNumberOfHits(dataset, elements)
      }
      return {
        hasSearchHit,
        numHits,
        commonMetadata: el,
      }
    })

    // then rank by most value hits
    const compareByHits = ReorderMetadataComparators.compareByHits()
    // then rank index of title hits asc,
    const compareByDatasetTitleMatch =
      ReorderMetadataComparators.compareByDatasetTitleMatch(match)
    // then user selected title for nonhits
    const userSelectedComparator = this.globalOrderByMap[userSelecterOrderBy]
    return enchanced.sort((left, right) => {
      return (
        compareByHits(left, right) ||
        compareByDatasetTitleMatch(left, right) ||
        userSelectedComparator(left, right)
      )
    })
  }

  /**
   * return metadata elements that have search hits as a value or in the title
   * if no search hits are found, then just return all the elements
   * @param metadata
   * @param filter
   */
  filterHits(
    metadata: Readonly<CommonMetadata[]>,
    hits: Readonly<AggregatedSearchResult>
  ): CommonMetadata[] {
    if (!metadata) {
      return []
    }

    if (!hits) {
      return [...metadata]
    }

    const elements = hits.elements || []
    const uniqDatasets = new Set(elements.map((el) => el.dataset.toLowerCase()))
    // const match = (hits.value || '').toLowerCase()
    return metadata.filter((el) => {
      const dataset = el.datasetName.toLowerCase()
      // const hasTitleHit = match !== undefined && dataset.indexOf(match) > -1
      return uniqDatasets.has(dataset)
    })
  }

  emptyHitsDefaultSort(
    metadata: Readonly<CommonMetadata[]>,
    userSelecterOrderBy: ORDER_BY_ENUM = DEFAULT_ORDER_BY
  ): Array<ReorderCommonMetadata> {
    const userSelectedComparator = this.globalOrderByMap[userSelecterOrderBy]
    return metadata
      .map((el) => ({
        hasTitleHit: false,
        hasSearchHit: false,
        numHits: 0,
        commonMetadata: el,
      }))
      .sort(userSelectedComparator)
  }

  calculateNumberOfHits(
    dataset: string,
    elements: Readonly<AggregratedSearchResultElement[]>
  ): number {
    if (isUndefined(elements) || isEmpty(elements)) {
      return 0
    }

    const ds = (dataset || '').toLowerCase()
    const filtered = elements.filter((el) => el.dataset.toLowerCase() === ds)
    return filtered.reduce((prev, cur) => prev + cur.count, 0)
  }
}
