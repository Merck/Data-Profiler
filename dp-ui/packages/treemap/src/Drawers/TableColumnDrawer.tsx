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
import Skeleton from '@material-ui/lab/Skeleton'
import { bindActionCreators, Dispatch } from '@reduxjs/toolkit'
import { isUndefined, range, sortBy } from 'lodash'
import React, { Fragment } from 'react'
import { connect, ConnectedProps } from 'react-redux'
import { setSelectedDrilldown } from '../actions'
import { updateSelectedViewWithRefresh } from '../compositeActions'
import ColumnValueList from '../components/ColumnValueList'
import Sentence from '../components/Sentence'
import { StoreState } from '../index'
import SelectedDrilldown from '../drilldown/models/SelectedDrilldown'
import AggregatedSearchResult from '../MultiSearch/models/AggregatedSearchResult'
import { nicefyNumber } from '../nicefyNumber'
import {
  clearColumnCounts,
  fetchColumnCounts,
  fetchMetadataHierarchy,
  refreshColCounts,
  updatePreviewDrawerDrilldown,
} from './actions'
import {
  clearCommentCounts,
  fetchCommentCounts,
  fetchCommentsAtAllLevels,
} from '../comments/actions'
import ColumnMetadata from './metadata/ColumnMetadata'
import TableMetadata from './metadata/TableMetadata'
import { getPreviewDrawerDrilldown } from './selectors'
import MultisearchService from '../MultiSearch/services/MultisearchService'
import HighlightSentence from '../components/HighlightSentence'
import { genCommonDrawerStyles } from './drawerStyles'
import JumpToListButton from './JumpToListButton'
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

