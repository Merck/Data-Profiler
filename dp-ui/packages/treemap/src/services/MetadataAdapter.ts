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
import { objectKeysToArray } from '../helpers/ObjectHelper'
import CommonMetadata from '../models/CommonMetadata'
import MetadataResponse from '../models/MetadataResponse'

// The metadata query dropping properties used for graphql
export const metadataGraphQLQuery = {
  query:
    '{metadata { num_tables dataset_name dataset_display_name table_name column_name data_type num_columns num_values num_unique_values load_time update_time visibility version_id }}',
}

// originally expecting: {<dataset name>: {dataset stuff}} [/v1]
// now needs to process {'metadata':[{dataset stuff}]} [/v2]
export default class MetadataAdapter {
  datasetMetadataResponseToCommonFormat(
    metaData: Readonly<Record<string, Array<MetadataResponse>>>
  ): Array<CommonMetadata> {
    const metadataArray = metaData.metadata // [/v2]
    //const metadataArray = objectKeysToArray<MetadataResponse>(metaData) [/v1]
    return this.datasetMetadataToCommonFormat(metadataArray)
  }

  datasetMetadataToCommonFormat(
    metaData: Readonly<Array<MetadataResponse>>
  ): Array<CommonMetadata> {
    return this.convertAll(metaData)
  }

  tableMetadataResponseToCommonFormat(
    metadata: Readonly<Record<string, MetadataResponse>>
  ): Array<CommonMetadata> {
    const metadataArray = objectKeysToArray<MetadataResponse>(metadata)
    return this.tableMetadataToCommonFormat(metadataArray)
  }

  tableMetadataToCommonFormat(
    metadata: Readonly<Array<MetadataResponse>>
  ): Array<CommonMetadata> {
    return this.convertAll(metadata)
  }

  columnMetadataResponseToCommonFormat(
    metadata: Readonly<Record<string, MetadataResponse>>
  ): Array<CommonMetadata> {
    const metadataArray = objectKeysToArray<MetadataResponse>(metadata)
    return this.columnMetadataToCommonFormat(metadataArray)
  }

  columnMetadataToCommonFormat(
    metadata: Readonly<Array<MetadataResponse>>
  ): Array<CommonMetadata> {
    return this.convertAll(metadata)
  }

  convertAll(
    metadata: Readonly<Array<MetadataResponse>>
  ): Array<CommonMetadata> {
    if (!metadata || !(metadata.length > 0)) {
      return []
    }
    return metadata.map((el) => this.convert(el))
  }

  convert(el: MetadataResponse): CommonMetadata {
    const datasetName = el.dataset_name || ''
    const datasetDisplayName = el.dataset_display_name || ''
    const tableName = el.table_name || ''
    const columnName = el.column_name || ''
    const dataType = el.data_type || ''
    const numColumns = Number(el.num_columns) // make sure is number, v2 returns string
    const numValues = Number(el.num_values) // make sure is number, v2 returns string
    const numRows = el.num_rows || Math.floor(numValues / numColumns) || 0
    const numUniqueValues = Number(el.num_unique_values) || 0
    // the table view will not have number of tables
    const numTables = Number(el.num_tables) || 1
    const loadedOn = Number(el.load_time) // make sure is number, v2 returns string
    const updatedOn = Number(el.update_time) // make sure is number, v2 returns string
    const visibility = el.visibility
    const versionId = el.version_id
    const properties = el.properties || {}
    const commonMetadata = {
      datasetName,
      datasetDisplayName,
      tableName,
      columnName,
      dataType,
      numTables,
      numColumns,
      numRows,
      numValues,
      numUniqueValues,
      loadedOn,
      updatedOn,
      visibility,
      versionId,
      properties,
    }
    return commonMetadata
  }
}
