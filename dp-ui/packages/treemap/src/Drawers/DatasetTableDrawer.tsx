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
import Chip from '@material-ui/core/Chip'
import InputLabel from '@material-ui/core/InputLabel'
import MenuItem from '@material-ui/core/MenuItem'
import Select from '@material-ui/core/Select'
import { createStyles, Theme, withStyles } from '@material-ui/core/styles'
import { bindActionCreators, Dispatch } from '@reduxjs/toolkit'
import React, { Fragment } from 'react'
import { connect, ConnectedProps } from 'react-redux'
import { setSelectedDrilldown } from '../actions'
import HighlightSentence from '../components/HighlightSentence'
import Sentence from '../components/Sentence'
import { updateSelectedViewWithRefresh } from '../compositeActions'
import { StoreState } from '../index'
import SelectedDrilldown from '../drilldown/models/SelectedDrilldown'
import AggregatedSearchResult from '../MultiSearch/models/AggregatedSearchResult'
import MultisearchService from '../MultiSearch/services/MultisearchService'
import { nicefyNumber } from '../nicefyNumber'
import MetadataService from '../services/MetadataService'
import {
  clearColumnCounts,
  fetchColumnCounts,
  fetchMetadataHierarchy,
  updatePreviewDrawerDrilldown,
} from './actions'
import {
  clearCommentCounts,
  fetchCommentCounts,
  fetchCommentsAtAllLevels,
} from '../comments/actions'
import { genCommonDrawerStyles } from './drawerStyles'
import DatasetMetadata from './metadata/DatasetMetadata'
import TableMetadata from './metadata/TableMetadata'
import { getPreviewDrawerDrilldown } from './selectors'
import JumpToListButton from './JumpToListButton'

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

