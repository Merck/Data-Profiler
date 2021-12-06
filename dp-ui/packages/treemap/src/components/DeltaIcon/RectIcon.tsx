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
import React, { Fragment } from 'react'

interface RectIconProps {
  color: string
  opacity: string
  strokeWidth: number
  x: number
  y: number
  withHorizontalLine?: boolean
  withVerticalLine?: boolean
  withCircle?: boolean
}

export function RectIcon(props: RectIconProps): JSX.Element {
  const { color, opacity, strokeWidth, x, y } = props
  const withHorizontalLine = props?.withHorizontalLine || false
  const withVerticalLine = props?.withVerticalLine || false
  const withCircle = props?.withCircle || false
  const width = 10
  const height = 10
  const midHeight = height / 2
  const midWidth = width / 2
  return (
    <Fragment>
      <rect
        x={x}
        y={y}
        width={width}
        height={height}
        stroke={color}
        strokeWidth={strokeWidth}
        fill="none"
        strokeOpacity={opacity}
        fillOpacity={opacity}></rect>
      {withHorizontalLine && (
        <line
          x1={x}
          y1={y + midHeight}
          x2={x + width}
          y2={y + midHeight}
          style={{ stroke: color, strokeWidth, opacity }}
        />
      )}
      {withVerticalLine && (
        <line
          x1={x + midWidth}
          y1={y}
          x2={x + midWidth}
          y2={y + height}
          style={{ stroke: color, strokeWidth, opacity }}
        />
      )}
      {withCircle && (
        <circle
          cx={x + midWidth}
          cy={y + midHeight}
          r={(midWidth + midHeight) / 8}
          stroke={color}
          strokeWidth={strokeWidth}
          fill="none"
          strokeOpacity={opacity}
          fillOpacity={opacity}></circle>
      )}
    </Fragment>
  )
}
