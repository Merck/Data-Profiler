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
import { HierarchyNode, scalePow } from 'd3'
import { has, isUndefined, orderBy } from 'lodash'
import TreemapObject from '../models/TreemapObject'
import { ORDER_BY_ENUM } from '../orderby/models/OrderBy'
import { formatNumberWithHumanSuffix } from '../helpers/NumberHelper'

type Order = 'asc' | 'desc'

export interface TreemapNode {
  children: TreemapNode[]
  parent: TreemapNode
  data: TreemapObject
  height: number
  depth: number
  value: number
}

/**
 *
 * Various sort function generators for Treemap Data classes
 */
export default class ReorderTreemapComparators {
  static ASC_MULTIPLIER = 1
  static DESC_MULTIPLIER = -1
  static DEFAULT_ORDER: Order = 'desc'
  static DEFAULT_CASE_SENSITIVITY = true

  generateTreemapComparator(
    orderBy: ORDER_BY_ENUM = ORDER_BY_ENUM.VALUE_DESC,
    caseSensitive: boolean = ReorderTreemapComparators.DEFAULT_CASE_SENSITIVITY
  ): <T extends HierarchyNode<TreemapObject>>(a: T, b: T) => number {
    switch (orderBy) {
      case ORDER_BY_ENUM.TITLE_ASC:
        return this.compareByTitle('asc', caseSensitive)
      case ORDER_BY_ENUM.TITLE_DESC:
        return this.compareByTitle('desc', caseSensitive)
      case ORDER_BY_ENUM.VALUE_ASC:
        return this.compareByLogScaleValue()
      // return this.compareBySize('asc')
      // return this.compareByValue('asc')
      case ORDER_BY_ENUM.VALUE_DESC:
        return this.compareByLogScaleValue()
      // return this.compareBySize('desc')
      // return this.compareByValue('desc')
    }
  }

  compareByTitle<T extends HierarchyNode<TreemapObject>>(
    order: Order = ReorderTreemapComparators.DEFAULT_ORDER,
    caseSensitive: boolean = ReorderTreemapComparators.DEFAULT_CASE_SENSITIVITY
  ): (a: T, b: T) => number {
    const multiplier = this.orderToMultiplier(order)
    return (a: T, b: T) => {
      if (isUndefined(a) && isUndefined(b)) {
        return 0
      }

      let title1 = a?.data?.name || ''
      let title2 = b?.data?.name || ''
      if (caseSensitive === false) {
        title1 = title1.toLowerCase()
        title2 = title2.toLowerCase()
      }
      return (title1 === title2 ? 0 : title1 > title2 ? 1 : -1) * multiplier
    }
  }

  compareByValue<T extends HierarchyNode<TreemapObject>>(
    order: Order = ReorderTreemapComparators.DEFAULT_ORDER
  ): (a: T, b: T) => number {
    const multiplier = this.orderToMultiplier(order)
    return (a: T, b: T) => {
      if (isUndefined(a) && isUndefined(b)) {
        return 0
      }

      // return (b.height - a.height || b.value - a.value) * multiplier
      return (b.value - a.value) * multiplier
    }
  }

  compareBySize<T extends HierarchyNode<TreemapObject>>(
    order: Order = ReorderTreemapComparators.DEFAULT_ORDER
  ): (a: T, b: T) => number {
    const multiplier = this.orderToMultiplier(order)
    return (a: T, b: T) => {
      if (isUndefined(a) && isUndefined(b)) {
        return 0
      }

      const val1 = a?.data?.size || 0
      const val2 = b?.data?.size || 0
      return (val2 - val1) * multiplier
    }
  }

  compareByLogScaleValue<T extends HierarchyNode<TreemapObject>>(): (
    a: T,
    b: T
  ) => number {
    return (a: T, b: T) => {
      if (isUndefined(a) && isUndefined(b)) {
        return 0
      }

      const val1 = a?.data?.sizeBucket || 0.1
      const val2 = b?.data?.sizeBucket || 0.1
      return val2 - val1
    }
  }

