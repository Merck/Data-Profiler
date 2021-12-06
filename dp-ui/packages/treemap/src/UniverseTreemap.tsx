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
import * as dplib from '@dp-ui/lib'
import { Button, CircularProgress, Grid, Paper } from '@material-ui/core'
import { createStyles, makeStyles } from '@material-ui/core/styles'
import ArrowRightIcon from '@material-ui/icons/ArrowRight'
import { bindActionCreators, Dispatch } from '@reduxjs/toolkit'
import { cloneDeep, get, has, isEmpty, isNull, isUndefined } from 'lodash'
import React, { Fragment, useLayoutEffect, useRef } from 'react'
import { connect } from 'react-redux'
import { v4 } from 'uuid'
import DatasetChip from './components/DatasetChip'
import OrderBySelection from './components/OrderBySelection'
import PreviewDrawer from './components/PreviewDrawer'
import TableChip from './components/TableChip'
import ViewModeSwitcher from './components/ViewModeSwitcher'
import { updateSelectedDrilldownWithRefresh } from './compositeActions'
import customWithLoadingSpinner from './customWithLoadingSpinner'
import { setDatasetsMetadata } from './Drawers/actions'
import AddCommentDrawer from './Drawers/AddCommentDrawer'
import AllDatasetDrawer from './Drawers/AllDatasetDrawer'
import CommentListDrawer from './Drawers/CommentListDrawer'
import DatasetTableDrawer from './Drawers/DatasetTableDrawer'
import TableColumnDrawer from './Drawers/TableColumnDrawer'
import { StoreState } from './index'
import ListView from './ListView'
import SelectedDrilldown from './drilldown/models/SelectedDrilldown'
import SelectedView, { SELECTED_VIEW_ENUM } from './models/SelectedView'
import MultiSearch from './MultiSearch'
import {
  getSelectedDrilldownForTreeMap,
  getSelectedView,
} from './MultiSearch/selectors'
import MultisearchService from './MultiSearch/services/MultisearchService'
import TreemapFilterService from './services/TreemapFilterService'
import TreemapInitialize from './TreemapInitialize'
import { treemap } from './universe-treemap'
import { closeRowViewer, openRowViewer, setSelectedView } from './actions'
import RowViewer from '@dp-ui/row-viewer'
import RowViewerLaunchButton from './components/RowViewerLaunchButton'
import { SelectedViewState } from './models/SelectedViewState'
import md5 from 'md5'
import RowviewerSearchTermAdapter from './services/RowviewerSearchTermAdapter'
import { checkAndSetGlobalDatasets } from './contextToRedux'
import { setListViewDrilldown } from './ListView/actions'

