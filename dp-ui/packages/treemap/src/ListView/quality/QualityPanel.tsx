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
import { bindActionCreators, Dispatch } from '@reduxjs/toolkit'
import React from 'react'
import { connect, ConnectedProps } from 'react-redux'
import moment from 'moment'
import { StoreState } from '../../index'
import CommonMetadata from '../../models/CommonMetadata'
import ReorderCommonMetadata from '../../models/ReorderCommonMetadata'
import { Grid } from '@material-ui/core'
import StatCard from './StatCard'
import CompleteQuality from '../../models/quality/CompleteQuality'
import { Alert, Skeleton } from '@material-ui/lab'
import {
  formatNumberWithHumanSuffix,
  formatNumberWithCommas,
} from '../../helpers/NumberHelper'
import QualityStat from '../../models/quality/QualityStat'
import FormattedQuality from '../../models/quality/FormattedQuality'
import { parseProperty } from '../parseProperty'
import CompletePerformance from '../../models/performance/CompletePerformance'
import _ from 'lodash'
import { Comment } from '../../comments/models/Comment'

// props from parent
export interface OwnProps {
  classes: Record<string, any>
  datasetMetadata: Readonly<ReorderCommonMetadata>
  tableMetadata?: Readonly<CommonMetadata[]>
}
// props from redux state
interface StateProps {}
// props from redux actions
type DispatchProps = ConnectedProps<typeof connector>
type Props = StateProps & DispatchProps & OwnProps
interface State {
  selectedTable?: string
  selectedTableData?: FormattedQuality
  tableNames?: { name: string; problematic: boolean }[]
}

