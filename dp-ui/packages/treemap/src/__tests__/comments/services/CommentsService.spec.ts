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
import { Comment } from '../../../comments/models/Comment'
import CommentsService from '../../../comments/services/CommentsService'
import CommentsStub from '../../mocks/Comments.stub'

describe('given a comments service', () => {
  let service: CommentsService
  let data: Readonly<Comment[]>

  beforeEach(() => {
    service = new CommentsService()
    data = CommentsStub.genSampleHierarchyEndpoint()
  })

  test('then expect sane test data', () => {
    expect(data).toBeTruthy()
  })

  test('then expect a service', () => {
    expect(service).toBeTruthy()
  })

  test('then expect to modify dates formats', () => {
    const arr = service.sortCommentsDTG(data)
    expect(arr).toBeTruthy()
    expect(arr.length).toBe(7)
    const comment = arr[0]
    expect(comment).toBeTruthy()
    expect(comment.dataset).toBe('d1')
    expect(comment.createdOn).toBe(1584390463)
  })

  test('then expect to modify dates formats', () => {
    const comment = data[0]
    const formatedDtg = service.formatCommentDTG(comment.createdOn)
    expect(formatedDtg).toBeTruthy()
    expect(formatedDtg).toBe('13 Aug 2020')
  })
})
