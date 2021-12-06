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
import React from 'react'
import {
  SIDEBAR_ICON_WIDTH,
  SIDEBAR_ICON_HEIGHT,
  SIDEBAR_ICON_FILL,
} from '../icon_defaults'

const DPSearchIcon = (props) => {
  const { fill, width, height } = props
  return (
    <React.Fragment>
      <svg
        width={`${width ? width : SIDEBAR_ICON_WIDTH}`}
        height={`${height ? height : SIDEBAR_ICON_HEIGHT}`}
        viewBox="0 0 18 19"
        version="1.1">
        <g
          transform="translate(-26.000000, -82.000000)"
          fill={`${fill ? fill : SIDEBAR_ICON_FILL}`}
          fillRule="nonzero">
          <g>
            <path d="M35.140625,102.202455 C35.7135417,102.488914 36.2734375,102.488914 36.8203125,102.202455 L36.8203125,102.202455 L44.9453125,98.1399554 C45.6484375,97.8014137 46,97.2415179 46,96.4602679 L46,96.4602679 L46,87.6712054 C46,87.2805804 45.8893229,86.9290179 45.6679688,86.6165179 C45.4466146,86.3040179 45.1536458,86.0696429 44.7890625,85.9133929 L44.7890625,85.9133929 L36.6640625,82.8665179 C36.2213542,82.7102679 35.7786458,82.7102679 35.3359375,82.8665179 L35.3359375,82.8665179 L27.2109375,85.9133929 C26.8463542,86.0696429 26.5533854,86.3040179 26.3320312,86.6165179 C26.1106771,86.9290179 26,87.2805804 26,87.6712054 L26,87.6712054 L26,96.4602679 C26,97.2415179 26.3385417,97.8014137 27.015625,98.1399554 L27.015625,98.1399554 L35.140625,102.202455 Z M36,91.2258929 L28.5,88.1790179 L28.5,88.1008929 L36,85.2883929 L43.5,88.1008929 L43.5,88.1790179 L36,91.2258929 Z M37.25,99.1946429 L37.25,93.4133929 L43.5,90.8352679 L43.5,96.0696429 L37.25,99.1946429 Z"></path>
          </g>
        </g>
      </svg>
    </React.Fragment>
  )
}

export default DPSearchIcon