const drawerHeightOffset = 136
const styles = () =>
  createStyles({
    root: {
      flex: '1',
      display: 'flex',
      flexFlow: 'column',
      paddingLeft: 28,
    },
    treemapViewRootLeft: {
      marginLeft: -28,
    },
    navBarClass: {
      flex: '0 0 auto',
      position: 'sticky',
      top: 0,
      zIndex: 1001,
    },
    multiSearchContainer: {
      marginLeft: -28,
      paddingLeft: 28,
      height: 86,
      maxWidth: 'calc(100% + 28px)',
      flexBasis: 'calc(100% + 28px)',
      background: '#e5ebee',
    },
    pageAlignLeft: {
      marginLeft: 28,
    },
    noMarginLeft: {
      marginLeft: 0,
    },
    listviewToolbarAlign: {
      marginLeft: -28,
      // top: 10,
    },
    treemapToolbarAlign: {
      // top: 10,
    },
    treemapClass: {
      flex: '1',
      height: '100%',
      width: '100%',
    },
    separator: {
      display: 'flex',
      alignItems: 'center',
      '& span': {
        display: 'flex',
        alignItems: 'center',
        '& svg': {
          opacity: 0.54,
        },
      },
    },
    currentButton: {
      '&:disabled': {
        color: '#000',
      },
    },
    previewDrawerClass: {
      position: 'absolute',
      top: drawerHeightOffset,
      zIndex: 3000,
      right: '0px',
      display: 'flex',
      flexFlow: 'row',
      height: `calc(100vh - ${drawerHeightOffset}px)`,
    },
    breadCrumbCurrent: {
      fontWeight: 'bold',
    },
    toolbarWrapper: {
      fontSize: '14pt',
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      background: '#fff',
      marginRight: 0,
      // marginTop: 12,
      // marginBottom: 2,
      height: 50,
      zIndex: 299,
      // width: 'calc(100vw - 72px)',
      position: 'relative',
      boxShadow:
        '0px 3px 3px -2px rgba(0,0,0,0.15), 0px 3px 4px 0px rgba(0,0,0,0.14), 0px 1px 8px 0px rgba(0,0,0,0.12)',
    },
    toolbar: {
      display: 'flex',
      alignItems: 'center',
      flex: 1,
      height: '100%',
    },
    rowViewerLaunchButton: {
      margin: 8,
      textTransform: 'lowercase',
    },
    rowViewerLaunchButtonIcon: {
      marginRight: 8,
      width: '0.75em',
      height: '0.75em',
    },
    loading: {
      position: 'fixed',
      left: 0,
      right: 0,
      top: 0,
      bottom: 0,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
    },
  })

const useStyles = makeStyles(styles)

