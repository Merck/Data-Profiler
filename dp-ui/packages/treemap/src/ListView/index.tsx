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
import { Typography } from '@material-ui/core'
import { createStyles, Theme, withStyles } from '@material-ui/core/styles'
import { bindActionCreators, Dispatch } from '@reduxjs/toolkit'
import { isNull, isUndefined } from 'lodash'
import md5 from 'md5'
import React, { RefObject } from 'react'
import { connect, ConnectedProps } from 'react-redux'
import { StoreState } from '../index'
import DatasetMetadataExpansionCard from './DatasetMetadataExpansionCard'
import { getReorderCommonMetadata } from './selectors'
import { setIsInitialLoad } from './actions'
import { getSelectedDrilldown } from '../drilldown/selectors'

// props from parent
export interface OwnProps {
  classes: Record<string, any>
}
// props from redux state
interface StateProps {}
// props from redux actions
type DispatchProps = ConnectedProps<typeof connector>
type Props = StateProps & DispatchProps & OwnProps
interface State {}

class ListView extends React.Component<Props, State> {
  cardRefMap: Record<string, RefObject<HTMLDivElement>>
  cardPanelRef: RefObject<HTMLDivElement>

  constructor(props: Props) {
    super(props)
    // create refs to the cards so we can scroll to if needed
    const refMap = {}
    this.props?.reorderedDatasets?.forEach((metadata) => {
      const currentDataset = metadata?.commonMetadata?.datasetName
      const cardRef = React.createRef()
      refMap[currentDataset] = cardRef
    })
    this.cardRefMap = refMap
    this.cardPanelRef = React.createRef()
  }

  componentDidMount(): void {
    this.ensureScrollToSelectedCard()
  }

  render(): JSX.Element {
    const {
      classes,
      hovered,
      filter,
      tableMetadata,
      drilldown,
      reorderedDatasets,
    } = this.props
    const inSearchMode = this.isDefined(hovered) || this.isDefined(filter)
    const hits = hovered || filter
    const hasMetadata = (tableMetadata || []).some(
      (el) => el.datasetName === drilldown?.dataset
    )
    const key = this.updateStateKey()
    return (
      <div
        id={key}
        ref={this.cardPanelRef}
        key={key}
        className={classes.container}>
        <div className={classes.panel}>
          <Typography variant="h6" gutterBottom>
            All Data
          </Typography>
          {reorderedDatasets?.map((metadata, i) => {
            const currentDataset = metadata?.commonMetadata?.datasetName
            const cardKey = `dp-lv-metadata-${metadata?.commonMetadata?.datasetName}-${i}`
            return (
              <div key={cardKey} id={cardKey} style={{ position: 'relative' }}>
                <div
                  ref={this.cardRefMap[currentDataset]}
                  className={classes.anchor}
                />
                <DatasetMetadataExpansionCard
                  drilldown={drilldown}
                  datasetMetadata={metadata}
                  tableMetadata={hasMetadata ? tableMetadata : []}
                  filter={filter}
                  hovered={hovered}
                  inSearchMode={inSearchMode}
                  searchToken={hits?.value}
                />
              </div>
            )
          })}
        </div>
      </div>
    )
  }

  isDefined(el: any): boolean {
    return !isUndefined(el) && !isNull(el)
  }

  updateStateKey = (): string => {
    const { hovered, filter, drilldown, userSelectedOrderBy } = this.props
    const filterValue = filter?.value
    const filterCounts = filter?.count
    const hoveredValue = hovered?.value
    const hoveredCounts = hovered?.count
    const { dataset, table, column } = drilldown
    const keys = {
      filterValue,
      filterCounts,
      hoveredValue,
      hoveredCounts,
      dataset,
      table,
      column,
      userSelectedOrderBy,
    }
    const jsonState = JSON.stringify(keys)
    const hash = md5(jsonState)
    return hash
  }

  ensureScrollToSelectedCard(): void {
    const { drilldown, hovered } = this.props
    const selectedDataset = drilldown?.dataset
    // only call on intial page load, with a selected dataset, and with refs populated
    // NOTE: 3/11/21 removed initial load constraint
    if (!this.cardRefMap || !selectedDataset || hovered) {
      return
    }
    this.props.setIsInitialLoad(false)
    Object.keys(this.cardRefMap).map((currentDataset) => {
      const shouldScrollTo = currentDataset === selectedDataset
      if (shouldScrollTo) {
        const cardRef = this.cardRefMap[currentDataset]
        if (cardRef?.current) {
          const card = cardRef.current

          card?.scrollIntoView({
            behavior: 'auto',
            block: 'start',
            inline: 'start',
          })

          // card scroll bar seems to get reset after this call
          // maybe wrap in timeout to fix?
          // const id = card?.id
          // document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start', inline: 'start' })
        }
      }
    })
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = ({ palette, spacing }: Theme) =>
  createStyles({
    root: {},
    container: {
      marginTop: '12px',
      padding: '12px 28px 12px 4px',
      justifyContent: 'center',
      display: 'flex',
      // minHeight: '90vh',
      // maxHeight: '90vh',
      // overflowY: 'scroll',
      // position: 'relative',
      // '& .MuiTableContainer-root': {
      //   maxHeight: '45vh'
      // }
    },
    panel: {
      flex: '1 1 auto',
    },
    anchor: {
      position: 'absolute',
      top: '-151px', // offset
    },
  })

const mapStateToProps = (state: StoreState) => ({
  reorderedDatasets: getReorderCommonMetadata(state),
  tableMetadata: state.treemap.selectedDatasetTableMetadata,
  filter: state.treemap.filter,
  hovered: state.treemap.hoveredValue,
  drilldown: getSelectedDrilldown(state),
  userSelectedOrderBy: state.orderby.selectedOrderBy,
  // isInitialLoad: state.listview.isInitialLoad,
})

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators(
    {
      setIsInitialLoad,
    },
    dispatch
  )

const connector = connect(mapStateToProps, mapDispatchToProps)
export default withStyles(styles)(connector(ListView))
