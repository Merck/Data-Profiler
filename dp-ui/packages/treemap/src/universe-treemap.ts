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
import * as d3 from 'd3'
import { treemapSquarify } from 'd3'
import { get, isEmpty, isNil, isUndefined, sortBy } from 'lodash'
import moment from 'moment'
import './assets/UniverseTreemap.css'
import HighlightTokenizerService from './components/HighlightSentence/services/HighlightTokenizerService'
import { DATE_TIME_GROUP_FMT } from './dpDateFormats'
import CommonMetadata from './models/CommonMetadata'
import SelectedDrilldown from './drilldown/models/SelectedDrilldown'
import TreemapObject from './models/TreemapObject'
import AggregatedSearchResult from './MultiSearch/models/AggregatedSearchResult'
import AggregatedSearchResultElement from './MultiSearch/models/AggregatedSearchResultElement'
import { nicefyNumber } from './nicefyNumber'
import { isLowerRightNthQuadrant } from './quandrants'
import ReorderTreemapComparators from './services/ReorderTreemapComparators'
import {
  DEFAULT_CELL_COLOR,
  searchFrequencyColorRange,
} from './searchHitColors'

const isDebug = false

function treemap(
  div,
  data: TreemapObject,
  metaData: Readonly<Array<CommonMetadata>>,
  selectedDrilldown: SelectedDrilldown,
  onTreemapCellClickFn,
  highlightedValue: AggregatedSearchResult
) {
  if (!div || !data) return

  // full screen - nav bar - padding
  const WIDTH = window.innerWidth - 72
  // full screen - filter bar - bread crumb - padding
  const HEIGHT = window.innerHeight - 86 - 50
  const MIN_FONT_SIZE = 12
  const MAX_FONT_SIZE = 40
  const MIN_WIDTH_FOR_TITLE = 36
  const DATASET_BORDER_OPACITY = 0.65
  const CELL_PADDING = 3

  const calcHighlightedCells = (
    highlighted: AggregatedSearchResult
  ): Array<AggregatedSearchResultElement> => {
    if (isNil(highlighted) || isEmpty(highlighted)) {
      return []
    }
    const fieldName = !selectedDrilldown
      ? 'dataset'
      : selectedDrilldown.currentView()
    const sortedElements = sortBy(highlighted.elements || [], fieldName)
    return sortedElements || []
  }

  const highlightedCells = calcHighlightedCells(highlightedValue)
  // clean the div before we write a treemap to it
  Array.from(
    d3
      .selectAll(div.childNodes)
      // .transition()
      // .ease(d3.easeSin)
      // .duration(450)
      .nodes()
  ).forEach((el: any) => el.remove())

  const margin = { top: 0, right: 0, bottom: 0, left: 0 }
  const width = WIDTH - margin.left - margin.right
  const height = HEIGHT - margin.top - margin.bottom
  const treeDim = { width, height }
  const cssHoverKey = 'box-shadow'
  const cssHoverVal = '#389D8F 0px 0px 0px 3px inset'
  const containerDiv = d3.select(div).append('div').attr('class', 'container')

  const tree = d3
    .treemap()
    .tile(treemapSquarify)
    .size([treeDim.width, treeDim.height])
  const treeDiv = containerDiv
    .append('div')
    .attr('class', 'treemap')
    .style('position', 'relative')
    .style('width', `${treeDim.width}px`)
    .style('height', `${treeDim.height}px`)
    .style('left', margin.left + 'px')
    .style('top', margin.top + 'px')

  const comparators = new ReorderTreemapComparators()
  const scaledData = !selectedDrilldown.dataset
    ? comparators.convertToLogScale(data)
    : !selectedDrilldown.table
    ? comparators.convertTablesToLogScale(data)
    : comparators.convertColumnsToLogScale(data)
  const userComparator = comparators.compareByValue('asc')
  const root = d3
    .hierarchy(scaledData, (d) => d.children)
    // .sum((d) => d.size)
    // .sum((d) => d?.sizeBucket)
    .sort(userComparator)

  const firstLevelRoot = root.sum((d) => d.totalSize)
  // const firstLevelRoot = root.sum((d) => d.size)
  const secondLevelRoot = root.sum((d) => d.size)

  const backgroundColor = (d) => {
    // determine which field to check
    // we are looking for datasets or tables
    const fieldName = !selectedDrilldown
      ? 'dataset'
      : selectedDrilldown.currentView()
    const curValJsonPath = fieldName === 'dataset' ? 'parent.data' : 'data'
    const val = get(Object.assign(d), curValJsonPath)
    // check field against current name
    const currentGrouping = highlightedCells.filter(
      (el) => el[fieldName] === val.name
    )
    if (isUndefined(currentGrouping) || isEmpty(currentGrouping)) {
      // this cell did not have search matches
      return DEFAULT_CELL_COLOR
    }
    // sum count of all hits for this current cell, grouped at the level of dataset, table, or column
    const count = currentGrouping.reduce((memo, cur) => memo + cur.count, 0)
    // found a cell with search matches, highlight cell based on scale
    return searchFrequencyColorRange(count)
  }

  const titleFontSize = (d) => {
    const dynamicFontSize = Math.min(
      MAX_FONT_SIZE,
      (d.x1 - d.x0 - CELL_PADDING) * 0.08
    )
    return Math.max(dynamicFontSize, MIN_FONT_SIZE)
  }

  const onTitleMouseEnter = (d, i, nodes) => {
    d3.event.stopPropagation()
    const el = d3.select(nodes[i])
    const treemapEl = treeDiv.node()
    const mouseX = d3.event.pageX
    const mouseY = d3.event.pageY
    const boundingBox = treemapEl.getBoundingClientRect()
    const flipSnackbarToTop = isLowerRightNthQuadrant(
      boundingBox,
      mouseX,
      mouseY,
      12
    )
    el.style(cssHoverKey, cssHoverVal)
    const containerDiv = d3.select(div).select('.container')
    const curDrilldown = selectedDrilldown || new SelectedDrilldown()
    let { dataset, table, column } = curDrilldown
    if (!selectedDrilldown || selectedDrilldown.isDatasetView()) {
      dataset = d.data.name
    } else if (selectedDrilldown.isTableView()) {
      table = d.data.name
    } else if (selectedDrilldown.isColumnView()) {
      column = d.data.name
    }
    const snackbarDiv = generateSnackbarDiv(
      dataset,
      table,
      column,
      metaData,
      flipSnackbarToTop
    )
    const node: any = containerDiv.node()
    node.appendChild(snackbarDiv)
  }

  const onTitleMouseLeave = (d, i, nodes) => {
    d3.event.stopPropagation()
    const el = d3.select(nodes[i])
    el.style(cssHoverKey, '')
    d3.select(div).selectAll('.container .treemap-snackbar').remove()
  }

  const onDatasetClick = (d, i, nodes) => {
    d3.event.stopPropagation()
    if (!d || !d.data || !d.data.name) {
      return
    }
    const dataset = d.data.name
    onTreemapCellClickFn(SelectedDrilldown.of({ dataset }))
  }

  const onTableNameClick =
    (currentSelectedDrilldown?: SelectedDrilldown) => (d) => {
      d3.event.stopPropagation()
      if (!d || !d.data || !d.data.name) {
        return
      }
      const tableName = d.data.name
      const drilldown = SelectedDrilldown.from(currentSelectedDrilldown)
      drilldown.table = tableName
      onTreemapCellClickFn(drilldown)
    }

  const onColumnNameClick =
    (currentSelectedDrilldown?: SelectedDrilldown) => (d) => {
      d3.event.stopPropagation()
      if (!d || !d.data || !d.data.name) {
        return
      }
      const columnName = d.data.name
      const drilldown = SelectedDrilldown.from(currentSelectedDrilldown)
      drilldown.column = columnName
      onTreemapCellClickFn(drilldown)
    }

  // titles
  treeDiv
    .selectAll('titles')
    .data(
      tree(secondLevelRoot)
        .descendants()
        .filter((d) => d.depth === 1)
      // .filter((d) => d.depth <= 2)
    )
    .enter()
    .append('div')
    .attr('class', 'node-title')
    .style('left', (d) => d.x0 + 'px')
    .style('top', (d) => d.y0 + 'px')
    .style('width', (d) => Math.max(0, d.x1 - d.x0 - CELL_PADDING) + 'px')
    .style('height', (d) => Math.max(0, d.y1 - d.y0 - CELL_PADDING) + 'px')
    .style('position', 'absolute')
    .style('background', (d) => {
      if (!d.children) {
        return
      }
      return backgroundColor(d.children[0])
    })
    .style('opacity', DATASET_BORDER_OPACITY)

  const treeNodes = treeDiv.datum(root).selectAll('.node')

  // in case we put transitions back in
  // to transition in
  // .style('opacity', '0')
  // .transition()
  // .ease(d3.easeSin)
  // .duration(450)

  // nodes at depth 1 and 2 were broken up in an attempt to get the scale accurate rendered
  // ie the root level is not rendered in log scale when there are subcells eg the top level dataset view (because it did not render correctly)
  // sub level views do not show logscale and therefore render in logscale
  //
  // attempts failed but kept the layout like this to try again some other time
  //
  // root nodes cells at depth 1
  treeNodes
    .data(
      tree(firstLevelRoot)
        .leaves()
        .filter((d) => {
          return d?.depth <= 1
        })
    )
    .enter()
    .append('div')
    .attr('class', 'node')
    .style('left', (d) => d.x0 + 'px')
    .style('top', (d) => d.y0 + 'px')
    // TODO: if cell is too small, remove width and height borders
    .style('width', (d) => Math.max(0, d.x1 - d.x0 - CELL_PADDING) + 'px')
    .style('height', (d) => Math.max(0, d.y1 - d.y0 - CELL_PADDING) + 'px')
    .style('background-color', backgroundColor)
    .style('opacity', (d) => '1')

  // sub cell nodes cells at depth 2
  treeNodes
    .data(
      tree(secondLevelRoot)
        .leaves()
        .filter((d) => {
          return d?.depth > 1
        })
    )
    .enter()
    .append('div')
    .attr('class', 'node')
    .style('left', (d) => d.x0 + 'px')
    .style('top', (d) => d.y0 + 'px')
    // TODO: if cell is too small, remove width and height borders
    .style('width', (d) => Math.max(0, d.x1 - d.x0 - CELL_PADDING) + 'px')
    .style('height', (d) => Math.max(0, d.y1 - d.y0 - CELL_PADDING) + 'px')
    .style('background-color', backgroundColor)
    .style('opacity', (d) => '1')

  treeNodes
    .exit()
    .transition()
    .ease(d3.easeSin)
    .duration(450)
    .style('opacity', '0')
    .remove()

  // Node Title Text HTML elements
  //Dataset Title > MIN_FONT_SIZE
  treeDiv
    .selectAll('titles')
    .data(
      tree(secondLevelRoot)
        .descendants()
        // filters titles of subcells
        .filter((d) => d.depth === 1)
    )
    .enter()
    .append('div')
    .style('opacity', (d) => '1')
    .append('node-title-text')
    .style('left', (d) => d.x0 + 'px')
    .style('top', (d) => d.y0 + 'px')
    .style('width', (d) => Math.max(0, d.x1 - d.x0 - CELL_PADDING) + 'px')
    .style('height', (d) => Math.max(0, d.y1 - d.y0 - CELL_PADDING) + 'px')
    .style('line-height', (d) => Math.max(0, d.y1 - d.y0 - CELL_PADDING) + 'px')
    .style('position', 'absolute')
    .style('text-align', 'center')
    .style('overflow', 'hidden')
    .style('cursor', 'pointer')
    .style(cssHoverKey, '')
    .style('font-size', (d) => titleFontSize(d) + 'px')
    .style('color', 'rgba(0, 0, 0, .82)')
    .style('line-height', 1.5)
    .style('vertical-align', 'middle')
    .style('display', 'inline-block')
    .append('div')
    .style('display', 'flex')
    .style('height', '100%')
    .style('width', '100%')
    .style('justify-content', 'center')
    .style('flex-flow', 'column')
    .html((d: any) => {
      const shouldWriteText = Math.max(0, d.x1 - d.x0 - 1) > MIN_WIDTH_FOR_TITLE
      // && titleFontSize(d) > MIN_FONT_SIZE
      let name = d.data.name
      if (isDebug) {
        const normalizeSize = Number(
          d?.data?.sizeBucket || d?.data?.size || d?.data?.totalSize
        ).toFixed(2)
        name = `${d.data.name} (${normalizeSize})`
      }
      const highlightService = new HighlightTokenizerService()
      // const fieldName = !selectedDrilldown
      //   ? 'dataset'
      //   : selectedDrilldown.currentView()
      // const sortedElements = sortBy(
      //   highlightedValue.elements || [],
      //   fieldName
      // ).map<string>((el) => el[fieldName])
      const sortedElements = sortBy([highlightedValue.value])
      const matchTerms = highlightService.normalizeMatchTerms(sortedElements)
      const tokens = highlightService.tokenizeAndMatchLongestSubstring(
        name,
        matchTerms
      )
      const title = tokens?.reduce((memo, token) => {
        return token.isMatch
          ? `${memo}<span class='highlight'>${token?.token}</span>`
          : `${memo}<span>${token?.token}</span>`
      }, '')
      return shouldWriteText ? `<div>${title}</div>` : ''
    })

  // apply click handlers
  const nodes = treeDiv.selectAll('node-title-text')
  // const nodes = treeDiv.selectAll('node')
  const isDatasetView = !selectedDrilldown || selectedDrilldown.isDatasetView()
  if (isDatasetView) {
    nodes.on('click.node-title-text', onDatasetClick)
  } else if (selectedDrilldown.isTableView()) {
    nodes.on('click.node-title-text', onTableNameClick(selectedDrilldown))
  } else if (selectedDrilldown.isColumnView()) {
    nodes.on('click.node-title-text', onColumnNameClick(selectedDrilldown))
  }
  nodes.on('mouseenter.node-title-text', onTitleMouseEnter)
  nodes.on('mouseleave.node-title-text', onTitleMouseLeave)
}

