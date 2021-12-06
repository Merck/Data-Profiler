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
import { isEmpty, uniq } from 'lodash'
import CommonMetadata from '../models/CommonMetadata'

/**
 * Operations and helper methods for CommonMetadata
 * Mostly doing aggregation and summing
 */
export default class MetadataService {
  buildViz = (metadata: Readonly<Array<CommonMetadata>>): string => {
    const vizSet = new Set(metadata.map((el) => el.visibility))
    return Array.from(vizSet).join('|')
  }

  /**
   * Warning: Note this method assumes the tableName is not empty
   *  and assumes at least on dataset to filter on
   * These assumptions do not work in some cases, like when
   * applying against the all datasets endpoint
   *
   * @see `sumTables`
   * @param metadata
   * @param dataset
   */
  uniqTables(
    metadata: Readonly<Array<CommonMetadata>>,
    dataset?: string
  ): string[] {
    if (!metadata) {
      return []
    }

    const arr = this.filter(metadata, dataset)
    // TODO: we should probably qualify the table name by its dataset
    return uniq(arr.map((value) => value.tableName).sort())
  }

  /**
   * @deprecated - maybe remove
   * @see `sumTables`
   * @param metadata
   * @param dataset
   */
  sumUniqTables(
    metadata: Readonly<Array<CommonMetadata>>,
    dataset?: string
  ): number {
    return this.uniqTables(metadata, dataset).length || 0
  }

  filter(
    metadata: Readonly<Array<CommonMetadata>>,
    dataset = ''
  ): Array<CommonMetadata> {
    if (!metadata) {
      return []
    }

    let arr = metadata as Array<CommonMetadata>
    if (!isEmpty(dataset.trim())) {
      const filter = dataset.trim().toLowerCase()
      arr = metadata.filter((el) => el.datasetName.toLowerCase() === filter)
    }
    return arr
  }

  sumBy(
    metadata: Readonly<Array<CommonMetadata>>,
    field: Readonly<string>
  ): number {
    if (!metadata || isEmpty(metadata) || !field || isEmpty(field)) {
      return 0
    }
    return metadata.reduce((memo, curValue) => memo + curValue[field] || 0, 0)
  }

  sumTables(
    metadata: Readonly<Array<CommonMetadata>>,
    dataset?: string
  ): number {
    const arr = this.filter(metadata, dataset)
    return this.sumBy(arr, 'numTables')
  }

  sumColumns(
    metadata: Readonly<Array<CommonMetadata>>,
    dataset?: string
  ): number {
    const arr = this.filter(metadata, dataset)
    return this.sumBy(arr, 'numColumns')
  }

  sumRows(metadata: Readonly<Array<CommonMetadata>>, dataset?: string): number {
    const arr = this.filter(metadata, dataset)
    return this.sumBy(arr, 'numRows')
  }

  sumValues(
    metadata: Readonly<Array<CommonMetadata>>,
    dataset?: string
  ): number {
    const arr = this.filter(metadata, dataset)
    return this.sumBy(arr, 'numValues')
  }

  sumUniqValues(
    metadata: Readonly<Array<CommonMetadata>>,
    dataset?: string
  ): number {
    const arr = this.filter(metadata, dataset)
    return this.sumBy(arr, 'numUniqueValues')
  }

  aggregateMetadata(
    metadata: Readonly<Array<CommonMetadata>>,
    dataset?: string
  ): CommonMetadata {
    const arr = this.filter(metadata, dataset)
    return {
      datasetName: dataset,
      numTables: this.sumTables(arr, dataset),
      numColumns: this.sumColumns(arr, dataset),
      numRows: this.sumRows(arr, dataset),
      numUniqueValues: this.sumUniqValues(arr, dataset),
      numValues: this.sumValues(arr, dataset),
    }
  }
}
