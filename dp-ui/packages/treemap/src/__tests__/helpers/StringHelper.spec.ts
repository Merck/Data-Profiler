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
import { truncateTo, upperFirstLetter } from '../../helpers/StringHelper'

describe('given an string helper', () => {
  let str1
  let str2

  beforeEach(() => {
    str1 = 'abc'
    str2 = 'test string'
  })

  it('then expect sane test data', () => {
    expect(str1).toBeTruthy()
    expect(str2).toBeTruthy()
  })

  it('then expect a string helper upperFirstLetter to accept null or undefined', () => {
    let s1 = upperFirstLetter(undefined)
    expect(s1).toEqual('')
    expect(s1.length).toEqual(0)

    s1 = upperFirstLetter(null)
    expect(s1).toEqual('')
    expect(s1.length).toEqual(0)
  })

  it('then expect a string helper upperFirstLetter to be algorithmically awesome', () => {
    const s1 = upperFirstLetter(str1)
    expect(s1).toEqual('Abc')
    expect(s1.length).toEqual(str1.length)
  })

  it('then expect a string helper truncateTo to accept null or undefined', () => {
    let s1 = truncateTo(undefined)
    expect(s1).toEqual('')
    expect(s1.length).toEqual(0)

    s1 = truncateTo(null)
    expect(s1).toEqual('')
    expect(s1.length).toEqual(0)
  })

  it('then expect a string helper truncateTo to be algorithmically awesome for short strings', () => {
    const s1 = truncateTo(str1)
    expect(s1).toEqual('abc')
    expect(s1.length).toEqual(str1.length)
  })

  it('then expect a string helper truncateTo to be algorithmically awesome for short strings', () => {
    const maxLen = 3
    const s1 = truncateTo(str1, maxLen, '.')
    expect(s1).toEqual('abc')
    expect(s1.length).toEqual(str1.length)
  })

  it('then expect a string helper truncateTo to be algorithmically awesome', () => {
    const maxLen = 5
    const s2 = truncateTo(str2, maxLen, '.')
    expect(s2).toEqual('te...')
    expect(s2.length).toEqual(maxLen)
  })

  it('then expect a string helper truncateTo to be algorithmically awesome', () => {
    const maxLen = 5
    const s2 = truncateTo(str2, maxLen, 'x')
    expect(s2).toEqual('texxx')
    expect(s2.length).toEqual(maxLen)
  })
})
