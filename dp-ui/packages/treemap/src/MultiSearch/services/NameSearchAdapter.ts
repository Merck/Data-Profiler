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
import { groupBy, isEmpty, isUndefined } from 'lodash'
import md5 from 'md5'
import AggregatedSearchResult from '../models/AggregatedSearchResult'
import AggregratedSearchResultElement from '../models/AggregatedSearchResultElement'
import AggregatedSearchResultHelper from '../models/AggregatedSearchResultHelper'
import MetadataResult from '../models/MetadataResult'

/**
 * class to transform metadata from a dataset,table,column title search
 *  into an <code>AggregatedSearchResult</code>
 */
export default class NameSearchAdapter {
  // lower case on the front end if the backend is case insensitive
  // this lowercase normalize feature might be broken
  static SHOULD_NORMALIZE_VALUES = false

  convert(
    metadata: Readonly<Partial<MetadataResult>[]>,
    elementName: string,
    key: string
  ): AggregatedSearchResult {
    const elements = metadata.map((el) => this.convertElement(el, elementName))
    const id = md5(JSON.stringify(elements))
    const count = elements
      ?.map((el) => el.count)
      .reduce((prev, cur) => prev + cur, 0)
    return {
      id,
      value: this.ensureNormalizeValues(key),
      count,
      elements,
    }
  }

  convertAll(
    metadata: Readonly<Partial<MetadataResult>[]>,
    elementName?: string
  ): AggregatedSearchResult {
    const elements = metadata?.map((metadata) => {
      const mostSpecificKey = elementName || this.mostSpecificKey(metadata)
      return this.convertElement(metadata, mostSpecificKey)
    })
    // const id = uuidv4()
    const id = md5(JSON.stringify(elements))
    const value = metadata.length > 0 ? metadata[0][elementName] : ''
    return {
      id,
      value: this.ensureNormalizeValues(value),
      count: elements?.length || 0,
      elements,
    }
  }

  convertElement(
    metadata: Readonly<Partial<MetadataResult>>,
    elementName?: string
  ): AggregratedSearchResultElement {
    const value = metadata[elementName]
    const element = {
      value: this.ensureNormalizeValues(value),
      count: 1,
      dataset: this.ensureNormalizeValues(metadata?.dataset || ''),
      table: this.ensureNormalizeValues(metadata?.table || ''),
      tableId: metadata?.table || '',
      column: this.ensureNormalizeValues(metadata?.column || ''),
      isTitleResult: true,
      error: metadata?.error,
    }
    return element
  }

  groupAndConvertAll(
    metadata: Readonly<Partial<MetadataResult>[]>,
    groupingKey: string
  ): AggregatedSearchResult[] {
    if (
      isUndefined(metadata) ||
      isEmpty(metadata) ||
      isUndefined(groupingKey)
    ) {
      return []
    }
    const grouped = groupBy(metadata, groupingKey)
    const keys = Object.keys(grouped)
    const results = keys.map((key) => {
      const metadataArray = grouped[key]
      const elements = metadataArray.map((metadata) =>
        this.convertElement(metadata, groupingKey)
      )
      const id = md5(JSON.stringify(elements))
      const count = elements
        ?.map((el) => el.count)
        .reduce((prev, cur) => prev + cur, 0)
      return {
        id,
        value: key,
        count,
        elements,
      }
    })
    // const id = uuidv4()
    return results
  }

  groupAndConvert(
    metadata: Readonly<Partial<MetadataResult>[]>,
    elementName: string,
    filterKey: string
  ): AggregatedSearchResult {
    if (
      isUndefined(metadata) ||
      isEmpty(metadata) ||
      isUndefined(elementName)
    ) {
      return AggregatedSearchResultHelper.emptyResult()
    }
    const filter = filterKey ? filterKey.toLowerCase() : ''
    const grouped = groupBy(metadata, elementName)
    const key = Object.keys(grouped).find((key) => key.toLowerCase() === filter)
    const filteredMetadata = grouped[key]
    return this.convert(filteredMetadata, elementName, filterKey)
  }

  mostSpecificKey(metadata: Readonly<Partial<MetadataResult>>): string {
    if (isUndefined(metadata)) {
      return undefined
    }

    const { dataset, table, column } = metadata
    if (isEmpty(dataset)) {
      return undefined
    } else if (!isEmpty(dataset) && isEmpty(table)) {
      return 'dataset'
    } else if (!isEmpty(dataset) && !isEmpty(table)) {
      return 'table'
    } else if (!isEmpty(dataset) && !isEmpty(table) && !isEmpty(column)) {
      return 'column'
    } else {
      console.log('unsure what key to use for ' + metadata)
      return undefined
    }
  }

  ensureNormalizeValues(value?: string): string {
    if (!value || !NameSearchAdapter.SHOULD_NORMALIZE_VALUES) {
      return value
    }

    // our title searches are lower case, so display them as lower case
    return value.trim().toLowerCase()
  }
}
