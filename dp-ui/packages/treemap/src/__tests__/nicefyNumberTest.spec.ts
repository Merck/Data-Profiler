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
// import { nicefyNumber } from '../nicefyNumber'

import { ok } from 'assert'

/**
 * TODO: remove ignore tests after we fix the lerna import and jest issue
 * It is a compile time error
 *     SyntaxError: Unexpected token 'export'
 *
 *  > 1 | import format from '@dp-ui/lib/dist/helpers/formatter'
 *        | ^
 */
/**
describe.skip('given a number and nicefyNumber', () => {
  const nums = {
    smallNum: 12,
    mediumNum: 1000,
    largeNum: 5200000,
  }

  test('then expect to nicefy small numbers', () => {
    expect(nums.smallNum).toBeTruthy()
    const niceNum = nicefyNumber(nums.smallNum)
    expect(niceNum).not.toBeUndefined()
    expect(niceNum).toBe('12')
  })

  test('then expect to nicefy medium numbers', () => {
    expect(nums.mediumNum).toBeTruthy()
    const niceNum = nicefyNumber(nums.mediumNum)
    expect(niceNum).not.toBeUndefined()
    expect(niceNum).toBe('1K')
  })

  test('then expect to nicefy large numbers', () => {
    expect(nums.largeNum).toBeTruthy()
    const niceNum = nicefyNumber(nums.largeNum)
    expect(niceNum).not.toBeUndefined()
    expect(niceNum).toBe('5.2M')
  })
})
*/

describe.skip('placeholder', () => {
  test('placeholder', () => {
    expect(true).toBeTruthy()
  })
})
