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
import { bindActionCreators, Dispatch } from '@reduxjs/toolkit'
import React from 'react'
import { connect, ConnectedProps } from 'react-redux'
import { StoreState } from '../../../index'
import ColumnCounts from '../../../models/ColumnCounts'
import CommonMetadata from '../../../models/CommonMetadata'
import SelectedDrilldown from '../../../drilldown/models/SelectedDrilldown'
import AggregatedSearchResult from '../../../MultiSearch/models/AggregatedSearchResult'
import {
  clearColumnCounts,
  clearListViewDrilldown,
  refreshColCounts,
  setListViewDrilldown,
} from '../../actions'
import ColumnMetadataPanel from './ColumnMetadataPanel'
import DistributionsList from './DistributionsList'
import DistributionsNavigation from './DistributionsNavigation'

// props from parent
export interface OwnProps {
  classes: Record<string, any>
  tableMetadata: Readonly<CommonMetadata[]>
  columnMetadata: Readonly<CommonMetadata[]>
  filter: Readonly<AggregatedSearchResult>
  hovered: Readonly<AggregatedSearchResult>
  drilldown: Readonly<SelectedDrilldown>
  columnCounts: Readonly<ColumnCounts>
}

type DispatchProps = ConnectedProps<typeof connector>
type Props = DispatchProps & OwnProps

interface State {}

class Distributions extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.handleClick = this.handleClick.bind(this)
  }

  handleClick(
    drilldown?: SelectedDrilldown,
    event?: React.MouseEvent<Element>
  ): void {
    if (event) {
      event?.preventDefault()
    }
    if (!drilldown) {
      return
    }
    this.props?.clearColumnCounts()
    this.props?.setListViewDrilldown(drilldown)
  }

  render(): JSX.Element {
    const {
      classes,
      drilldown,
      filter,
      hovered,
      columnMetadata,
      columnCounts,
    } = this.props
    const { dataset, table, column } = drilldown
    const selectedMetadata = columnMetadata?.find(
      (el) =>
        dataset === el?.datasetName &&
        table === el?.tableName &&
        column === el?.columnName
    )

    return (
      <div className={classes.root}>
        <DistributionsNavigation
          filter={filter}
          hovered={hovered}
          drilldown={drilldown}
          columnMetadata={columnMetadata}
          handleClick={this.handleClick}
        />
        <ColumnMetadataPanel
          filter={filter}
          hovered={hovered}
          drilldown={drilldown}
          columnMetadata={selectedMetadata}
        />
        <div className={classes.distroPanel}>
          <DistributionsList
            refreshColCounts={this.props.refreshColCounts}
            filter={filter}
            hovered={hovered}
            columnMetadata={selectedMetadata}
            columnCounts={columnCounts}
            drilldown={SelectedDrilldown.of({
              dataset,
              table,
              column,
            })}
          />
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
      display: 'flex',
      flexFlow: 'row',
      height: '100%',
      overflowY: 'auto',
    },
    distroPanel: {
      width: '100%',
      margin: '0 24px',
      display: 'flex',
      flex: 1,
    },
  })

const mapStateToProps = (state: StoreState) => ({})

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators(
    {
      setListViewDrilldown,
      clearListViewDrilldown,
      clearColumnCounts,
      refreshColCounts,
    },
    dispatch
  )

const connector = connect(mapStateToProps, mapDispatchToProps)
export default withStyles(styles)(connector(Distributions))
