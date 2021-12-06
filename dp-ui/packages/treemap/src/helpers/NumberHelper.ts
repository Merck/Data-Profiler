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
import format from '@dp-ui/lib/dist/helpers/formatter'

export const safeCalculatePercent = (count: number, total: number): number => {
  if (total === 0) {
    return 0
  }
  return (count / total) * 100
}

export const ensureValidPercent = (percent = 0): number => {
  if (percent > 100) {
    return 100
  } else if (percent < 0) {
    return 0
  }
  return percent
}

export function formatNumberWithScientificSuffix(n: number): string {
  if (n === undefined) {
    return '0'
  }
  let x = `${n}`.length
  const p = Math.pow
  const d = p(10, 1)
  x -= x % 3
  return Math.round((n * d) / p(10, x)) / d + ' kMGTPE'[x / 3]
}

export function formatNumberWithHumanSuffix(num: number): string {
  return format.niceifyNumber(num)
}

export function formatNumberWithCommas(num: number): string {
  return format.commafyNumber(num)
}