class TableColumnDrawer extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.handleSelectDrilldown = this.handleSelectDrilldown.bind(this)
    this.handleDeSelectDrilldown = this.handleDeSelectDrilldown.bind(this)
    this.handleViewCommentsList = this.handleViewCommentsList.bind(this)
  }

  handleDeSelectDrilldown(event: React.SyntheticEvent): void {
    if (event) {
      event.preventDefault()
    }

    // clear preview drawer state
    const selectedTable = SelectedDrilldown.from(this.props.drawerDrilldown)
    // deselect column, but keep the dataset and table context
    selectedTable.column = undefined
    this.props.updatePreviewDrawerDrilldown(selectedTable)
    // call to clear column specific values
    this.props.clearColumnCounts()
    this.props.clearCommentCounts()
    // load table level comment counts
    this.props.fetchCommentCounts(selectedTable)
  }

  handleSelectDrilldown(event: React.ChangeEvent<HTMLInputElement>): void {
    if (event) {
      event.preventDefault()
    }

    const column = event.target.value
    // fetch new data for preview drawer state
    const selectedColumn = SelectedDrilldown.from(this.props.drawerDrilldown)
    // change column, but keep the dataset and table context
    selectedColumn.column = column
    this.props.updatePreviewDrawerDrilldown(selectedColumn)
    // load non search result, column counts values
    this.props.fetchColumnCounts(selectedColumn)
    this.props.fetchCommentCounts(selectedColumn)
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
    const treemapDrilldown = SelectedDrilldown.from(drawerDrilldown)
    treemapDrilldown.column = undefined
    this.props.setSelectedDrilldown(treemapDrilldown)
    this.props.fetchCommentsAtAllLevels(drawerDrilldown)
    this.props.fetchMetadataHierarchy(drawerDrilldown)
  }

  generateHeader(title: 'Column' | 'Table'): JSX.Element {
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

  generateValuesSkeleton(): JSX.Element {
    return (
      <div>
        {range(16).map((i) => (
          <Skeleton
            key={`dp-preview-drawer-value-skeleton-${i}`}
            animation={false}
            height={48}></Skeleton>
        ))}
      </div>
    )
  }

  generateColumnPanel(): JSX.Element {
    const {
      classes,
      searchFilter,
      hovered,
      drawerDrilldown,
      columnCounts,
      commentCounts,
      columnMetadata,
      columnData,
    } = this.props
    if (!columnMetadata) {
      return <Fragment></Fragment>
    }
    const metadata = columnMetadata
      .filter(
        (el) =>
          el.datasetName === drawerDrilldown.dataset &&
          el.tableName === drawerDrilldown.table
      )
      .find((el) => el.columnName === drawerDrilldown.column)
    const result = this.currentlySelectedSearchResult(
      searchFilter,
      drawerDrilldown
    )
    const overallCount = metadata?.numValues ? metadata.numValues : 0
    const uniqueCount = metadata?.numUniqueValues ? metadata.numUniqueValues : 0
    return (
      <Fragment>
        {this.generateHeader('Column')}
        <div className={classes.name}>
          <HighlightSentence
            content={drawerDrilldown.table}
            matchTerms={[searchFilter?.value, hovered?.value]}
            useBold={false}
          />
        </div>
        <div className={classes.selectDrilldown}>
          <Chip
            label={drawerDrilldown.column}
            onDelete={this.handleDeSelectDrilldown}
          />
        </div>
        <JumpToListButton drawerDrilldown={drawerDrilldown} />
        <div className={classes.columnMetaContent}>
          <ColumnMetadata
            result={result}
            selectedColumn={drawerDrilldown}
            columnCounts={columnCounts}
            columnData={columnData}
            commentCounts={commentCounts}
            columnMetadata={metadata}
            handleViewCommentsFn={(e) =>
              this.handleViewCommentsList(drawerDrilldown, e)
            }></ColumnMetadata>
        </div>
        <div className={classes.columnValueCountContent}>
          {isUndefined(columnCounts) && this.generateValuesSkeleton()}
          {!isUndefined(columnCounts) && (
            <ColumnValueList
              refreshColCounts={this.props.refreshColCounts}
              drilldown={drawerDrilldown}
              columnCounts={columnCounts}
              numValues={overallCount}
              numUniqueValues={uniqueCount}
              filter={searchFilter}></ColumnValueList>
          )}
        </div>
      </Fragment>
    )
  }

  generateTablePanel(): JSX.Element {
    const {
      classes,
      searchFilter,
      hovered,
      tableMetadata,
      drawerDrilldown,
      columnData,
      commentCounts,
    } = this.props
    const drilldown = drawerDrilldown || new SelectedDrilldown()
    const columns = sortBy(columnData, 'name')
    const metadata = tableMetadata.find(
      (el) => el.tableName === drilldown.table
    )
    const result = this.currentlySelectedSearchResult(searchFilter, drilldown)
    return (
      <Fragment>
        {this.generateHeader('Table')}
        <div className={classes.name}>
          <HighlightSentence
            content={drawerDrilldown.table}
            matchTerms={[searchFilter?.value, hovered?.value]}
            useBold={false}
          />
        </div>
        <div className={classes.selectDrilldown}>
          <InputLabel className={classes.actionBtn} id="preview-select-label">
            select column
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
            {columns.map((el, i) => {
              return (
                <MenuItem key={`column-select-${i}`} value={el.name}>
                  {el.name}
                </MenuItem>
              )
            })}
          </Select>
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

  generateEmptyPanel(): JSX.Element {
    return (
      <Fragment>
        <p>N/A</p>
      </Fragment>
    )
  }

  render(): JSX.Element {
    const { isOpen, drawerDrilldown: selectedColumn } = this.props

    if (!isOpen) {
      return this.generateEmptyPanel()
    }

    if (!selectedColumn.column) {
      return this.generateTablePanel()
    } else {
      return this.generateColumnPanel()
    }
  }
}

const commonDrawerStyles = genCommonDrawerStyles()
// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = (theme: Theme) =>
  createStyles({
    root: {},
    columnValueCountContent: {
      paddingTop: '12px',
      position: 'relative',
    },
    ...commonDrawerStyles,
  })

const mapStateToProps = (state: StoreState) => ({
  selectedDrilldown: getSelectedDrilldown(state),
  isOpen: state.previewdrawer.isOpen,
  drawerDrilldown: getPreviewDrawerDrilldown(state),
  searchFilter: state.treemap.filter,
  hovered: state.treemap.filter,
  tableMetadata: state.treemap.selectedDatasetTableMetadata,
  columnMetadata: state.treemap.selectedTableColumnMetadata,
  columnData: state.treemap.selectedColumnData,
  columnCounts: state.previewdrawer.columnCounts,
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
      refreshColCounts,
      updateSelectedViewWithRefresh,
    },
    dispatch
  )

const connector = connect(mapStateToProps, mapDispatchToProps)
export default withStyles(styles)(connector(TableColumnDrawer))
