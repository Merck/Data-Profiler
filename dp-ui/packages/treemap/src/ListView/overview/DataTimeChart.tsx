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
import { createStyles, withStyles } from '@material-ui/core/styles'
import {
  Tooltip,
  CartesianGrid,
  XAxis,
  YAxis,
  Area,
  AreaChart,
  ResponsiveContainer,
  Label,
} from 'recharts'
import { Alert } from '@material-ui/lab'

const formatTick = (data, tick) => {
  const index = data.length - data.findIndex((o) => o.day === tick) - 1
  return index
}

const DataTimeChart = (props) => (
  <div className={props.classes.root}>
    <strong>Data usage over time</strong>
    {props.data.length > 1 ? (
      <ResponsiveContainer height={130}>
        <AreaChart
          data={props.data}
          margin={{
            top: 5,
            right: 0,
            left: 0,
            bottom: 5,
          }}>
          {/* <CartesianGrid strokeDasharray="3 3" /> */}
          <XAxis
            dataKey="day"
            tick={{ fontSize: 10 }}
            tickFormatter={(tick) => formatTick(props.data, tick)}>
            <Label
              value="Weeks Ago"
              fontSize={10}
              offset={-1}
              position="insideBottom"
            />
          </XAxis>

          {/* <YAxis allowDecimals={false} /> */}
          <Area
            type="monotone"
            dataKey="views"
            stroke="#658eef"
            strokeWidth={4}
            fill="#b7c5fa"
            dot={false}
          />
          <Tooltip />
        </AreaChart>
      </ResponsiveContainer>
    ) : (
      <Alert severity="error">Error fetching chart data</Alert>
    )}
  </div>
)

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = createStyles((theme) => ({
  root: {
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'space-between',
    backgroundColor: 'white',
    padding: '20px',
    paddingBottom: '10px',
    borderRadius: '10px',
    width: '100%',
  },
}))

export default withStyles(styles)(DataTimeChart)
