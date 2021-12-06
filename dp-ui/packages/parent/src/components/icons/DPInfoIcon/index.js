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

const DPInfoIcon = (props) => {
  const { fill, width, height } = props
  return (
    <React.Fragment>
      <svg
        width={`${width ? width : SIDEBAR_ICON_WIDTH}`}
        height={`${height ? height : SIDEBAR_ICON_HEIGHT}`}
        viewBox="0 0 18 19"
        version="1.1">
        <g
          transform="translate(-29.000000, -180.000000)"
          fill={`${fill ? fill : SIDEBAR_ICON_FILL}`}
          fillRule="nonzero">
          <g id="search">
            <path d="M37.9999785,198.238745 C42.8148627,198.238745 46.7187078,194.336308 46.7187078,189.520016 C46.7187078,184.70654 42.8148627,180.801287 37.9999785,180.801287 C33.1850944,180.801287 29.2812493,184.70654 29.2812493,189.520016 C29.2812493,194.336308 33.1850944,198.238745 37.9999785,198.238745 Z M37.9999785,187.621583 C37.1844859,187.621583 36.5234196,186.960517 36.5234196,186.145024 C36.5234196,185.329532 37.1844859,184.668465 37.9999785,184.668465 C38.8154711,184.668465 39.4765375,185.329532 39.4765375,186.145024 C39.4765375,186.960517 38.8154711,187.621583 37.9999785,187.621583 Z M39.5468499,194.020005 L36.4531072,194.020005 C36.220129,194.020005 36.0312332,193.83111 36.0312332,193.598131 L36.0312332,192.754383 C36.0312332,192.521405 36.220129,192.332509 36.4531072,192.332509 L36.8749812,192.332509 L36.8749812,190.082515 L36.4531072,190.082515 C36.220129,190.082515 36.0312332,189.893619 36.0312332,189.660641 L36.0312332,188.816893 C36.0312332,188.583915 36.220129,188.395019 36.4531072,188.395019 L38.7031019,188.395019 C38.9360801,188.395019 39.1249759,188.583915 39.1249759,188.816893 L39.1249759,192.332509 L39.5468499,192.332509 C39.7798281,192.332509 39.9687238,192.521405 39.9687238,192.754383 L39.9687238,193.598131 C39.9687238,193.83111 39.7798281,194.020005 39.5468499,194.020005 Z"></path>
          </g>
        </g>
      </svg>
    </React.Fragment>
  )
}

export default DPInfoIcon
