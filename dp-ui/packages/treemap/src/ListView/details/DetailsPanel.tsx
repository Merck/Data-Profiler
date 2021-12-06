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
import React, { Fragment } from 'react'
import { connect, ConnectedProps } from 'react-redux'
import { StoreState } from '../../index'
import CommonMetadata from '../../models/CommonMetadata'
import ReorderCommonMetadata from '../../models/ReorderCommonMetadata'
import AggregatedSearchResult from '../../MultiSearch/models/AggregatedSearchResult'
import ColumnDetails from './column/ColumnDetails'
import Distributions from './distribution/Distributions'
import TableDetails from './table/TableDetails'
import SelectedDrilldown from '../../drilldown/models/SelectedDrilldown'

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

class DatasetMetadataExpansionCard extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
  }

  generateDetailsPanel(): JSX.Element {
    const {
      drilldown,
      filter,
      hovered,
      datasetMetadata,
      tableMetadata,
      columnMetadata,
      samples,
      columnCounts,
    } = this.props
    const isCurrentCardSelectedDataset =
      datasetMetadata?.commonMetadata?.datasetName === drilldown?.dataset
    if (!drilldown || !isCurrentCardSelectedDataset) {
      return <Fragment></Fragment>
    }
    if (drilldown?.column) {
      return (
        <Distributions
          columnCounts={columnCounts}
          filter={filter}
          hovered={hovered}
          drilldown={drilldown}
          tableMetadata={tableMetadata}
          columnMetadata={columnMetadata}></Distributions>
      )
    } else if (drilldown?.table) {
      return (
        <ColumnDetails
          filter={filter}
          hovered={hovered}
          drilldown={drilldown}
          tableMetadata={tableMetadata}
          columnMetadata={columnMetadata}
          samples={samples}></ColumnDetails>
      )
    } else if (drilldown?.dataset) {
      return (
        <TableDetails
          filter={filter}
          hovered={hovered}
          drilldown={drilldown}
          numTables={datasetMetadata?.commonMetadata?.numTables}
          tableMetadata={tableMetadata}></TableDetails>
      )
    } else {
      return <Fragment></Fragment>
    }
  }

  render(): JSX.Element {
    const { classes } = this.props
    return <div className={classes.root}>{this.generateDetailsPanel()}</div>
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = createStyles((theme) => ({
  root: {
    display: 'flex',
    flexDirection: 'column',
    flex: 1,
  },
}))

const mapStateToProps = (state: StoreState) => ({
  columnMetadata: state.treemap?.selectedTableColumnMetadata,
  samples: state.listview?.samples,
  columnCounts: state.listview?.colCounts,
})

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators({}, dispatch)

const connector = connect(mapStateToProps, mapDispatchToProps)
export default withStyles(styles)(connector(DatasetMetadataExpansionCard))
