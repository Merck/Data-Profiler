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
import { isNumber, isUndefined } from 'lodash'
import React, { Fragment } from 'react'
import { CommentCounts } from '../../comments/models/CommentCounts'
import Sentence from '../../components/Sentence'
import ColumnCounts from '../../models/ColumnCounts'
import CommonMetadata from '../../models/CommonMetadata'
import SelectedDrilldown from '../../drilldown/models/SelectedDrilldown'
import TreemapObject from '../../models/TreemapObject'
import AggregatedSearchResult from '../../MultiSearch/models/AggregatedSearchResult'
import { nicefyNumber } from '../../nicefyNumber'
import AggregratedSearchResultService from '../../services/AggregatedSearchResultService'
import MetadataSkeleton from './MetadataSkeleton'
import { genCommonMetadataStyles } from './metadataStyles'
import { ChatBubble } from '@material-ui/icons'

// props from parent
export interface Props {
  classes: Record<string, any>
  selectedColumn: Readonly<SelectedDrilldown>
  result: Readonly<AggregatedSearchResult>
  columnMetadata: Readonly<CommonMetadata>
  columnData: Readonly<TreemapObject[]>
  columnCounts: Readonly<ColumnCounts>
  commentCounts: Readonly<Partial<CommentCounts>>
  handleViewCommentsFn: (event: React.SyntheticEvent) => void
}
interface State {}

class ColumnMetadata extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
  }

  generateSearchResultCount(): JSX.Element {
    const { classes, result } = this.props
    if (!result) {
      return <Fragment></Fragment>
    }
    return (
      <div className={classes.resultCountContent}>
        <Sentence>
          <span>
            <b>{nicefyNumber(result.count).trim()}</b>
          </span>
          <span>results for</span>
          <span>
            <b>"{result.value}"</b>
          </span>
        </Sentence>
      </div>
    )
  }

  render(): JSX.Element {
    const { classes, result, selectedColumn, commentCounts, columnMetadata } =
      this.props
    if (!columnMetadata || !selectedColumn || isUndefined(commentCounts)) {
      return <MetadataSkeleton lines={4} height={24}></MetadataSkeleton>
    }

    const { dataset, table } = selectedColumn
    const service = new AggregratedSearchResultService()
    const hitValueCount = service.calcValueHitCount(result, dataset, table)

    return (
      <Fragment>
        {this.generateSearchResultCount()}
        <div className={classes.metadataPanel}>
          <div className={classes.metadataRow}>
            <span className={classes.metadataRowLeft}>Type</span>
            <span className={classes.metadataRowRight}>
              {columnMetadata?.dataType}
            </span>
          </div>
          <div className={classes.metadataRow}>
            <span className={classes.metadataRowLeft}>Values</span>
            <span className={`${classes.metadataRowRight}`}>
              <Sentence>
                {hitValueCount > 0 && (
                  <span>
                    <b>{nicefyNumber(hitValueCount)}</b> of
                  </span>
                )}
              </Sentence>
              <span>{nicefyNumber(columnMetadata.numValues)}</span>
            </span>
          </div>
          <div className={classes.metadataRow}>
            <span className={classes.metadataRowLeft}>Unique Values</span>
            <span className={`${classes.metadataRowRight}`}>
              <Sentence>
                {hitValueCount > 0 && (
                  <span>
                    <b>{nicefyNumber(hitValueCount)}</b> of
                  </span>
                )}
              </Sentence>
              <span>{nicefyNumber(columnMetadata.numUniqueValues)}</span>
            </span>
          </div>
        </div>
        <div className={`${classes.metadataRow} ${classes.commentsRow}`}>
          <span className={classes.metadataRowLeft}>
            <ChatBubble className={classes.commentsIcon}></ChatBubble>
            <a
              href="#"
              className={classes.viewCommentsLink}
              onClick={(e) => this.props.handleViewCommentsFn(e)}>
              Comments (
              {isNumber(commentCounts?.count) ? commentCounts.count : 'unknown'}
              )
            </a>
          </span>
        </div>
        <div>
          <hr />
        </div>
      </Fragment>
    )
  }
}

const commonMetadataStyles = genCommonMetadataStyles()
// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = (theme: Theme) =>
  createStyles({
    root: {},
    ...commonMetadataStyles,
  })

export default withStyles(styles)(ColumnMetadata)
