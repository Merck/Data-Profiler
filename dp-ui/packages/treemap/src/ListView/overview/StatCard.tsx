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
import { createStyles, withStyles } from '@material-ui/core/styles'
import ArrowDropUpIcon from '@material-ui/icons/ArrowDropUp'
import { SSL_OP_SSLEAY_080_CLIENT_DH_BUG } from 'constants'
import { autoType } from 'd3'
import React from 'react'
import { ACTIVATED_COLOR } from '../../dpColors'

// props from parent
export interface OwnProps {
  classes: Record<string, any>
  value: string
  label: string
  description: string
  delta?: number
  placeholder?
  isTotal?
}
type Props = OwnProps

class StatCard extends React.PureComponent<Props> {
  constructor(props: Props) {
    super(props)
  }

  render(): JSX.Element {
    const { classes, value, label, description, delta, isTotal, placeholder } =
      this.props
    return (
      <div
        className={`${classes.root}${
          placeholder ? ` ${classes.placeholder}` : ''
        }`}>
        <span className={classes.stat}>
          {value}
          {delta > 0 && <ArrowDropUpIcon />}
        </span>
        <span className={classes.label}>
          {isTotal && <span className={classes.total}>TOTAL</span>}
          {label}
        </span>
        <span className={classes.description}>{description}</span>
      </div>
    )
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = createStyles((theme) => ({
  root: {
    background: '#eaebeb',
    borderRadius: '10px',
    padding: '18px 16px',
    textAlign: 'center',
    minWidth: 200,
    flex: 1,
    '& span': {
      margin: '8px 0',
    },
  },
  stat: {
    display: 'inline-block',
    position: 'relative',
    color: ACTIVATED_COLOR,
    fontSize: '50pt',
    lineHeight: '50pt',
    fontWeight: 500,
    letterSpacing: '1px !important',
    margin: '12px auto !important',
    '& svg': {
      position: 'absolute',
      left: '100%',
      top: '2px',
      fontSize: '44px',
      marginLeft: '5px',
      filter: 'brightness(0)',
      opacity: 0.2,
    },
  },
  label: {
    display: 'block',
    fontSize: '20pt',
    fontWeight: 600,
    letterSpacing: '1px !important',
    textTransform: 'uppercase',
    position: 'relative',
  },
  total: {
    position: 'absolute',
    top: '-24px',
    left: 0,
    right: 0,
    fontSize: '10pt',
    opacity: 0.3,
  },
  description: {
    display: 'block',
    fontSize: '14pt',
    opacity: 0.6,
  },
  placeholder: {
    position: 'relative',
    '&:before': {
      content: '"PLACEHOLDER"',
      display: 'block',
      position: 'absolute',
      top: '10px',
      left: 0,
      right: 0,
      margin: 'auto',
      fontSize: '10px',
      opacity: 0.3,
    },
  },
}))

export default withStyles(styles)(StatCard)
