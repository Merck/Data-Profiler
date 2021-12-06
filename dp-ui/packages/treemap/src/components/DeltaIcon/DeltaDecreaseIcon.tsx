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
import { createStyles, Theme, withStyles } from '@material-ui/core'
import React, { Fragment } from 'react'
import { DatasetDeltaEnum } from '../../models/delta/DatasetDeltaEnum'
import { RectIcon } from './RectIcon'

// props from parent
export interface Props {
  color: string
  opacity: string
  strokeWidth: number
  deltaEnum: DatasetDeltaEnum
}
interface State {}

export class DeltaDecreaseIcon extends React.PureComponent<Props, State> {
  leadingCoords = {
    x: 2,
    y: 15,
  }

  followingCoords = {
    x: 55,
    y: 27,
  }

  constructor(props: Props) {
    super(props)
  }

  buildIcon(isLeadingIcon = false): JSX.Element {
    const { deltaEnum } = this.props
    switch (DatasetDeltaEnum[deltaEnum] as DatasetDeltaEnum) {
      case DatasetDeltaEnum.COLUMN_REMOVED:
        return this.columnRemoved(isLeadingIcon)
      case DatasetDeltaEnum.TABLE_REMOVED:
        return this.tableRemoved(isLeadingIcon)
      case DatasetDeltaEnum.TABLE_VALUES_DECREASED:
      case DatasetDeltaEnum.COLUMN_VALUES_DECREASED:
      case DatasetDeltaEnum.DATASET_VALUES_DECREASED:
        return this.rowRemoved(isLeadingIcon)
      default:
        return <Fragment />
    }
  }

  columnRemoved(isLeadingIcon = false): JSX.Element {
    const coords = isLeadingIcon ? this.leadingCoords : this.followingCoords
    const { x, y } = coords
    const properties = {
      ...this.props,
      x,
      y,
    }
    return <RectIcon {...properties} withVerticalLine={true} />
  }

  tableRemoved(isLeadingIcon = false): JSX.Element {
    const coords = isLeadingIcon ? this.leadingCoords : this.followingCoords
    const { x, y } = coords
    const properties = {
      ...this.props,
      x,
      y,
    }
    return <RectIcon {...properties} />
  }

  rowRemoved(isLeadingIcon = false): JSX.Element {
    const coords = isLeadingIcon ? this.leadingCoords : this.followingCoords
    const { x, y } = coords
    const properties = {
      ...this.props,
      x,
      y,
    }
    return <RectIcon {...properties} withHorizontalLine={true} />
  }

  valueDecreased(isLeadingIcon = false): JSX.Element {
    const coords = isLeadingIcon ? this.leadingCoords : this.followingCoords
    const { x, y } = coords
    const properties = {
      ...this.props,
      x,
      y,
    }
    return <RectIcon {...properties} withCircle={true} />
  }

  render() {
    const { color, opacity, strokeWidth } = this.props
    return (
      <svg
        version="1.1"
        baseProfile="full"
        width="68"
        height="48"
        xmlns="http://www/w3/org/2000/svg">
        {this.buildIcon(true)}
        <line
          x1="12"
          y1="20"
          x2="28"
          y2="20"
          style={{ stroke: color, strokeWidth, opacity }}
        />
        <line
          x1="28"
          y1="20"
          x2="38"
          y2="32"
          style={{ stroke: color, strokeWidth, opacity }}
        />
        <line
          x1="38"
          y1="32"
          x2="54"
          y2="32"
          style={{ stroke: color, strokeWidth, opacity }}
        />
        {this.buildIcon(false)}
      </svg>
    )
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = (theme: Theme) =>
  createStyles({
    root: {},
  })

export default withStyles(styles)(DeltaDecreaseIcon)
