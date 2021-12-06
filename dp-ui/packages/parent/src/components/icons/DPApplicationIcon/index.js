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

const DPApplicationIcon = (props) => {
  const { fill, width, height } = props
  return (
    <React.Fragment>
      <svg
        width={`${width ? width : SIDEBAR_ICON_WIDTH}`}
        height={`${height ? height : SIDEBAR_ICON_HEIGHT}`}
        viewBox="0 0 18 19"
        version="1.1">
        <g
          transform="translate(-27.000000, -133.000000)"
          fill={`${fill ? fill : SIDEBAR_ICON_FILL}`}
          fillRule="nonzero">
          <g>
            <path d="M27,143.566384 L35,143.566384 L35,133.665254 L27,133.665254 L27,143.566384 Z M27,151.487288 L35,151.487288 L35,145.54661 L27,145.54661 L27,151.487288 Z M37,151.487288 L45,151.487288 L45,141.586158 L37,141.586158 L37,151.487288 Z M37,133.665254 L37,139.605932 L45,139.605932 L45,133.665254 L37,133.665254 Z"></path>
          </g>
        </g>
      </svg>
    </React.Fragment>
  )
}

export default DPApplicationIcon
