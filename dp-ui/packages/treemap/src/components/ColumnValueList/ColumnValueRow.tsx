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
import { safeCalculatePercent } from '../../helpers/NumberHelper'
import ValueCount from '../../models/ValueCount'
import AggregatedSearchResult from '../../MultiSearch/models/AggregatedSearchResult'
import { nicefyNumber } from '../../nicefyNumber'
import Bar from '../Bar'
import HighlightSentence from '../HighlightSentence'

// props from parent
export interface Props {
  style?: object
  classes: Record<string, any>
  filter: Readonly<AggregatedSearchResult>
  hovered?: Readonly<AggregatedSearchResult>
  highlightMatches?: Readonly<string[]>
  numValues?: number
  rowValue?: ValueCount
  uniqKey?: string
  relativePercentCount?: number
}
interface State {}

/**
 * Component to display a single colum counts value row
 *  used in a preview drawer, also in the list view column drilldown
 */
class ColumnValueRow extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
  }

  render(): JSX.Element {
    const {
      style,
      classes,
      rowValue,
      filter,
      hovered,
      highlightMatches,
      numValues,
      relativePercentCount,
      uniqKey,
    } = this.props
    const percentCount = relativePercentCount || numValues
    const extraMatches = highlightMatches || []
    return (
      <div key={`col-val-row-${uniqKey}`} className={classes.row} style={style}>
        <div className={classes.valueRow}>
          <span className={classes.valueRowLeft} title={rowValue.value}>
            <HighlightSentence
              content={rowValue.value}
              matchTerms={[filter?.value, hovered?.value, ...extraMatches]}
              useBold={false}
              showEllipsis={true}
            />
          </span>
          <span className={classes.valueRowRight}>
            {nicefyNumber(rowValue.count)}
          </span>
        </div>
        <div>
          <Bar
            percent={safeCalculatePercent(rowValue.count, percentCount)}></Bar>
        </div>
      </div>
    )
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
// width: 'calc(100vw - 68px)',
// marginLeft: 'auto',
const styles = (theme: Theme) =>
  createStyles({
    root: {},
    row: {
      '&:not(:last-of-type)': {
        paddingBottom: '24px',
      },
    },
    valueRow: {
      display: 'flex',
      paddingBottom: '8px',
    },
    valueRowLeft: {
      width: '100%',
      position: 'relative',
      '& [class^="Sentence-sentence"]': {
        position: 'absolute',
        top: 0,
        left: 0,
        flex: '1 1 auto',
        whiteSpace: 'nowrap',
        maxWidth: '100%',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        paddingRight: '8px',
      },
    },
    valueRowRight: {
      marginLeft: '40px',
    },
  })

export default withStyles(styles)(ColumnValueRow)
