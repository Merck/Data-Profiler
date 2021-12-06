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
// import {
//   ensureValidPercent,
//   formatNumberWithHumanSuffix,
//   formatNumberWithScientificSuffix,
//   safeCalculatePercent,
// } from '../../helpers/NumberHelper'

/**
 * TODO: remove ignore tests after we fix the lerna import and jest issue
 * It is a compile time error
 *     SyntaxError: Unexpected token 'export'
 *
 *  > 1 | import format from '@dp-ui/lib/dist/helpers/formatter'
 *        | ^
 */
/**
 * 
 describe.skip('given a number helper', () => {
   const nums = {
     smallNum: 12,
     mediumNum: 1000,
     largeNum: 5200000,
     xlargeNum: 819199440000,
    }
    
    it('then expect to calculate a safe percentage', () => {
      const percentage = safeCalculatePercent(100, 100)
      expect(percentage).toEqual(100)
    })
    
    it('then expect to calculate a safe percentage', () => {
      const percentage = safeCalculatePercent(25, 100)
      expect(percentage).toEqual(25)
    })
    
    it('then expect to calculate a safe percentage and guard divide by 0', () => {
      const percentage = safeCalculatePercent(100, 0)
      expect(percentage).toEqual(0)
    })
    
    it('then expect to check percentage bounds upper', () => {
      const percentage = ensureValidPercent(200)
      expect(percentage).toEqual(100)
    })
    
    it('then expect to check percentage bounds lower', () => {
      const percentage = ensureValidPercent(-200)
      expect(percentage).toEqual(0)
    })
    
    it('then expect to check percentage bounds empty', () => {
      const percentage = ensureValidPercent()
      expect(percentage).toEqual(0)
    })
    
    it('then expect to check percentage bounds normal', () => {
      const percentage = ensureValidPercent(50.5)
      expect(percentage).toEqual(50.5)
    })
    
    test('then expect to formatNumberWithScientificSuffix small numbers', () => {
      expect(nums.smallNum).toBeTruthy()
      const niceNum = formatNumberWithScientificSuffix(nums.smallNum)
      expect(niceNum).not.toBeUndefined()
      expect(niceNum).toBe('12 ')
    })
    
    test('then expect to formatNumberWithScientificSuffix medium numbers', () => {
      expect(nums.mediumNum).toBeTruthy()
      const niceNum = formatNumberWithScientificSuffix(nums.mediumNum)
      expect(niceNum).not.toBeUndefined()
      expect(niceNum).toBe('1k')
    })
    
    test('then expect to formatNumberWithScientificSuffix large numbers', () => {
      expect(nums.largeNum).toBeTruthy()
      const niceNum = formatNumberWithScientificSuffix(nums.largeNum)
      expect(niceNum).not.toBeUndefined()
      expect(niceNum).toBe('5.2M')
    })
    
    test('then expect to formatNumberWithScientificSuffix xlarge numbers', () => {
      expect(nums.largeNum).toBeTruthy()
      const niceNum = formatNumberWithScientificSuffix(nums.xlargeNum)
      expect(niceNum).not.toBeUndefined()
      expect(niceNum).toBe('0.8T')
    })
    
    test('then expect to formatNumberWithHumanSuffix small numbers', () => {
      expect(nums.smallNum).toBeTruthy()
      const niceNum = formatNumberWithHumanSuffix(nums.smallNum)
      expect(niceNum).not.toBeUndefined()
      expect(niceNum).toBe('12')
    })
    
    test('then expect to formatNumberWithHumanSuffix medium numbers', () => {
      expect(nums.mediumNum).toBeTruthy()
      const niceNum = formatNumberWithHumanSuffix(nums.mediumNum)
      expect(niceNum).not.toBeUndefined()
      expect(niceNum).toBe('1K')
    })
    
    test('then expect to formatNumberWithHumanSuffix large numbers', () => {
      expect(nums.largeNum).toBeTruthy()
      const niceNum = formatNumberWithHumanSuffix(nums.largeNum)
      expect(niceNum).not.toBeUndefined()
      expect(niceNum).toBe('5.2M')
    })
    
    test('then expect to formatNumberWithHumanSuffix large numbers', () => {
      expect(nums.largeNum).toBeTruthy()
      const niceNum = formatNumberWithHumanSuffix(nums.xlargeNum)
      expect(niceNum).not.toBeUndefined()
      expect(niceNum).toBe('819.2B')
    })
  })
  */

describe.skip('placeholder', () => {
  test('placeholder', () => {
    expect(true).toBeTruthy()
  })
})
