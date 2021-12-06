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
import { isUndefined } from 'lodash'
import React, { Fragment } from 'react'
import { DatasetDeltaEnum } from '../../models/delta/DatasetDeltaEnum'
import { DeltaDecreaseIcon } from './DeltaDecreaseIcon'
import { DeltaIncreaseIcon } from './DeltaIncreaseIcon'

// props from parent
export interface Props {
  deltaEnum: DatasetDeltaEnum
}
interface State {}

export class DeltaIcon extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props)
  }

  shouldIgnore(deltaEnum: Readonly<DatasetDeltaEnum>): boolean {
    if (!deltaEnum) {
      return false
    }
    return deltaEnum == DatasetDeltaEnum.NO_OP
  }

  deltaIsDecreasing(deltaEnum: Readonly<DatasetDeltaEnum>): boolean {
    const delta: string = DatasetDeltaEnum[deltaEnum]
    if (isUndefined(delta)) {
      return false
    }
    const lowerDelta = delta.toLowerCase()
    return (
      lowerDelta.indexOf('removed') > 0 || lowerDelta.indexOf('decreased') > 0
    )
  }

  deltaIsIncreasing(deltaEnum: Readonly<DatasetDeltaEnum>): boolean {
    const delta = DatasetDeltaEnum[deltaEnum]
    if (isUndefined(delta)) {
      return false
    }
    const lowerDelta = delta.toLowerCase()
    return (
      lowerDelta.indexOf('added') > 0 || lowerDelta.indexOf('increased') > 0
    )
  }

  render() {
    const { deltaEnum } = this.props
    const color = 'black'
    const opacity = '.65'
    const strokeWidth = 2
    const shouldIgnore = this.shouldIgnore(deltaEnum)
    if (shouldIgnore) {
      return <Fragment />
    }

    const isDecreasing = this.deltaIsDecreasing(deltaEnum)
    if (isDecreasing) {
      return (
        <DeltaDecreaseIcon
          strokeWidth={strokeWidth}
          opacity={opacity}
          color={color}
          deltaEnum={deltaEnum}
        />
      )
    }

    const isIncreasing = this.deltaIsIncreasing(deltaEnum)
    if (isIncreasing) {
      return (
        <DeltaIncreaseIcon
          strokeWidth={strokeWidth}
          opacity={opacity}
          color={color}
          deltaEnum={deltaEnum}
        />
      )
    }

    console.log(
      'WARN: unknown delta direction. Did you add a new delta type? skipping for now...'
    )
    return <Fragment />
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = (theme: Theme) =>
  createStyles({
    root: {},
  })

export default withStyles(styles)(DeltaIcon)