class QualityPanel extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = {
      selectedTable: '',
      selectedTableData: {} as FormattedQuality,
      tableNames: [],
    }
    this.handleTableChange = this.handleTableChange.bind(this)
    this.ensureTableNames = this.ensureTableNames.bind(this)
  }

  ensureTableNames(problematicData: QualityStat[]) {
    if (!this.state.tableNames.length) {
      const problematicTableNames = problematicData.map((table) => table.table)
      const tableNames = this.props.tableMetadata.map((table) => {
        return {
          name: table.tableName,
          problematic: problematicTableNames.includes(table.tableName),
        }
      })

      this.setState({
        tableNames: _.orderBy(tableNames, ['name'], ['asc']),
      })
    }
  }

  handleTableChange(
    data: QualityStat[],
    event?: React.ChangeEvent<HTMLInputElement>
  ): void {
    if (event) {
      event.preventDefault()
    }

    const val = event.target.value
    const tableData = data.find((t) => t.table === val)
    this.setState({
      selectedTable: val,
      selectedTableData: val
        ? this.formatQualityData(tableData)
        : ({} as FormattedQuality),
    })
  }

  formatQualityData(data: QualityStat): FormattedQuality {
    const alerts = []
    const count = data?.count || 0
    let missingness = !isNaN(count) ? count / data?.totalNumValues : 0

    let type: string

    switch (true) {
      case missingness > 0.5:
        type = 'error'
      case missingness > 0.25:
        type = 'warning'
      default:
        type = 'success'
    }

    alerts.push({
      type,
      message: count
        ? `Missingness: ${formatNumberWithHumanSuffix(
            count
          )}/${formatNumberWithHumanSuffix(data.totalNumValues)} values`
        : 'No missing values!',
    })

    if (count) {
      alerts.push({
        type: 'warning',
        message: `${formatNumberWithCommas(count)} values null`,
      })
    } else {
      missingness = 0
    }

    return {
      value: 100 - Math.ceil(missingness * 100),
      alerts,
    }
  }

  formatComplianceData(
    data: { count: number; dataType: string }[],
    desc: string,
    comments: Readonly<Comment[]>,
    commentCounts: number,
    isInFlight: boolean,
    color: string
  ): FormattedQuality {
    const alerts = []
    let value = 100

    // description
    if (desc) {
      alerts.push({ type: 'success', message: 'Description: found' })
    } else {
      alerts.push({ type: 'error', message: 'Description: missing' })
      value = value - 50
    }

    // data types
    if (!data.length) {
      alerts.push({ type: 'warning', message: 'Column Types: N/A' })
    } else if (data.length > 1) {
      alerts.push({
        type: 'warning',
        message: 'Column Types: Mixed',
        tooltip: `<ul>${data
          .map(
            (type, i) =>
              `<li><div><span style="color: rgba(${color},${
                1 - (1 / data.length) * i
              });margin-right: 8px">${'\u2B24'}</span>${
                type.dataType
              }</div> <span>${type.count}</span></li>`
          )
          .join('')}</ul>`,
      })
      value = value - 25
    } else {
      alerts.push({
        type: 'warning',
        message: `Column Types: ${data[0].dataType}`,
      })
    }

    // comments
    if (isInFlight) {
      alerts.push({ type: 'warning', message: 'Fetching comments...' })
    } else if (!comments?.length) {
      alerts.push({ type: 'warning', message: 'Comments: 0' })
    } else {
      const commentArray = [...comments]

      if (commentCounts > comments.length) {
        commentArray.push({
          note: `...${commentCounts - comments.length} more comments`,
        } as Comment)
      }

      alerts.push({
        type: 'warning',
        message: `Comments: ${commentCounts}`,
        tooltip: commentArray,
      })
    }

    return {
      value,
      alerts,
    }
  }

  render(): JSX.Element {
    const { classes, datasetMetadata, comments, commentCounts, isInFlight } =
      this.props
    const { selectedTable, tableNames, selectedTableData } = this.state

    const quality = datasetMetadata?.commonMetadata?.properties.quality
    const performance = datasetMetadata?.commonMetadata?.properties.performance
    const desc = datasetMetadata?.commonMetadata?.properties?.description
    const completeQuality = parseProperty(quality) as CompleteQuality
    const completePerformance = parseProperty(
      performance
    ) as CompletePerformance
    if (!completeQuality || !completePerformance) {
      // return <Alert severity="info">Loading Quality Data</Alert>
      return (
        <div style={{ padding: '24px' }}>
          <div className={classes.title}>
            <span>Quality</span>
          </div>
          <Grid container spacing={3}>
            <Grid item xs>
              <Skeleton variant="rect" animation={false} height={227} />
            </Grid>
            <Grid item xs>
              <Skeleton variant="rect" animation={false} height={227} />
            </Grid>
            <Grid item xs>
              <Skeleton variant="rect" animation={false} height={227} />
            </Grid>
          </Grid>
        </div>
      )
    }
    const dataTypes = _.orderBy(
      completeQuality?.qualities.find(
        (item) => item.qualityEnum === 'COLUMN_TYPES'
      )?.columnTypeCounts,
      ['count'],
      ['desc']
    )
    const pieColor = '56,157,143' // rgb of main DP teal
    const formattedCompliance = this.formatComplianceData(
      dataTypes,
      desc,
      comments,
      commentCounts,
      isInFlight,
      pieColor
    )
    const numUsers = completePerformance.numUsersWithAttribute
    const viewsLastSevenDays = _.take(completePerformance.chartData, 8)
    const numViewsLastSevenDays =
      viewsLastSevenDays.reduce((a, b) => a + (b.views || 0), 0) || 0
    const topViewCount = completePerformance.topViewCountAcrossAllDatasets || 1
    const refreshTimestamp = moment
      .utc(completeQuality?.qualities?.[0]?.updatedOnMillis)
      .fromNow()
    const requiredKeys = [
      'updatedOnMillis',
      'datasetUpdatedOnMillis',
      'dataset',
      'table',
      'count',
      'column',
      'totalNumValues',
      'columnTypeCounts',
      'qualityEnum',
      'qualityWarningEnum',
    ]

    let containsAllKeys: boolean

    if (completeQuality?.qualities?.[0]) {
      const providedKeys = Object.keys(completeQuality?.qualities?.[0])
      containsAllKeys = requiredKeys.every((key) => providedKeys.includes(key))
    }

    if (!containsAllKeys) {
      return <Alert severity="error">Error fetching Quality data</Alert>
    }

    return (
      <div className={classes.root}>
        <div style={{ position: 'relative' }}>
          <div className={classes.title}>
            <span>Quality</span>
            <span className={classes.timestamp}>
              Last refreshed - {refreshTimestamp}
            </span>
          </div>
          <Grid container spacing={3}>
            <Grid item xs>
              <StatCard
                title="Compliance"
                value={formattedCompliance.value}
                // color="#2f857c"
                color={pieColor}
                alerts={formattedCompliance.alerts}
                pieData={dataTypes}
                pieTitle="Column Types"
              />
            </Grid>
            <Grid item xs>
              <StatCard
                title="Data Quality"
                value={selectedTableData?.value}
                color="#5451e4"
                alerts={selectedTableData?.alerts}
                tables={tableNames}
                handleTableChange={(e) =>
                  this.handleTableChange(completeQuality?.qualities, e)
                }
                ensureTableNames={() =>
                  this.ensureTableNames(completeQuality?.qualities)
                }
                selectedTable={selectedTable}
              />
            </Grid>
            <Grid item xs>
              <StatCard
                title="Usage"
                value={
                  topViewCount >= numViewsLastSevenDays
                    ? (numViewsLastSevenDays / topViewCount) * 100
                    : 0
                }
                color="#2d74b5"
                alerts={[
                  {
                    type: 'success',
                    message: `${formatNumberWithCommas(
                      numUsers
                    )} users can view`,
                  },
                  // {
                  //   type: 'success',
                  //   message: `${numViewsLastSevenDays} views last 7 days`,
                  // },
                ]}
              />
            </Grid>
          </Grid>
        </div>
      </div>
    )
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = createStyles((theme) => ({
  root: {
    padding: 24,
    // maxHeight: 'calc(125vh - 456px)',
    overflowY: 'auto',
    '& .MuiGrid-item': {
      display: 'flex',
    },
  },
  title: {
    fontSize: '1.2em',
    // height: '52px',
    marginBottom: '10px',
    display: 'flex',
    alignItems: 'center',
  },
  timestamp: {
    position: 'absolute',
    top: -1,
    right: 0,
    fontSize: 12,
    opacity: 0.4,
  },
}))

const mapStateToProps = (state: StoreState) => ({
  comments: _.orderBy(
    state.listview.qualityTabComments,
    ['timestamp'],
    ['desc']
  ),
  commentCounts: state.listview.qualityTabCommentsCount,
  isInFlight: state.listview.qualityTabCommentsInFlight,
})

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators({}, dispatch)

const connector = connect(mapStateToProps, mapDispatchToProps)
export default withStyles(styles)(connector(QualityPanel))