class DatasetTableDrawer extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.handleSelectDrilldown = this.handleSelectDrilldown.bind(this)
    this.handleDeselectDrilldown = this.handleDeselectDrilldown.bind(this)
    this.handleViewCommentsList = this.handleViewCommentsList.bind(this)
  }

  handleDeselectDrilldown(event?: React.SyntheticEvent): void {
    if (event) {
      event.preventDefault()
    }

    // clear preview drawer state
    const selectedDataset = new SelectedDrilldown(
      this.props.drawerDrilldown.dataset
    )
    this.props.updatePreviewDrawerDrilldown(selectedDataset)
    // call to clear column specific values
    this.props.clearCommentCounts()
    // load dataset level comment counts
    this.props.fetchCommentCounts(selectedDataset)
  }

  handleSelectDrilldown(event?: React.ChangeEvent<HTMLInputElement>): void {
    if (event) {
      event.preventDefault()
    }

    const table = event.target.value
    // fetch new data for preview drawer state
    const selectedTable = SelectedDrilldown.from(this.props.drawerDrilldown)
    // change table, but keep the dataset context
    selectedTable.table = table
    selectedTable.column = undefined
    this.props.updatePreviewDrawerDrilldown(selectedTable)
    // load table level comment counts
    this.props.fetchCommentCounts(selectedTable)
  }

  handleViewCommentsList(
    drilldown: SelectedDrilldown,
    event?: React.SyntheticEvent
  ): void {
    if (event) {
      event.preventDefault()
    }

    const drawerDrilldown = SelectedDrilldown.from(drilldown)
    drawerDrilldown.showComments = true
    this.props.updatePreviewDrawerDrilldown(drawerDrilldown)
    // NOTE: make sure to reset the table and column for the treemap drilldown
    // or the customer spinner will think data is being loaded
    const treemapDrilldown = SelectedDrilldown.from(drawerDrilldown)
    treemapDrilldown.table = undefined
    treemapDrilldown.column = undefined
    this.props.setSelectedDrilldown(treemapDrilldown)
    this.props.fetchCommentsAtAllLevels(drawerDrilldown)
    this.props.fetchMetadataHierarchy(drawerDrilldown)
  }

  generateHeader(title: 'Dataset' | 'Table'): JSX.Element {
    const { classes } = this.props
    return <h2 className={classes.header}>{title}</h2>
  }

  /**
   * TODO: remove this code if it is not used
   *
   * @deprecated
   * @returns
   */
  generateSearchResultCount(): JSX.Element {
    const { classes, searchFilter, drawerDrilldown } = this.props
    const result = this.currentlySelectedSearchResult(
      searchFilter,
      drawerDrilldown
    )
    if (result) {
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
    } else {
      return <Fragment></Fragment>
    }
  }

  currentlySelectedSearchResult(
    results: Readonly<AggregatedSearchResult>,
    selected: SelectedDrilldown = new SelectedDrilldown()
  ): AggregatedSearchResult {
    const service = new MultisearchService()
    return service.filterAndSum(results, selected)
  }

  generateTablePanel(): JSX.Element {
    const {
      classes,
      searchFilter,
      hovered,
      drawerDrilldown,
      tableMetadata,
      commentCounts,
    } = this.props
    const metadata = tableMetadata
      .filter((el) => el.datasetName === drawerDrilldown.dataset)
      .find((el) => el.tableName === drawerDrilldown.table)
    const result = this.currentlySelectedSearchResult(
      searchFilter,
      drawerDrilldown
    )

    return (
      <Fragment>
        {this.generateHeader('Table')}
        <div className={classes.name}>
          <HighlightSentence
            content={drawerDrilldown.dataset}
            matchTerms={[searchFilter?.value, hovered?.value]}
            useBold={false}
          />
        </div>
        <div className={classes.selectDrilldown}>
          <Chip
            label={drawerDrilldown.table}
            onDelete={this.handleDeselectDrilldown}
          />
        </div>
        <JumpToListButton drawerDrilldown={drawerDrilldown} />
        <div>
          <TableMetadata
            result={result}
            drilldown={drawerDrilldown}
            tableMetadata={metadata}
            commentCounts={commentCounts}
            handleViewCommentsFn={(e) =>
              this.handleViewCommentsList(drawerDrilldown, e)
            }></TableMetadata>
        </div>
      </Fragment>
    )
  }

  generateDatasetPanel(): JSX.Element {
    const {
      classes,
      drawerDrilldown,
      searchFilter,
      hovered,
      tableMetadata,
      commentCounts,
    } = this.props
    const drilldown = drawerDrilldown || new SelectedDrilldown()
    const service = new MetadataService()
    const tables = service.uniqTables(tableMetadata, drilldown.dataset)
    const metadata = service.aggregateMetadata(tableMetadata, drilldown.dataset)
    const result = this.currentlySelectedSearchResult(searchFilter, drilldown)
    return (
      <Fragment>
        {this.generateHeader('Dataset')}
        <div className={classes.name}>
          <HighlightSentence
            content={drawerDrilldown.dataset}
            matchTerms={[searchFilter?.value, hovered?.value]}
            useBold={false}
          />
        </div>
        <div className={classes.selectDrilldown}>
          <InputLabel className={classes.actionBtn} id="preview-select-label">
            select table
          </InputLabel>
          <Select
            MenuProps={{
              disablePortal: true,
              classes: { paper: classes.menuPaper },
            }}
            value=""
            className={classes.select}
            labelId="preview-select-label"
            id="preview-select-column"
            onChange={this.handleSelectDrilldown}>
            {tables.map((el, i) => {
              return (
                <MenuItem key={`column-select-${i}`} value={el}>
                  {el}
                </MenuItem>
              )
            })}
          </Select>
        </div>
        <JumpToListButton drawerDrilldown={drawerDrilldown} />
        <div>
          <DatasetMetadata
            result={result}
            drilldown={drawerDrilldown}
            datasetMetadata={metadata}
            commentCounts={commentCounts}
            handleViewCommentsFn={(e) =>
              this.handleViewCommentsList(drawerDrilldown, e)
            }></DatasetMetadata>
        </div>
      </Fragment>
    )
  }

  generateEmptyPanel(): JSX.Element {
    return (
      <Fragment>
        <p>N/A</p>
      </Fragment>
    )
  }

  render(): JSX.Element {
    const { isOpen, drawerDrilldown } = this.props

    if (!isOpen) {
      return this.generateEmptyPanel()
    }

    if (!drawerDrilldown.table) {
      return this.generateDatasetPanel()
    } else {
      return this.generateTablePanel()
    }
  }
}

const commonDrawerStyles = genCommonDrawerStyles()
// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = (theme: Theme) =>
  createStyles({
    root: {},
    ...commonDrawerStyles,
  })

const mapStateToProps = (state: StoreState) => ({
  isOpen: state.previewdrawer.isOpen,
  drawerDrilldown: getPreviewDrawerDrilldown(state),
  searchFilter: state.treemap.filter,
  hovered: state.treemap.hoveredValue,
  tableMetadata: state.treemap.selectedDatasetTableMetadata,
  commentCounts: state.comments.commentCounts,
})

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators(
    {
      updatePreviewDrawerDrilldown,
      setSelectedDrilldown,
      fetchColumnCounts,
      clearColumnCounts,
      fetchCommentCounts,
      clearCommentCounts,
      fetchCommentsAtAllLevels,
      fetchMetadataHierarchy,
      updateSelectedViewWithRefresh,
    },
    dispatch
  )

const connector = connect(mapStateToProps, mapDispatchToProps)
export default withStyles(styles)(connector(DatasetTableDrawer))
