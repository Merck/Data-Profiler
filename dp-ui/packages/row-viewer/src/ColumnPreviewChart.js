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
import { LineChart, Line, BarChart, Bar } from 'recharts'

const ColumnPreviewChart = (props) => {
  if (!props.samples || props.samples.length === 0) return null

  const color = '#ddd'
  const chartProps = { width: 50, height: 20 }

  const lineChart = (
    <LineChart {...chartProps} data={props.samples}>
      <Line
        isAnimationActive={false}
        dataKey="count"
        dot={false}
        stroke={color}
        strokeWidth={2}
      />
    </LineChart>
  )

  const barChart = (
    <BarChart {...chartProps} data={props.samples}>
      <Bar dataKey="count" isAnimationActive={false} fill={color} />
    </BarChart>
  )

  return <div>{props.samples.length > 20 ? lineChart : barChart}</div>
}

export default ColumnPreviewChart
