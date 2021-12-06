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
import { createStyles, Theme, withStyles } from '@material-ui/core/styles'
import { isEmpty } from 'lodash'
import React, { Fragment } from 'react'
import { LineChart, Line, BarChart, Bar } from 'recharts'
import ColumnSample from '../../models/ColumnSample'

// props from parent
export interface Props {
  classes: Record<string, any>
  width?: number
  height?: number
  color?: string
  samples: Readonly<ColumnSample[]>
}
interface State {}

/**
 * Simple Component to space a sentence of spans consistently and correctly
 */
class ColumnPreviewChart extends React.PureComponent<Props, State> {
  static DEFAULT_HEIGHT = 20
  static DEFAULT_WIDTH = 50
  static DEFAULT_COLOR = '#BEBEBE'

  constructor(props: Props) {
    super(props)
  }

  render(): JSX.Element {
    const { samples } = this.props
    const width = this.props?.width || ColumnPreviewChart.DEFAULT_WIDTH
    const height = this.props?.height || ColumnPreviewChart.DEFAULT_HEIGHT
    const color = this.props?.color || ColumnPreviewChart.DEFAULT_COLOR

    if (!samples || isEmpty(samples)) {
      return <Fragment></Fragment>
    }

    const chartProps = { width, height }

    const lineChart = (
      <LineChart {...chartProps} data={samples}>
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
      <BarChart {...chartProps} data={samples}>
        <Bar dataKey="count" isAnimationActive={false} fill={color} />
      </BarChart>
    )

    return <div>{samples.length > 20 ? lineChart : barChart}</div>
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = (theme: Theme) =>
  createStyles({
    root: {},
  })

export default withStyles(styles)(ColumnPreviewChart)
