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
 * metadata as seen from the server RESTful endpoints
 */
export default interface MetadataResponse {
  dataset_name: string
  dataset_display_name?: string
  table_name?: string
  column_name?: string
  data_type?: string
  num_tables: number
  num_columns: number
  num_rows: number
  num_unique_values?: number
  num_values: number
  load_time?: number
  update_time?: number
  visibility?: string
  version_id: string
  properties?: Record<string, any>
}
