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
import { RowResultsEndpointShape } from '../../services/RowResultsAdapter'

export default class RowsStub {
  /**
   * POST {{ base_url  }}/data/rows
   * "{
   *   "dataset": "dataset1",
   *   "table": "prod_with_tokens",
   *   "column": null,
   *   "filters": {},
   *   "limit": 3,
   *   "api_filter": false
   * }"
   */
  static rowsEndpointResponse(): RowResultsEndpointShape {
    return {
      rows: [],
      columns: {},
      sortedColumns: [
        'valid_tokens_1',
        'valid_tokens_2',
        'valid_tokens_3',
        'valid_tokens_4',
        'valid_tokens_5',
        'valid_tokens_6',
        'valid_tokens_7',
        'valid_tokens_8',
        'valid_tokens_9',
        'valid_tokens_10',
        'valid_tokens_11',
        'valid_tokens_12',
        'valid_tokens_13',
        'valid_tokens_14',
        'valid_tokens_15',
        'valid_tokens_16',
        'valid_tokens_17',
        'valid_tokens_18',
        'valid_tokens_19',
      ],
      endLocation: '',
      statistics: {},
      count: 3,
    }
  }
}