const UniverseTreemap = (props) => {
  const classes = useStyles()
  const d3Container = useRef(null)
  const container = useRef(null)
  const multisearchService = new MultisearchService()
  const filterService = new TreemapFilterService()

  const onTreemapCellClick = (selectedDrilldown: SelectedDrilldown) => {
    if (!selectedDrilldown) return
    props.updateSelectedDrilldownWithRefresh(selectedDrilldown)
  }

  const onBreadCrumbClick = (
    e?: React.MouseEvent,
    selectedDrilldown?: SelectedDrilldown
  ) => {
    if (e) e.preventDefault()
    if (!selectedDrilldown) return
    const { rowViewer } = props
    if (rowViewer && rowViewer?.previousViewMode) {
      const prevView = rowViewer.previousViewMode
      props.closeRowViewer(prevView)
    }
    props.updateSelectedDrilldownWithRefresh(selectedDrilldown)
  }

  const renderTreemap = () => {
    if (d3Container.current) {
      checkAndSetGlobalDatasets(props)
      const filteredTreemapData = buildFilteredData()
      const highlightItems = buildHighlightItems()
      const metadata = buildMetadata()
      treemap(
        d3Container.current,
        filteredTreemapData,
        metadata,
        props.selectedDrilldown,
        onTreemapCellClick,
        highlightItems
      )
    }
  }

  const buildFilteredData = () => {
    if (selectedDrilldown && selectedDrilldown.isColumnView()) {
      if (props.columnData) {
        // filteredTreemapData.children[0].children = props.columnData
        return { children: cloneDeep(props.columnData) }
      }
    }
    const hasSearchFilter =
      !isNull(props.searchFilter) && !isEmpty(props.searchFilter.elements)
    const searchFilter = hasSearchFilter ? props.searchFilter : undefined
    const filteredTreemapData =
      filterService.generateContextSensitiveFilteredTreemapData(
        cloneDeep(props.treemapData),
        searchFilter,
        props.selectedDrilldown
      )
    return filteredTreemapData
  }

  const buildMetadata = () => {
    const isDatasetView =
      isNull(props.selectedDrilldown) || props.selectedDrilldown.isDatasetView()
    // if dataset view, use the global dataset metadata
    // if table or column view, use the correct metadata
    if (isDatasetView) {
      return props.datasetsMeta
    } else if (props.selectedDrilldown.isTableView()) {
      return props.tableMetadata
    } else if (
      props.selectedDrilldown.isColumnView() ||
      props.selectedDrilldown.isSpecificColumnView()
    ) {
      return props.columnMetadata
    } else {
      console.log(
        'unknown view: cannot determine metadata moving on...',
        props.selectedDrilldown
      )
      return []
    }
  }

  const buildHighlightItems = () => {
    // const isDatasetView =
    //   isNull(props.selectedDrilldown) || props.selectedDrilldown.isDatasetView()
    // if dataset, use only the hover values
    // if table or column view use hover and search filter values
    // pass in selectedDrilldown to filter on ONLY items
    //  in selected dataset, table, and column
    const buildForTableOrColumn = () => {
      // const value = 'highlighted + clicked search results'
      const value = props.searchFilter?.value
        ? props.searchFilter?.value
        : props.hoveredValue?.value
      const result = multisearchService.sumAllResults(
        [props.hoveredValue, props.searchFilter],
        value
      )
      const drilldownWithNoColumns = SelectedDrilldown.from(
        props.selectedDrilldown
      )
      drilldownWithNoColumns.column = undefined
      result.elements = multisearchService.optionalFilterElements(
        result.elements,
        drilldownWithNoColumns
      )
      return result
    }
    return buildForTableOrColumn()
  }

  const generateToolbar = (
    selectedDrilldown: SelectedDrilldown = new SelectedDrilldown(),
    props: any,
    classes: any
  ) => {
    const { selectedView, rowViewer } = props
    const toolbarState = calcView(props)
    const { isTreemapView, isListView, isRowView } = toolbarState
    if (isTreemapView || isRowView) {
      const breadcrumb = isTreemapView
        ? selectedDrilldown
        : new SelectedDrilldown(rowViewer?.dataset, rowViewer?.table)
      return generateBreadcrumbToolbar(
        toolbarState,
        selectedView,
        breadcrumb,
        classes
      )
    } else if (isListView) {
      return generateListviewToolbar(classes)
    }
  }

  const generateListviewToolbar = (classes: any) => {
    return (
      <div
        className={`${classes.toolbarWrapper} ${classes.listviewToolbarAlign}`}>
        <div className={`${classes.toolbar} ${classes.pageAlignLeft}`}>
          <span> </span>
          <OrderBySelection></OrderBySelection>
        </div>
        <ViewModeSwitcher></ViewModeSwitcher>
      </div>
    )
  }

  const generateBreadcrumbToolbar = (
    toolbarState: SelectedViewState,
    selectedView: SelectedView,
    selectedDrilldown: SelectedDrilldown = new SelectedDrilldown(),
    classes: any
  ) => {
    const currentSelectedView = selectedView?.currentView()
    return (
      <div
        className={`${classes.toolbarWrapper} ${classes.treemapToolbarAlign}`}>
        <div className={`${classes.toolbar} ${classes.pageAlignLeft}`}>
          <Button
            className={`${
              !selectedDrilldown?.dataset ? classes.breadCrumbCurrent : ''
            }`}
            onClick={(e) => onBreadCrumbClick(e, new SelectedDrilldown())}>
            All Data
          </Button>
          {selectedDrilldown.dataset && (
            <span key={selectedDrilldown.dataset} className={classes.separator}>
              <span>
                <ArrowRightIcon />
              </span>
              <Button
                className={`${
                  !selectedDrilldown?.table ? classes.breadCrumbCurrent : ''
                }`}
                onClick={(e) =>
                  onBreadCrumbClick(
                    e,
                    SelectedDrilldown.of({ dataset: selectedDrilldown.dataset })
                  )
                }>
                {selectedDrilldown.dataset}
              </Button>
              {!selectedDrilldown?.table && <DatasetChip></DatasetChip>}
            </span>
          )}
          {selectedDrilldown.table && (
            <span key={selectedDrilldown.table} className={classes.separator}>
              <span>
                <ArrowRightIcon />
              </span>
              <Button
                className={`${
                  !selectedDrilldown?.column ? classes.breadCrumbCurrent : ''
                }`}
                onClick={(e) =>
                  onBreadCrumbClick(
                    e,
                    SelectedDrilldown.of({
                      dataset: selectedDrilldown.dataset,
                      table: selectedDrilldown.table,
                    })
                  )
                }>
                {selectedDrilldown.table}
              </Button>
              {!selectedDrilldown?.column && <TableChip></TableChip>}
            </span>
          )}
        </div>
        {!toolbarState?.isRowView && <ViewModeSwitcher></ViewModeSwitcher>}
        {selectedDrilldown.table && (
          <RowViewerLaunchButton
            dataset={selectedDrilldown.dataset}
            table={selectedDrilldown.table}
            launchedFrom={currentSelectedView}
          />
        )}
      </div>
    )
  }

  const generatePreviewDrawer = (
    selectedDrilldown: SelectedDrilldown = new SelectedDrilldown()
  ) => {
    let drawer
    if (selectedDrilldown.shouldShowAddComment()) {
      drawer = <AddCommentDrawer></AddCommentDrawer>
    } else if (selectedDrilldown.shouldShowComments()) {
      drawer = <CommentListDrawer></CommentListDrawer>
    } else if (selectedDrilldown.isEmpty()) {
      drawer = <AllDatasetDrawer></AllDatasetDrawer>
    } else if (selectedDrilldown.isTableView()) {
      drawer = <DatasetTableDrawer></DatasetTableDrawer>
    } else if (selectedDrilldown.isColumnView()) {
      drawer = <TableColumnDrawer></TableColumnDrawer>
    } else {
      drawer = <h3>No drawer loaded</h3>
    }
    return <PreviewDrawer>{drawer}</PreviewDrawer>
  }

  const onWindowResize = () => renderTreemap()

  const { selectedDrilldown, isInFlight } = props
  // required to guess the treemap size
  //  it will expand to fit the window, but will display nothing without a guess
  const guessTreemapSize = 600
  const {
    isTreemapView,
    isListView,
    isRowView,
    isTreemapLaunchedRowView,
    isListViewLaunchedRowView,
  } = calcView(props)
  const refreshKey = updateStateKey(props)
  // console.log(refreshKey)

  useLayoutEffect(() => {
    window.addEventListener('resize', onWindowResize)
    renderTreemap()
    return () => {
      window.removeEventListener('resize', onWindowResize)
    }
    // }, [container.current, d3Container.current])
  }, [refreshKey, container.current, d3Container.current])

  const rowViewProps = isRowView ? genRowviewerProps(props) : {}

  return (
    <div
      className={`${classes.root} ${
        isTreemapView || isRowView ? classes.treemapViewRootLeft : ''
      }`}>
      <Grid container className={classes.navBarClass}>
        <Grid
          container
          direction="row"
          item
          xs={12}
          sm={11}
          className={`${classes.multiSearchContainer} ${
            isTreemapView || isRowView ? classes.noMarginLeft : ''
          }`}>
          <Grid item xs={11}>
            <div style={{ display: 'flex', height: '100%' }}>
              <MultiSearch></MultiSearch>
            </div>
          </Grid>
        </Grid>
        {(isTreemapView ||
          isTreemapLaunchedRowView ||
          isListView ||
          isListViewLaunchedRowView) && (
          <Grid item xs={12} sm={12}>
            {generateToolbar(selectedDrilldown, props, classes)}
          </Grid>
        )}
      </Grid>
      {isTreemapView && (
        <div className={classes.previewDrawerClass}>
          {generatePreviewDrawer(selectedDrilldown)}
        </div>
      )}
      <div key={refreshKey} ref={container}>
        {isInFlight ? (
          <div className={classes.loading}>
            <CircularProgress />
          </div>
        ) : (
          <>
            {isTreemapView && (
              <div
                style={{ height: guessTreemapSize, width: guessTreemapSize }}
                className={classes.treemapClass}
                ref={d3Container}>
                Loading...
              </div>
            )}
            {isListView && <ListView></ListView>}
            {isRowView && <RowViewer {...rowViewProps}></RowViewer>}
          </>
        )}
      </div>
    </div>
  )
}

