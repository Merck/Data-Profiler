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
import { uniqBy } from 'lodash'
import React, { Fragment } from 'react'
import { CommentCounts } from '../../comments/models/CommentCounts'
import Sentence from '../../components/Sentence'
import CommonMetadata from '../../models/CommonMetadata'
import SelectedDrilldown from '../../drilldown/models/SelectedDrilldown'
import AggregatedSearchResult from '../../MultiSearch/models/AggregatedSearchResult'
import { nicefyNumber } from '../../nicefyNumber'
import AggregratedSearchResultService from '../../services/AggregatedSearchResultService'
import MetadataService from '../../services/MetadataService'
import MetadataSkeleton from './MetadataSkeleton'
import { genCommonMetadataStyles } from './metadataStyles'

// props from parent
export interface Props {
  classes: Record<string, any>
  drilldown: Readonly<SelectedDrilldown>
  result: Readonly<AggregatedSearchResult>
  datasetsMetadata: Readonly<CommonMetadata[]>
  commentCounts: Readonly<Partial<CommentCounts>>
  // handleViewCommentsFn: (event: React.SyntheticEvent) => void
}

interface State {}

class AllDatasetMetadata extends React.Component<Props, State> {
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
    const { classes, drilldown, datasetsMetadata, result } = this.props
    if (!datasetsMetadata) {
      return <MetadataSkeleton lines={4} height={24}></MetadataSkeleton>
    }

    const metadataService = new MetadataService()
    const metadata = metadataService.aggregateMetadata(datasetsMetadata)
    const { dataset, table } = drilldown
    const service = new AggregratedSearchResultService()
    const uniqDatasetNames = uniqBy(datasetsMetadata, (el) => el.datasetName)
    const numDatasetMatchingSearch = service.calcNumDatasets(result)
    const numTablesMatchingSearch = service.calcNumTables(result, dataset)
    const numColumnsMatchingSearch = service.calcNumColumns(
      result,
      dataset,
      table
    )
    const hitValueCount = service.calcValueHitCount(result, dataset, table)
    return (
      <Fragment>
        {this.generateSearchResultCount()}
        <div className={classes.metadataPanel}>
          <div className={classes.metadataRow}>
            <span className={classes.metadataRowLeft}>Datasets</span>
            <span className={classes.metadataRowRight}>
              <Sentence>
                {numDatasetMatchingSearch > 0 && (
                  <span>
                    <b>{nicefyNumber(numDatasetMatchingSearch)}</b> of
                  </span>
                )}
              </Sentence>
              {nicefyNumber(uniqDatasetNames.length || 0)}
            </span>
          </div>
          <div className={classes.metadataRow}>
            <span className={classes.metadataRowLeft}>Tables</span>
            <span className={classes.metadataRowRight}>
              <Sentence>
                {numTablesMatchingSearch > 0 && (
                  <span>
                    <b>{nicefyNumber(numTablesMatchingSearch)}</b> of
                  </span>
                )}
              </Sentence>
              {nicefyNumber(metadata.numTables)}
            </span>
          </div>
          <div className={classes.metadataRow}>
            <span className={classes.metadataRowLeft}>Columns</span>
            <span className={classes.metadataRowRight}>
              <Sentence>
                {numColumnsMatchingSearch > 0 && (
                  <span>
                    <b>{nicefyNumber(numColumnsMatchingSearch)}</b> of
                  </span>
                )}
              </Sentence>
              {nicefyNumber(metadata.numColumns)}
            </span>
          </div>
          <div className={classes.metadataRow}>
            <span className={classes.metadataRowLeft}>Rows</span>
            <span className={classes.metadataRowRight}>
              {nicefyNumber(metadata.numRows)}
            </span>
          </div>
          <div className={classes.metadataRow}>
            <span className={classes.metadataRowLeft}>Values</span>
            <span className={classes.metadataRowRight}>
              <Sentence>
                {hitValueCount > 0 && (
                  <span>
                    <b>{nicefyNumber(hitValueCount)}</b> of
                  </span>
                )}
              </Sentence>
              {nicefyNumber(metadata.numValues)}
            </span>
          </div>
          {/*
          // TODO: fetch correct unique values count
          <div className={classes.metadataRow}>
            <span className={classes.metadataRowLeft}>Unique Values</span>
            <span className={classes.metadataRowRight}>
              {nicefyNumber(datasetMetadata.numUniqueValues)}
            </span>
          </div> */}
          {/* <div className={classes.metadataRow}>
            <span className={classes.metadataRowLeft}>
              Comments
              <a
                href="#"
                className={classes.viewCommentsLink}
                onClick={(e) => this.props.handleViewCommentsFn(e)}>
                view
              </a>
            </span>
            <span className={classes.metadataRowRight}>
              {isNumber(commentCounts?.count) ? commentCounts.count : 'unknown'}
            </span>
          </div> */}
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

export default withStyles(styles)(AllDatasetMetadata)