function generateSnackbarDiv(
  datasetName: string,
  tableName: string,
  columnName: string,
  metaData: Readonly<Array<CommonMetadata>> = [],
  flipSnackbarToTop = false
): HTMLDivElement {
  const positionClazz = flipSnackbarToTop
    ? 'treemap-snackbar-flip-to-top'
    : 'treemap-snackbar-flip-to-bottom'
  const element =
    datasetName && !tableName
      ? filterMetadataOnDatasetName(metaData, datasetName)
      : filterMetadataOnTableName(metaData, tableName)
  const tableCount = element && element.numTables ? element.numTables : 'n/a'
  const columnCount = element && element.numColumns ? element.numColumns : 'n/a'
  const rowCount =
    element && element.numRows ? nicefyNumber(element.numRows) : 'n/a'
  const updatedOn = element && element.updatedOn ? element.updatedOn : '-1'
  const snackbar = document.createElement('div')
  const updatedOnDtgFormatted = moment
    .utc(updatedOn)
    .format(DATE_TIME_GROUP_FMT)
  snackbar.setAttribute(
    'class',
    'treemap-snackbar treemap-snackbar-show ' + positionClazz
  )
  const spacer = `<span class="snackbar-spacer">|</span>`
  const labelTemplate = (label: string) =>
    `<span class="snackbar-label">${label}</span>`
  const valueTemplate = (val) => `<span class="snackbar-value">${val}</span>`
  const snackbarTemplate = (label: string, val: string | number): string =>
    `${labelTemplate(label)}${valueTemplate(val)}${spacer}`
  const v0 = datasetName ? `${snackbarTemplate('Dataset', datasetName)}` : ''
  const v1 = tableName ? `${snackbarTemplate('Table', tableName)}` : ''
  const v1_1 = columnName ? `${snackbarTemplate('Column', columnName)}` : ''
  const v2 = `${snackbarTemplate('Updated On', updatedOnDtgFormatted)}`
  const v3 = `${snackbarTemplate('Tables', tableCount)}`
  const v4 = `${snackbarTemplate('Columns', columnCount)}`
  const v5 = `${labelTemplate('Rows')}${valueTemplate(rowCount)}`
  snackbar.innerHTML = `${v0}${v1}${v1_1}${v2}${v3}${v4}${v5}`
  return snackbar
}

function filterMetadataOnDatasetName(
  metaData: Readonly<Array<CommonMetadata>>,
  datasetName: string
): CommonMetadata {
  return metaData.find((el) => el.datasetName === datasetName)
}

function filterMetadataOnTableName(
  metaData: Readonly<Array<CommonMetadata>>,
  tableName: string
): CommonMetadata {
  return metaData.find((el) => el.tableName === tableName)
}

export { treemap }

export default treemap
