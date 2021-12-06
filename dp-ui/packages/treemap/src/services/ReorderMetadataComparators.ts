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
import { isUndefined } from 'lodash'
import ReorderCommonMetadata from '../models/ReorderCommonMetadata'

type Order = 'asc' | 'desc'
const DEFAULT_ORDER: Order = 'desc'

/**
 * Various sort function generators for ReorderCommonMetadata classes
 */
export default class ReorderMetadataComparators {
  static ASC_MULTIPLIER = 1
  static DESC_MULTIPLIER = -1
  static DEFAULT_CASE_SENSITIVITY = true

  static compareByDatasetTitle<T extends ReorderCommonMetadata>(
    order: Order = DEFAULT_ORDER,
    caseSensitive: boolean = ReorderMetadataComparators.DEFAULT_CASE_SENSITIVITY
  ): (a: T, b: T) => number {
    const multiplier = ReorderMetadataComparators.orderToMultiplier(order)
    return (a: T, b: T) => {
      if (isUndefined(a) && isUndefined(b)) {
        return 0
      }

      let ds1 = a?.commonMetadata?.datasetName || ''
      let ds2 = b?.commonMetadata?.datasetName || ''
      if (caseSensitive === false) {
        ds1 = ds1.toLowerCase()
        ds2 = ds2.toLowerCase()
      }
      return (ds1 === ds2 ? 0 : ds1 > ds2 ? 1 : -1) * multiplier
    }
  }

  static compareByValue<T extends ReorderCommonMetadata>(
    order: Order = DEFAULT_ORDER
  ): (a: T, b: T) => number {
    const multiplier = ReorderMetadataComparators.orderToMultiplier(order)
    return (a: T, b: T) => {
      if (isUndefined(a) && isUndefined(b)) {
        return 0
      }

      const ds1 = a?.commonMetadata?.numValues || 0
      const ds2 = b?.commonMetadata?.numValues || 0
      return (ds1 === ds2 ? 0 : ds1 > ds2 ? 1 : -1) * multiplier
    }
  }

  static compareByLoadedOn<T extends ReorderCommonMetadata>(
    order: Order = DEFAULT_ORDER
  ): (a: T, b: T) => number {
    const multiplier = ReorderMetadataComparators.orderToMultiplier(order)
    return (a: T, b: T) => {
      if (isUndefined(a) && isUndefined(b)) {
        return 0
      }

      const ds1 = a?.commonMetadata?.loadedOn || 0
      const ds2 = b?.commonMetadata?.loadedOn || 0
      return (ds1 === ds2 ? 0 : ds1 > ds2 ? 1 : -1) * multiplier
    }
  }

  static compareByDatasetTitleMatch<T extends ReorderCommonMetadata>(
    match = '',
    order: Order = DEFAULT_ORDER
  ): (a: T, b: T) => number {
    const multiplier = ReorderMetadataComparators.orderToMultiplier(order)
    return (a: T, b: T) => {
      if (isUndefined(a) && isUndefined(b)) {
        return 0
      }

      const ds1 = (a?.commonMetadata?.datasetName || '').toLowerCase()
      const ds2 = (b?.commonMetadata?.datasetName || '').toLowerCase()
      // we want title match to always be case insensitive
      const index1 = ds1.indexOf(match)
      const index2 = ds2.indexOf(match)
      return (index2 - index1) * multiplier
    }
  }

  // static compareByDatasetTitleMatchDemoteHits<T extends ReorderCommonMetadata>(
  //   match: string = '',
  //   order: Order = DEFAULT_ORDER
  // ): (a: T, b: T) => number {
  //   const reorderByDatasetTitleMatch = ReorderMetadataComparators.compareByDatasetTitleMatch(
  //     match
  //   )
  //   const demote = ReorderMetadataComparators.demote(order)
  //   return (a: T, b: T) => {
  //     if (isUndefined(a) && isUndefined(b)) {
  //       return 0
  //     }
  //     if (a?.numHits > 0) {
  //       return demote
  //     }
  //     return compareByDatasetTitleMatch(a, b)
  //   }
  // }

  static compareByHits<T extends ReorderCommonMetadata>(
    order: Order = DEFAULT_ORDER
  ): (a: T, b: T) => number {
    const multiplier = ReorderMetadataComparators.orderToMultiplier(order)
    return (a: T, b: T) => {
      if (isUndefined(a) && isUndefined(b)) {
        return 0
      }

      const ds1 = a?.numHits || 0
      const ds2 = b?.numHits || 0
      return (ds1 - ds2) * multiplier
    }
  }

  static orderToMultiplier(order: Order = DEFAULT_ORDER): number {
    if (order === 'desc') {
      return ReorderMetadataComparators.DESC_MULTIPLIER
    }
    return ReorderMetadataComparators.ASC_MULTIPLIER
  }

  // static demote(order: Order = DEFAULT_ORDER): number {
  //   if (order === 'desc') {
  //     return  Number.MIN_VALUE
  //   }
  //   return  Number.MAX_VALUE
  // }
}
