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
import { isEmpty, isUndefined } from 'lodash'
import React, { Fragment } from 'react'
import uuid from 'uuid/v4'
import { truncateTo } from '../../helpers/StringHelper'
import Sentence from '../Sentence'
import { HighlightToken } from './models/HighlightToken'
import HighlightTokenizerService from './services/HighlightTokenizerService'

// props from parent
export interface Props {
  classes: Readonly<Record<string, any>>
  content: Readonly<string>
  matchTerms: Readonly<string[]>
  useBold?: Readonly<boolean>
  willBreak?: Readonly<boolean>
  isFullWidth?: Readonly<boolean>
  willTruncate?: Readonly<boolean>
  showEllipsis?: Readonly<boolean>
}
interface State {}

/**
 * Simple Component to token, highlight and space sentence
 */
class HighlightSentence extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
  }

  renderToken(token: HighlightToken): JSX.Element {
    const { useBold } = this.props
    if (useBold) {
      return <b>{token.token}</b>
    } else {
      return <Fragment>{token.token}</Fragment>
    }
  }

  render(): JSX.Element {
    const { classes, matchTerms, content, useBold, showEllipsis } = this.props
    const addWillBreakClass = !isUndefined(this.props?.willBreak)
      ? this.props?.willBreak
      : true
    const addIsFullWidthClass = !isUndefined(this.props?.isFullWidth)
      ? this.props?.isFullWidth
      : false
    const doTruncation = !isUndefined(this.props?.willTruncate)
      ? this.props?.willTruncate
      : false
    let normalizedContent = content
    if (doTruncation) {
      normalizedContent = truncateTo(content)
    }
    const key = uuid()
    const service = new HighlightTokenizerService()
    const normalizedMatchTerms = service.normalizeMatchTerms(matchTerms)
    const tokenized = service.tokenizeAndMatchLongestSubstring(
      normalizedContent,
      normalizedMatchTerms
    )
    const baseClazzes = `${classes.higlightSentence} ${
      addWillBreakClass ? classes.willBreak : ''
    } ${addIsFullWidthClass ? classes.isFullWidth : ''}`
    const highlightClazzes = `${baseClazzes} ${
      !useBold ? classes.highlight : ''
    }`
    return (
      <Sentence spacingPx={4} showEllipsis={showEllipsis}>
        {tokenized?.map((token: HighlightToken, i) => {
          if (token.isMatch) {
            return (
              <span key={`${key}-${i}`} className={highlightClazzes}>
                {this.renderToken(token)}
              </span>
            )
          } else {
            return (
              <span className={baseClazzes} key={`${key}-${i}`}>
                {token.token}
              </span>
            )
          }
        })}
      </Sentence>
    )
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = (theme: Theme) =>
  createStyles({
    root: {},
    highlight: {
      background: '#deebff',
    },
    higlightSentence: {},
    willBreak: {
      wordBreak: 'break-all',
    },
    isFullWidth: {
      width: '100%',
    },
  })

export default withStyles(styles)(HighlightSentence)
