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
import { isArray, isEmpty, sortBy } from 'lodash'
import { utc } from 'moment'
import { Comment } from '../models/Comment'

export default class CommentsService {
  sortCommentsDTG(arr: Readonly<Comment[]>): Comment[] {
    if (isArray(arr) && !isEmpty(arr)) {
      return sortBy(arr, ['createdOn'])
    }
    return [...arr]
  }

  formatCommentDTG(epochSeconds = 1000): string {
    const dtg = utc(epochSeconds * 1000)
    return dtg.format('DD MMM YYYY')
  }
}
