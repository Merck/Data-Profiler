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
import AggregatedSearchResult from '../MultiSearch/models/AggregatedSearchResult'
import { uniqBy, cloneDeep, find, isUndefined, isEmpty } from 'lodash'
import TreemapObject from '../models/TreemapObject'
import SelectedDrilldown from '../drilldown/models/SelectedDrilldown'

export default class TreemapFilterService {
  /**
   * method will know how to filter the given treemap data based on filters given and current view
   *
   * @param {*} data - all the treemap data
   * @param {*} searchFilter - search result filter from filter textbox
   * @param {*} datasetFilter - optional - current dataset viewed (clicked on), or null if viewing all datasets
   * @return {*} filtered data
   */
  generateContextSensitiveFilteredTreemapData = (
    data: TreemapObject,
    searchFilter?: AggregatedSearchResult,
    selectedDrillDown: SelectedDrilldown = new SelectedDrilldown()
  ): TreemapObject => {
    if (!selectedDrillDown) {
      return data
    }

    // first pass filter to datasets or tables
    const drillDownData = this.filterDataOn(data, selectedDrillDown)
    if (!searchFilter) {
      return drillDownData
    }

    // second pass filter based on latest selected search results
    if (selectedDrillDown.isDatasetView()) {
      // no prior dataset filter, filter at dataset level
      return this.filterDatasets(drillDownData, searchFilter)
    } else if (selectedDrillDown.isTableView()) {
      // we are in a table view, filter at dataset level
      //  we should still show all the tables, search hits will be highlighted
      // return this.filterDatasets(drillDownData, searchFilter)
      return drillDownData
    } else {
      // we are in a column view, filter at table level
      //  we should still show all the columns, search hits will be highlighted
      return drillDownData
    }
  }

  /**
   * filter the datasets against the given selected search result(s)
   *
   * @param data
   * @param searchFilter
   */
  filterDatasets = (
    data: TreemapObject,
    searchFilter: AggregatedSearchResult
  ): TreemapObject => {
    if (!searchFilter || !searchFilter.elements) {
      return data
    }
    // filter at dataset level
    const uniqDatasetFilters = uniqBy(searchFilter.elements, 'dataset').map(
      (el) => (el.dataset || '').toLowerCase()
    )
    let datasets = cloneDeep(data.children).filter((el) =>
      uniqDatasetFilters.includes(el.name.toLowerCase())
    )
    datasets = datasets && datasets.length > 0 ? datasets : data.children
    const filteredTreemapData = {
      children: datasets,
    }
    return filteredTreemapData
  }

  /**
   * filter the tables against the given selected search result(s)
   *
   * the ui should still show all tables even with a searchFilter applied
   * @param data
   * @param datasetFilter
   * @param filter
   */
  filterTables = (
    data: TreemapObject,
    searchFilter: AggregatedSearchResult,
    selectedDrilldown: SelectedDrilldown
  ): TreemapObject => {
    if (!searchFilter || !searchFilter.elements || !selectedDrilldown) {
      return data
    }
    // filter at table level
    const uniqTableFilters = uniqBy(searchFilter.elements, 'table').map(
      (el: any) => (el.table || '').toLowerCase()
    )
    const dsFilter = (selectedDrilldown.dataset || '').toLowerCase()
    let tables = cloneDeep(data.children).map((el) => {
      const isCurDataset = el.name.toLowerCase().indexOf(dsFilter) > -1
      if (isCurDataset) {
        el.children = el.children.filter((el) =>
          uniqTableFilters.includes(el.name.toLowerCase())
        )
      }
      return el
    })
    tables = tables && tables.length > 0 ? tables : data.children
    const filteredTreemapData = {
      children: tables,
    }
    return filteredTreemapData
  }

  /**
   * filter given treemap data by the given selected drilldown
   * @param data
   * @param selectedDrillDown
   */
  filterDataOn(
    data: TreemapObject,
    selectedDrilldown: SelectedDrilldown
  ): TreemapObject {
    if (!data || !data.children || !selectedDrilldown) {
      return data
    }

    // we are in dataset view, show all datasets
    if (selectedDrilldown.isDatasetView()) {
      return data
    }

    let dataset
    // user clicked into a dataset
    if (selectedDrilldown.dataset) {
      const datasetName = selectedDrilldown.dataset
      const elements = data.children
      dataset = elements
        .filter((el) => el !== undefined)
        .filter((el) => el.name && el.name === datasetName)

      dataset = dataset[0].children.map((el) => {
        el.children = el.children || []
        el.size = el.size || 0
        if (isUndefined(el.totalSize) && !isEmpty(el.children)) {
          el.totalSize = el.children.reduce((accumulator, currentValue) => {
            return accumulator + currentValue
          }, 0)
        }
        return el
      })

      // users clicked into a table
      if (selectedDrilldown.table) {
        const tableName = selectedDrilldown.table
        dataset = dataset
          .filter((el) => el !== undefined)
          .filter((el) => el.name && el.name === tableName)
      }
    }

    return { children: dataset }
  }

  /**
   * lookup a sub treemap node based on the dataset name given
   * @param datasetName
   * @param data
   * @return a treemap node or undefined if non found to satisfy the given dataset name
   */
  lookupDatasetByDatasetName(
    datasetName: string,
    data: TreemapObject
  ): TreemapObject {
    if (!datasetName || !data || !data.children) {
      return data
    }
    const datasets = data.children
    const arr = datasets.filter((dataset) => dataset.name === datasetName)
    return arr && arr.length > 0 ? arr[0] : undefined
  }

  /**
   * lookup a sub treemap node based on the table name given
   * @param tableName
   * @param data
   * @return a treemap node or undefined if non found to satisfy the given table name
   */
  lookupDatasetByTableName(
    tableName: string,
    data: TreemapObject
  ): TreemapObject {
    if (!tableName || !data || !data.children) {
      return data
    }
    const datasets = data.children
    return datasets.find((el) =>
      find(el.children, (table) => table.name === tableName)
    )
  }
}
