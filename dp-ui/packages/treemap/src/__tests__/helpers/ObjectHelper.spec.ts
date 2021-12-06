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
import {
  objectKeysToArray,
  optionalFilterIfDefined,
} from '../../helpers/ObjectHelper'

describe('given an object helper', () => {
  let obj
  let obj1

  beforeEach(() => {
    obj = {
      key1: 'val1',
      key2: 'val2',
      key3: 'val3',
    }

    obj1 = {
      key1: {
        subkey1: 'k1.v1',
        subkey2: 'k1.v2',
      },
      key2: {
        subkey1: 'k2.v1',
        subkey2: 'k2.v2',
      },
    }
  })

  it('then expect sane test data', () => {
    expect(obj).toBeTruthy()
  })

  it('then expect a key to array helper to accept null or undefined', () => {
    let arr = objectKeysToArray(undefined)
    expect(arr).toBeTruthy()
    expect(arr).toEqual(expect.any(Array))
    expect(arr.length).toEqual(0)

    arr = objectKeysToArray(null)
    expect(arr).toBeTruthy()
    expect(arr).toEqual(expect.any(Array))
    expect(arr.length).toEqual(0)
  })

  it('then expect a key to array helper', () => {
    const arr = objectKeysToArray(obj)
    expect(arr).toBeTruthy()
    expect(arr).toEqual(expect.any(Array))
    expect(arr.length).toEqual(3)
    expect(arr[0]).toEqual('val1')
  })

  it('then expect a key to array helper to accept objects', () => {
    const arr = objectKeysToArray<any>(obj1)
    expect(arr).toBeTruthy()
    expect(arr).toEqual(expect.any(Array))
    expect(arr.length).toEqual(2)
    expect(arr[0]).toEqual(obj1.key1)
  })

  it('then expect an equals and set string helper', () => {
    const a = 'test'
    const b = 'test'
    const isEqualsAndSet = optionalFilterIfDefined(a, b)
    expect(isEqualsAndSet).toBeTruthy()
    expect(isEqualsAndSet).toEqual(true)
  })

  it('then expect an equals and set string helper', () => {
    const a = 'test'
    const b = ''
    // Note: b is unset so the test just returns true
    const isEqualsAndSet = optionalFilterIfDefined(a, b)
    expect(isEqualsAndSet).toBeTruthy()
    expect(isEqualsAndSet).toEqual(true)
  })

  it('then expect an equals and set string helper', () => {
    const a = ''
    const b = ''
    const isEqualsAndSet = optionalFilterIfDefined(a, b)
    expect(isEqualsAndSet).toBeTruthy()
    expect(isEqualsAndSet).toEqual(true)
  })

  it('then expect an equals and set object helper', () => {
    const a = {
      k1: 'k1',
      k2: 'k2',
    }
    const b = {
      k1: 'k1',
      k2: 'k2',
    }
    const isEqualsAndSet = optionalFilterIfDefined(a, b)
    expect(isEqualsAndSet).toBeTruthy()
    expect(isEqualsAndSet).toEqual(true)
  })
})
