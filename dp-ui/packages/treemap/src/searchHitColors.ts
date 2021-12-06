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
import * as d3 from 'd3'

// old scale
/**
  '#66ffff',
  '#4dffff',
  '#33ffff',
  '#1affff',
  '#00ffff',
  '#00e6e6',
  '#00cccc',
  '#00b3b3',
  '#009999',
 */
// const DEFAULT_CELL_COLOR = '#c0c0c0'
const DEFAULT_CELL_COLOR = '#cbd7dd'
const TEAL_50 = '#99EDE6'
// not sure the exact name for these
// i extraced them with an eye dropper off the gradient gif
const TEAL_1 = '#8ee5df'
const TEAL_2 = '#76d5ce'
const TEAL_3 = '#60c6bd'
const TEAL_4 = '#43b3a9'
const TEAL_5 = '#2ba298'
const TEAL_6 = '#1e968a'
const TEAL_7 = '#1a887e'
const TEAL_400 = '#00857A'

const shadeLightToDark: Readonly<Array<string>> = [
  // default grey
  DEFAULT_CELL_COLOR,
  TEAL_50,
  TEAL_1,
  TEAL_2,
  TEAL_3,
  TEAL_4,
  TEAL_5,
  TEAL_6,
  TEAL_7,
  TEAL_400,
]

const searchFrequencyColorRange = d3
  .scaleThreshold<number, string>()
  .domain([1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000])
  .range(shadeLightToDark)

export {
  DEFAULT_CELL_COLOR,
  TEAL_50,
  TEAL_1,
  TEAL_2,
  TEAL_3,
  TEAL_4,
  TEAL_5,
  TEAL_6,
  TEAL_7,
  TEAL_400,
  shadeLightToDark,
  searchFrequencyColorRange,
}
