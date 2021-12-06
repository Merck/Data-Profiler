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
import { isEmpty, isEqual, isNull, isUndefined } from 'lodash'

/**
 * convert response json from a single keyed element to an array
 *
 * @param obj
 * @param includeKey
 * @param mapKeyToField
 */
export const objectKeysToArray = <T>(
  obj?: Record<string, T>,
  includeKey = false,
  mapKeyToField = 'key'
): Array<T> => {
  if (isUndefined(obj) || isNull(obj)) {
    return []
  }
  return Object.keys(obj).reduce((acc, key) => {
    const cur = obj[key]
    if (includeKey) {
      cur[mapKeyToField] = key
    }
    return [...acc, obj[key]]
  }, [])
}

/**
 * convert response json from a single keyed element to an array
 * this method adds the key as an additional element if requested
 *
 * @param obj
 * @param includeKey
 * @param mapKeyToField
 */
export const objectKeysToFlatArray = <T>(
  obj?: Record<string, Array<T>>,
  includeKey = false,
  mapKeyToField = 'key'
): Array<T> => {
  if (isUndefined(obj) || isNull(obj)) {
    return []
  }
  return Object.keys(obj).reduce((acc, key) => {
    let arr = obj[key]
    if (includeKey) {
      arr = arr.map((el) => {
        el[mapKeyToField] = key
        return el
      })
    }
    return [...acc, ...arr]
  }, [])
}

/**
 * does an equality check when and iff both items defined
 * if one item is empty or undefined then pass the check as true
 *
 * @param a
 * @param b
 */
export const optionalFilterIfDefined = <T>(a?: T, b?: T) => {
  const isSet1 = !isUndefined(a) && !isEmpty(a)
  const isSet2 = !isUndefined(b) && !isEmpty(b)
  return isSet1 && isSet2 ? isEqual(a, b) : true
}
