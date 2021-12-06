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
import ReorderCommonMetadata from '../../models/ReorderCommonMetadata'
import { Grid } from '@material-ui/core'
import { ACTIVATED_COLOR } from '../../dpColors'
import StatCard from './StatCard'
import DataTimeChart from './DataTimeChart'
import DailyStat from '../../models/performance/DailyStat'
import CompletePerformance from '../../models/performance/CompletePerformance'
import { Alert, Skeleton } from '@material-ui/lab'
import {
  formatNumberWithHumanSuffix,
  formatNumberWithCommas,
} from '../../helpers/NumberHelper'
import _ from 'lodash'
import { PERFORMANCE_PLACEHOLDERS_FEATURE_FLAG } from '@dp-ui/parent/src/features'
import { parseProperty } from '../parseProperty'
import DescriptionPanel from '../description/DescriptionPanel'

// props from parent
export interface OwnProps {
  classes: Record<string, any>
  datasetMetadata: Readonly<ReorderCommonMetadata>
}
// props from redux state
interface StateProps {}
// props from redux actions
type DispatchProps = ConnectedProps<typeof connector>
type Props = StateProps & DispatchProps & OwnProps
interface State {}

class OverviewPanel extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
  }

  formatDates = (data: DailyStat[]) => {
    const formattedData = [...data].map((d) => {
      const splitDate = d.day.split(',')
      const startDate = moment(splitDate[0]).format('MMM Do')
      const endDate = moment(splitDate[1]).format('MMM Do')

      return {
        ...d,
        day: `${startDate} to ${endDate}`,
      }
    })

    return formattedData
  }

  render(): JSX.Element {
    const { classes, datasetMetadata } = this.props
    const performance = datasetMetadata?.commonMetadata?.properties.performance
    const completePerformance = parseProperty(
      performance
    ) as CompletePerformance
    if (!completePerformance) {
      // return <Alert severity="info">Loading Performance Data</Alert>
      return (
        <div style={{ padding: '24px' }}>
          <Grid container spacing={5}>
            <Grid item md={5}>
              <Skeleton variant="rect" animation={false} height={460} />
            </Grid>
            <Grid item md={7}>
              <Grid container spacing={5}>
                <Grid item md>
                  <Skeleton variant="rect" animation={false} height={182} />
                </Grid>
              </Grid>
              <Grid container spacing={5} style={{ marginTop: '20px' }}>
                <Grid item xs>
                  <Skeleton variant="rect" animation={false} height={238} />
                </Grid>
                <Grid item xs>
                  <Skeleton variant="rect" animation={false} height={238} />
                </Grid>
                <Grid item xs>
                  <Skeleton variant="rect" animation={false} height={238} />
                </Grid>
              </Grid>
            </Grid>
          </Grid>
        </div>
      )
    }
    const chartData = this.formatDates(
      _.orderBy(completePerformance?.chartData, ['day'], ['asc']) as DailyStat[]
    )
    const numUsers = completePerformance.numUsersWithAttribute
    const numContractors = completePerformance.numUserWithAttributeAndContractor
    const downloadData = completePerformance.downloadData
    const searchAppearanceData = completePerformance.searchAppearanceData
    const refreshTimestamp = moment
      .utc(completePerformance?.updatedOnMillis)
      .fromNow()
    const requiredKeys = [
      'updatedOnMillis',
      'chartData',
      'numUsersWithAttribute',
      'numUserWithAttributeAndContractor',
      'downloadData',
      'searchAppearanceData',
    ]
    const providedKeys = Object.keys(completePerformance)
    const containsAllKeys = requiredKeys.every((key) =>
      providedKeys.includes(key)
    )

    if (!containsAllKeys) {
      return <Alert severity="error">Error fetching Performance data</Alert>
    }

    return (
      <div className={classes.root}>
        <div style={{ position: 'relative' }}>
          <span className={classes.timestamp}>
            Last refreshed - {refreshTimestamp}
          </span>
          <Grid container spacing={5}>
            <Grid item md={5}>
              <DescriptionPanel
                datasetMetadata={datasetMetadata}></DescriptionPanel>
            </Grid>
            <Grid item md={7}>
              <Grid container>
                {/* {PERFORMANCE_PLACEHOLDERS_FEATURE_FLAG && (
              <Grid item md>
                <div className={classes.impact}>
                  <div>
                    <div className={classes.badge}></div>
                  </div>
                  <div>
                    <span className={classes.label}>TOP PERFORMER</span>
                    <span className={classes.header}>High Impact Data Set</span>
                    <span className={classes.description}>
                      Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                      Suspendisse dapibus molestie mollis.
                    </span>
                  </div>
                </div>
              </Grid>
            )} */}
                <Grid item md>
                  <DataTimeChart data={chartData} />
                </Grid>

                <Grid container spacing={5} style={{ marginTop: '20px' }}>
                  <Grid item xs>
                    <StatCard
                      label="Users"
                      value={formatNumberWithHumanSuffix(numUsers)}
                      description={`${formatNumberWithCommas(
                        numUsers
                      )} users have access: ${formatNumberWithCommas(
                        numUsers - numContractors
                      )} internal and ${formatNumberWithCommas(
                        numContractors
                      )} contractors`}
                      isTotal
                    />
                  </Grid>
                  <Grid item xs>
                    <StatCard
                      label="Searches"
                      value={formatNumberWithHumanSuffix(
                        searchAppearanceData.allTime
                      )}
                      description={`Appeared in ${formatNumberWithCommas(
                        searchAppearanceData.lastSevenFromToday
                      )} Search Results (last 7 days)`}
                      delta={
                        searchAppearanceData.lastSevenFromToday -
                        searchAppearanceData.lastSevenFromYesterday
                      }
                      isTotal
                    />
                  </Grid>
                  <Grid item xs>
                    <StatCard
                      label="Downloads"
                      value={formatNumberWithCommas(downloadData.allTime)}
                      description={`Dataset downloaded ${formatNumberWithCommas(
                        downloadData.lastSevenFromToday
                      )} times (last 7 days)`}
                      delta={
                        downloadData.lastSevenFromToday -
                        downloadData.lastSevenFromYesterday
                      }
                      isTotal
                    />
                  </Grid>
                  {/* <Grid item xs>
              <StatCard
                placeholder
                label="Copies"
                value="32"
                description={`32 data moves`}
                delta={1}
              />
            </Grid> */}
                  {/* {PERFORMANCE_PLACEHOLDERS_FEATURE_FLAG && (
              <>
                <Grid item xs>
                  <StatCard
                    placeholder
                    label="Applications"
                    value="3"
                    description={`Built with this data`}
                    delta={1}
                    isTotal
                  />
                </Grid>
                <Grid item xs>
                  <StatCard
                    placeholder
                    label="App Users"
                    value="800"
                    description={`800 Active Application Users`}
                    delta={1}
                    isTotal
                  />
                </Grid>
              </>
            )} */}
                </Grid>
              </Grid>
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
    maxHeight: 'calc(125vh - 456px)',
    overflowY: 'auto',
    '& .MuiGrid-item': {
      display: 'flex',
    },
  },
  timestamp: {
    position: 'absolute',
    top: -1,
    right: 0,
    fontSize: 12,
    opacity: 0.4,
  },
  impact: {
    display: 'flex',
    '& span': {
      display: 'block',
      marginBottom: '3px',
    },
  },
  badge: {
    borderRadius: '50%',
    marginRight: '24px',
    backgroundColor: '#f4f2fd',
    width: '130px',
    height: '130px',
  },
  label: {
    color: ACTIVATED_COLOR,
    letterSpacing: '1px !important',
    fontWeight: 600,
  },
  header: {
    fontSize: '24pt',
    fontWeight: 300,
  },
  description: {
    fontSize: '16pt',
    fontWeight: 300,
    opacity: 0.8,
  },
}))

const mapStateToProps = (state: StoreState) => ({})

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators({}, dispatch)

const connector = connect(mapStateToProps, mapDispatchToProps)
export default withStyles(styles)(connector(OverviewPanel))
