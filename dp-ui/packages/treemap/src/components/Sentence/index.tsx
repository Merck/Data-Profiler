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
import { isNumber } from 'lodash'
import React from 'react'

// props from parent
export interface Props {
  classes?: Record<string, any>
  spacingPx?: number
  noBreak?: boolean
  showEllipsis?: boolean
}
interface State {}

/**
 * Simple Component to space a sentence of spans consistently and correctly
 */
class Sentence extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props)
  }

  render(): JSX.Element {
    const { classes } = this.props
    const noBreak = this.props.noBreak || false
    const showEllipsis = this.props.showEllipsis || false

    return (
      <span
        className={`${classes.sentence} ${noBreak ? classes.noBreak : ''} ${
          showEllipsis ? classes.showEllipsis : ''
        }`}>
        {this.props.children}
      </span>
    )
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = (theme: Theme) =>
  createStyles({
    root: {},
    sentence: {
      // display: 'flex',
      '& span': {
        // flex: '0 0 auto',
        marginRight: (props: Props) =>
          `${isNumber(props.spacingPx) ? props.spacingPx : 4}px`,
      },
    },
    noBreak: {
      '& span:first-child': {
        whiteSpace: 'nowrap',
      },
    },
    showEllipsis: {
      position: 'absolute',
      top: 0,
      left: 0,
      flex: '1 1 auto',
      whiteSpace: 'nowrap',
      maxWidth: '100%',
      overflow: 'hidden',
      textOverflow: 'ellipsis',
    },
  })

export default withStyles(styles)(Sentence)
