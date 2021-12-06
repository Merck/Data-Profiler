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
/**
 * metadata for snackbar is build in two ways
 *  depending on dataset or table view
 * this is a common format for the two views
 * dataset name, table name, table count, column count, row count, loaded on
 */
export default interface CommonMetadata {
  datasetName: string
  datasetDisplayName?: string
  tableName?: string
  columnName?: string
  dataType?: string
  numTables: number
  numColumns: number
  numRows: number
  numUniqueValues?: number
  numValues: number
  loadedOn?: number
  updatedOn?: number
  visibility?: string
  versionId?: string
  properties?: Record<string, any>
}