  convertToLogScale(
    treemap: TreemapObject
    // buckets: number = 10
  ): TreemapObject {
    // cleanup any api errors
    // console.log("converting datasets to log scale...")
    const filtered = [...treemap?.children].filter((el) => has(el, 'children'))
    const sorted = orderBy(filtered, 'totalSize', 'desc')
    const minSize = sorted[sorted.length - 1].totalSize || 0
    const maxSize = sorted[0].totalSize || Number.POSITIVE_INFINITY
    // console.log("scale is [" + formatNumberWithHumanSuffix(minSize) + ", " + formatNumberWithHumanSuffix(maxSize) + "]")
    // const rootLevelLogScale = scalePow().exponent(1).domain([minSize, maxSize])
    const childLargeLogScale = scalePow()
      .exponent(0.5)
      .domain([minSize, maxSize])
    const scaled = sorted.reduce((acc, val) => {
      const sortedChildren = orderBy(val?.children, 'size', 'desc')
      // const childLargeLogScale = scalePow()
      //   .exponent(.45)
      //   .domain([
      //     sortedChildren[sortedChildren.length - 1]?.size,
      //     sortedChildren[0]?.size,
      //   ])
      // const childLargeLogScale = scalePow()
      //   .exponent(1.5)
      //   .domain([minSize, maxSize])
      const children = sortedChildren?.map((child) => {
        const childSizeScaled = childLargeLogScale(child?.size)
        // const childSizeBucket = this.bucket(childSizeScaled, buckets) || 0.1
        return Object.assign(child, { size: Math.abs(childSizeScaled) })
      })
      // const sizeWithScale = rootLevelLogScale(val?.totalSize)
      // let sizeBucket = this.bucket(sizeWithScale, buckets)
      // the scale is binned from [1,.10]
      // if descending flip the bin
      // if (order === 'asc') {
      //   sizeBucket = Math.abs(sizeBucket - 1)
      //   console.log('reversing bucket value ', val?.name, sizeBucket)
      // }
      const obj = Object.assign(val, {
        children: [...children],
        // size: sizeWithScale,
        // size: val.totalSize,
        // size: maxSize * sizeWithScale,
        // size: val.size,
        // size: sizeBucket,
        // sizeBucket: sizeBucket || 0.1,
      })
      return [...acc, { ...obj }]
    }, [])
    const treeObj = { children: [...scaled] }
    return treeObj
  }

  convertTablesToLogScale(
    treemap: TreemapObject
    // buckets: number = 10
  ): TreemapObject {
    // cleanup any api errors
    // console.log("converting to table or columns log scale...")
    const filtered = [...treemap?.children].filter((el) => has(el, 'children'))
    const sorted = orderBy(filtered, 'size', 'desc')
    const minSize = sorted[sorted.length - 1].size || 0
    const maxSize = sorted[0].size || Number.POSITIVE_INFINITY
    // console.log("scale is [" + formatNumberWithHumanSuffix(minSize) + ", " + formatNumberWithHumanSuffix(maxSize) + "]")
    const rootLevelLogScale = scalePow()
      .exponent(0.15)
      .domain([minSize, maxSize])
    const scaled = sorted.reduce((acc, val) => {
      const sizeWithScale = rootLevelLogScale(val?.size)
      const obj = Object.assign(val, {
        size: sizeWithScale,
        // size: val.totalSize,
        // size: maxSize * sizeWithScale,
        // size: val.size,
        // size: sizeBucket,
        // sizeBucket: sizeBucket || 0.1,
      })
      return [...acc, { ...obj }]
    }, [])
    const treeObj = { children: [...scaled] }
    return treeObj
  }

  convertColumnsToLogScale(treemap: TreemapObject): TreemapObject {
    // cleanup any api errors
    const filtered = [...treemap?.children].filter((el) => has(el, 'children'))
    const sorted = orderBy(filtered, 'size', 'desc')
    const scaled = sorted.reduce((acc, val) => {
      const obj = Object.assign(val, {
        size: 1,
      })
      return [...acc, { ...obj }]
    }, [])
    const treeObj = { children: [...scaled] }
    return treeObj
  }

  bucket(num = 0, buckets = 10): number {
    const bucket = Math.ceil(num * buckets) / buckets
    return Number(bucket.toFixed(2))
  }

  orderToMultiplier(
    order: Order = ReorderTreemapComparators.DEFAULT_ORDER
  ): number {
    if (order === 'desc') {
      return ReorderTreemapComparators.DESC_MULTIPLIER
    }
    return ReorderTreemapComparators.ASC_MULTIPLIER
  }
}
