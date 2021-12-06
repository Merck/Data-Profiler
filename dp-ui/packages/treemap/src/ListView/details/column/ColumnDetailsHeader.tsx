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
import React from 'react'
import { createStyles, Theme, withStyles } from '@material-ui/core/styles'
import HighlightSentence from '../../../components/HighlightSentence'
import Sentence from '../../../components/Sentence'
import TableChip from '../../../components/TableChip'
import CommonMetadata from '../../../models/CommonMetadata'
import AggregatedSearchResult from '../../../MultiSearch/models/AggregatedSearchResult'
import { nicefyNumber } from '../../../nicefyNumber'
import AggregratedSearchResultService from '../../../services/AggregatedSearchResultService'
import SelectedDrilldown from '../../../drilldown/models/SelectedDrilldown'
import { connect, ConnectedProps } from 'react-redux'
import { bindActionCreators, Dispatch } from '@reduxjs/toolkit'
import RowViewerLaunchButton from '../../../components/RowViewerLaunchButton'
import { SELECTED_VIEW_ENUM } from '../../../models/SelectedView'
import moment from 'moment'
import { DATE_TIME_GROUP_FMT } from '../../../dpDateFormats'
import { LIGHT_GRAY } from '../../../dpColors'
import * as dplib from '@dp-ui/lib'
import DownloadLauncherButton from '../../../components/DownloadLauncherButton'

// props from parent
export interface OwnProps {
  classes: Record<string, any>
  tableMetadata: Readonly<CommonMetadata[]>
  drilldown: Readonly<SelectedDrilldown>
  hits?: Readonly<AggregatedSearchResult>
}

type DispatchProps = ConnectedProps<typeof connector>
type Props = DispatchProps & OwnProps

interface State {}

class ColumnDetailsHeader extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
  }

  render(): JSX.Element {
    const { classes, tableMetadata, hits, drilldown } = this.props

    const searchToken = hits?.value
    const selectedTable = drilldown?.table || ''
    const metadata = tableMetadata?.filter((el) => {
      return (
        el.datasetName === drilldown?.dataset &&
        el.tableName === drilldown?.table
      )
    })[0]
    const { dataset, table } = drilldown
    const service = new AggregratedSearchResultService()
    let numColumnsMatchingSearch = 0
    let valueHitCount = 0
    if (hits) {
      numColumnsMatchingSearch = service.calcNumColumns(hits, dataset, table)
      valueHitCount = service.calcValueHitCount(hits, dataset, table)
    }
    return (
      <div className={classes.root}>
        <div className={classes.panel}>
          <div className={classes.chip}>
            <TableChip></TableChip>
          </div>
          <div className={classes.metaRow}>
            <div style={{ fontSize: '1.4em' }}>
              <HighlightSentence
                content={selectedTable}
                matchTerms={[searchToken]}
                useBold={false}></HighlightSentence>
            </div>
            <div className={classes.metaDetails}>
              <div>
                <span>Columns &nbsp;</span>
                <span className={classes.mutatedInfo}>
                  (
                  <Sentence>
                    {numColumnsMatchingSearch > 0 && (
                      <span>
                        <b>{nicefyNumber(numColumnsMatchingSearch)?.trim()}</b>{' '}
                        of
                      </span>
                    )}
                  </Sentence>
                  <span>{nicefyNumber(metadata?.numColumns)?.trim()}</span>)
                </span>
              </div>
              <div>
                <span>Rows &nbsp;</span>
                <span className={classes.mutatedInfo}>
                  ({nicefyNumber(metadata?.numRows)?.trim()})
                </span>
              </div>
              <div>
                <span>Values &nbsp;</span>
                <span className={classes.mutatedInfo}>
                  (
                  <Sentence>
                    {valueHitCount > 0 && (
                      <span>
                        <b>{nicefyNumber(valueHitCount)?.trim()}</b> of
                      </span>
                    )}
                  </Sentence>
                  <span>{nicefyNumber(metadata?.numValues)?.trim()}</span>)
                </span>
              </div>
              <div>
                <span>Updated Date &nbsp;</span>
                <span className={classes.mutatedInfo}>
                  ({moment(metadata?.updatedOn).format(DATE_TIME_GROUP_FMT)})
                </span>
              </div>
            </div>
            <div
              style={{
                display: 'flex',
                flexDirection: 'column',
              }}>
              <RowViewerLaunchButton
                dataset={drilldown?.dataset}
                table={drilldown?.table}
                launchedFrom={SELECTED_VIEW_ENUM.LIST}
              />
              <DownloadLauncherButton
                dataset={drilldown?.dataset}
                table={drilldown?.table}
              />
            </div>
          </div>
        </div>
      </div>
    )
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = (theme: Theme) =>
  createStyles({
    root: {
      marginBottom: '24px',
      minHeight: '48px',
    },
    panel: {
      display: 'flex',
      flexFlow: 'column',
    },
    chip: {
      paddingBottom: '4px',
    },
    metaRow: {
      display: 'flex',
      alignItems: 'center',
    },
    metaDetails: {
      flex: '1 1 0',
      display: 'flex',
      flexFlow: 'row',
      justifyContent: 'center',
      alignItems: 'center',
      '& > *': {
        paddingLeft: '12px',
        display: 'flex',
      },
    },
    mutatedInfo: {
      color: LIGHT_GRAY,
    },
  })

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators({}, dispatch)

const connector = connect(null, mapDispatchToProps)
const ColumnDetailsHeaderWithStyles = withStyles(styles)(
  connector(ColumnDetailsHeader)
)
export default dplib.DPContext(ColumnDetailsHeaderWithStyles)