const genRowviewerProps = (props: any): any => {
  let rowviewerHits
  // if rowviewer has search terms, then assume we did a multisearch
  //   and have a chip or hovered value selected
  const rowviewerProps = {
    searchColInfo: {},
    searchTerms: [],
    ...props.rowViewer,
    treemap: {
      ...props.treemap,
    },
  }
  const { searchFilter, hoveredValue } = props
  const hits = searchFilter || hoveredValue
  if (hits && hits.value) {
    const rowviewerSearchTermAdapter = new RowviewerSearchTermAdapter()
    rowviewerHits = rowviewerSearchTermAdapter.convertAll(
      props.rowViewer?.dataset,
      props.rowViewer?.table,
      hits
    )
    rowviewerProps['searchColInfo'] = rowviewerHits
    rowviewerProps['searchTerms'] = [hits?.value]
  }
  return rowviewerProps
}

const calcView = (props: any): SelectedViewState => {
  const { selectedView, rowViewer } = props
  return selectedView?.calcToolbarState(rowViewer)
}

const updateStateKey = (props: Record<string, any>): string => {
  const { selectedView } = props
  const { searchFilter, hoveredValue } = props
  let keys
  switch (selectedView?.view) {
    case SELECTED_VIEW_ENUM.LIST:
      const listViewDrilldown = props?.listViewDrilldown
      const listViewKeys = {
        selectedView,
        listViewDrilldown,
        searchFilter: searchFilter?.value,
        hoveredValue: hoveredValue?.value,
      }
      keys = md5(JSON.stringify(listViewKeys))
      break
    case SELECTED_VIEW_ENUM.ROW_VIEWER:
      const rowViewer = props?.rowViewer
      const rowViewKeys = {
        selectedView,
        dataset: rowViewer?.dataset,
        table: rowViewer?.table,
        // searchFilter: searchFilter?.value,
        // hoveredValue: hoveredValue?.value,
      }
      keys = md5(JSON.stringify(rowViewKeys))
      break
    case SELECTED_VIEW_ENUM.TREEMAP:
      //  TODO: FIX me: i had trouble being smart about treemap updates
      //  if i used just the values needed,then i missed refreshes
      //  so instead i use a new key everytime, which does more work on the browser
      //  but guarantees we dont miss state refreshes
      // just brute force a new key
      keys = v4()
      break
    default:
      console.log(
        'unknown selected view, generating brute force key and moving on...',
        selectedView
      )
      // just brute force a new key
      keys = v4()
  }

  // const jsonState = JSON.stringify(keys)
  // const hash = md5(jsonState)
  // console.log(JSON.parse(jsonState))
  // console.log(hash)
  return keys
}

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators(
    {
      updateSelectedDrilldownWithRefresh,
      setDatasetsMetadata,
      openRowViewer,
      setSelectedView,
      setListViewDrilldown,
      closeRowViewer,
    },
    dispatch
  )

const mapStateToProps = (state: StoreState) => ({
  treemap: state.treemap,
  treemapData: state.treemap.treemapData,
  searchFilter: state.treemap.filter,
  hoveredValue: state.treemap.hoveredValue,
  selectedDrilldown: getSelectedDrilldownForTreeMap(state),
  selectedView: getSelectedView(state),
  tableMetadata: state.treemap.selectedDatasetTableMetadata,
  columnMetadata: state.treemap.selectedTableColumnMetadata,
  columnData: state.treemap.selectedColumnData,
  datasetsMeta: state.previewdrawer.datasetsMeta,
  rowViewer: state.treemap.rowViewer,
  listViewDrilldown: state?.drilldown,
  isInFlight: state.treemap.isSearchInFlight,
})

const connector = connect(mapStateToProps, mapDispatchToProps)

export default TreemapInitialize(
  dplib.DPContext(customWithLoadingSpinner(connector(UniverseTreemap)))
)
