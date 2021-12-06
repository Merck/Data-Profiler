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
import { Tab, Tabs } from '@material-ui/core/'
import { createStyles, withStyles } from '@material-ui/core/styles'
import { bindActionCreators, Dispatch } from '@reduxjs/toolkit'
import React from 'react'
import { connect, ConnectedProps } from 'react-redux'
import { StoreState } from '../index'
import CommonMetadata from '../models/CommonMetadata'
import ReorderCommonMetadata from '../models/ReorderCommonMetadata'
import AggregatedSearchResult from '../MultiSearch/models/AggregatedSearchResult'
import { setTabIndex } from './actions'
import ActivityDeltaPanel from './delta/ActivityDeltaPanel'
import DetailsPanel from './details/DetailsPanel'
import OverviewPanel from './overview/OverviewPanel'
import QualityPanel from './quality/QualityPanel'
import SelectedDrilldown from '../drilldown/models/SelectedDrilldown'
import { ACTIVATED_COLOR, SELECTION_GREEN } from '../dpColors'
import {
  DATASET_DELTA_TAB_FEATURE_FLAG,
  QUALITY_TAB_FEATURE_FLAG,
  CARD_ACTIONS_FEATURE_FLAG,
} from '@dp-ui/parent/src/features'
import CardActions from './CardActions'

// props from parent
export interface OwnProps {
  classes: Record<string, any>
  datasetMetadata: Readonly<ReorderCommonMetadata>
  tableMetadata?: Readonly<CommonMetadata[]>
  drilldown: Readonly<SelectedDrilldown>
  filter: Readonly<AggregatedSearchResult>
  hovered: Readonly<AggregatedSearchResult>
  inSearchMode: boolean
  searchToken?: string
}
// props from redux state
interface StateProps {}
// props from redux actions
type DispatchProps = ConnectedProps<typeof connector>
type Props = StateProps & DispatchProps & OwnProps
interface State {}

interface TabPanelProps {
  index: string
  value?: string
  ariaPrefix?: string
  children?: React.ReactNode
  classes?: any
  className?: any
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ariaPrefix, ...other } = props

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`${ariaPrefix}-tabpanel-${index}`}
      aria-labelledby={`${ariaPrefix}-tab-${index}`}
      {...other}>
      {value === index && children}
    </div>
  )
}

class TabbedPanels extends React.Component<Props, State> {
  ariaPrefix = 'datamart'

  constructor(props: Props) {
    super(props)
    this.handleChange = this.handleChange.bind(this)
  }

  tabA11yProps(index: number, ariaPrefix = 'dataprofiler') {
    return {
      id: `${ariaPrefix}-tab-${index}`,
      'aria-controls': `${ariaPrefix}-tabpanel-${index}`,
    }
  }

  handleChange(event: React.ChangeEvent<{}>, newValue: string) {
    if (event) {
      event.preventDefault()
    }

    this.props.setTabIndex(newValue)
  }

  render(): JSX.Element {
    const { filter, hovered, tableMetadata, inSearchMode } = this.props
    const { classes, datasetMetadata, drilldown } = this.props
    const value = this.props.tabIndex
    const ariaPrefix = this.ariaPrefix
    const dataset = datasetMetadata.commonMetadata.datasetName

    // TODO: pull out metadata in state

    if (drilldown?.dataset !== dataset) {
      return <></>
    }

    return (
      <div className={classes.root}>
        <Tabs
          value={value}
          onChange={this.handleChange}
          // indicatorColor="primary"
          aria-label={`${dataset} tabs`}>
          <Tab
            label="Overview"
            value="overview"
            {...this.tabA11yProps(0, ariaPrefix)}
          />
          <Tab
            label="Data"
            value="data"
            {...this.tabA11yProps(2, ariaPrefix)}
          />
          {DATASET_DELTA_TAB_FEATURE_FLAG === true && (
            <Tab
              disabled={DATASET_DELTA_TAB_FEATURE_FLAG ? false : true}
              label="Activity"
              value="activity"
              {...this.tabA11yProps(3, ariaPrefix)}
            />
          )}
          {QUALITY_TAB_FEATURE_FLAG === true && (
            <Tab
              disabled={QUALITY_TAB_FEATURE_FLAG ? false : true}
              label="Quality"
              value="quality"
              {...this.tabA11yProps(4, ariaPrefix)}
            />
          )}
        </Tabs>
        <TabPanel
          className={classes.tabPanel}
          ariaPrefix={ariaPrefix}
          value={value}
          index="overview">
          <OverviewPanel datasetMetadata={datasetMetadata}></OverviewPanel>
        </TabPanel>
        <TabPanel
          className={classes.tabPanel}
          ariaPrefix={ariaPrefix}
          value={value}
          index="data">
          <DetailsPanel
            inSearchMode={inSearchMode}
            filter={filter}
            hovered={hovered}
            drilldown={drilldown}
            datasetMetadata={datasetMetadata}
            tableMetadata={tableMetadata}></DetailsPanel>
          {CARD_ACTIONS_FEATURE_FLAG === true && (
            <CardActions
              dataset={datasetMetadata?.commonMetadata?.datasetName}
              tableMetadata={tableMetadata}
            />
          )}
        </TabPanel>
        {DATASET_DELTA_TAB_FEATURE_FLAG === true && (
          <TabPanel
            className={classes.tabPanel}
            ariaPrefix={ariaPrefix}
            value={value}
            index="activity">
            <ActivityDeltaPanel
              datasetMetadata={datasetMetadata}></ActivityDeltaPanel>
          </TabPanel>
        )}
        {QUALITY_TAB_FEATURE_FLAG === true && (
          <TabPanel
            className={classes.tabPanel}
            ariaPrefix={ariaPrefix}
            value={value}
            index="quality">
            <QualityPanel
              datasetMetadata={datasetMetadata}
              tableMetadata={tableMetadata}></QualityPanel>
          </TabPanel>
        )}
      </div>
    )
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = createStyles((theme) => ({
  root: {
    display: 'flex',
    flexDirection: 'column',
    flex: 1,
    '& .Mui-disabled': {
      opacity: 0.3,
    },
    '& .Mui-selected': {
      color: ACTIVATED_COLOR,
    },
    '& .MuiTabs-indicator': {
      backgroundColor: SELECTION_GREEN,
    },
  },
  tabPanel: {
    padding: 16,
    margin: -16,
    marginTop: 0,
    backgroundColor: '#f7f7f7',
    '&:not([hidden])': {
      display: 'flex',
      flexDirection: 'column',
      flex: 1,
    },
    height: '100vh',
  },
}))

const mapStateToProps = (state: StoreState) => ({
  tabIndex: state.listview.tabIndex,
})

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators(
    {
      setTabIndex,
    },
    dispatch
  )

const connector = connect(mapStateToProps, mapDispatchToProps)
export default withStyles(styles)(connector(TabbedPanels))
