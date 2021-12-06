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
import React from 'react'
import { SELECTION_GREEN } from '../../dpColors'
import { ensureValidPercent } from '../../helpers/NumberHelper'
import { DEFAULT_CELL_COLOR } from '../../searchHitColors'

// props from parent
export interface Props {
  classes: Record<string, any>
  percent?: number
}
interface State {
  percent?: number
}

/**
 * Simple Component to display a horizontal bar chart
 */
class Bar extends React.PureComponent<Props, State> {
  static DEFAULT = 0
  constructor(props: Props) {
    super(props)
    this.state = {
      percent: ensureValidPercent(this.props.percent),
    }
  }

  render(): JSX.Element {
    const { classes } = this.props
    const { percent } = this.state
    return (
      <div className={classes.bar}>
        <div className={classes.value} style={{ width: `${percent}%` }}></div>
      </div>
    )
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = (theme: Theme) =>
  createStyles({
    root: {},
    bar: {
      display: 'flex',
      background: DEFAULT_CELL_COLOR,
      width: '100%',
      height: '4px',
    },
    value: {
      background: SELECTION_GREEN,
    },
  })

export default withStyles(styles)(Bar)
