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
import { api } from '@dp-ui/lib'
import { encodeVariable } from '@dp-ui/lib/dist/helpers/strings'
import { aggregateSortAndTake } from '@dp-ui/lib/dist/helpers/autosuggest'
import { isEmpty } from 'lodash'
import { devOffline } from './devOffline'
import { DEV_OFFLINE } from '@dp-ui/parent/src/features'
import { setOptionalParamIfExists } from './helpers/ParamsHelper'
import ColumnCounts from './models/ColumnCounts'
import ColumnSample from './models/ColumnSample'
import MetadataHierarchy from './models/MetadataHierarchy'
import SelectedDrilldown from './drilldown/models/SelectedDrilldown'
import ColumnCountsAdapter from './services/ColumnCountsAdapter'
import ColumnSampleAdapter from './services/ColumnSampleAdapter'
import RowResultsAdapter from './services/RowResultsAdapter'

/**
 * TODO: Remove Me if not in use
 * @deprecated
 * @param filterTerm
 * @param dataset
 * @param table
 */
const loadFilteredSuggestions = (
  filterTerm: string,
  dataset?: string,
  table?: string
) => {
  return new Promise((resolve) => {
    const term = filterTerm.toLowerCase().trim()
    if (isEmpty(term)) {
      return resolve([])
    }

    const params = {
      term: [term],
      begins_with: true,
      limit: 1000,
    }
    setOptionalParamIfExists(params, 'dataset', dataset)
    setOptionalParamIfExists(params, 'table', table)
    return api()
      .post({ resource: 'search', postObject: params })
      .then((res) => resolve(aggregateSortAndTake(res.body)))
      .catch((err) => {
        console.log(err)
        resolve('Loading Failed')
      })
  })
}

/**
 * @param selectedDrilldown
 */
const fetchColumnSampleValues = (
  selectedDrilldown?: Partial<SelectedDrilldown>
): Promise<Array<Partial<ColumnSample>>> => {
  return new Promise((resolve) => {
    if (
      !selectedDrilldown ||
      !selectedDrilldown.dataset ||
      !selectedDrilldown.table
    ) {
      resolve([])
    }
    const { dataset, table } = selectedDrilldown
    const resource = `samples/${encodeVariable(
      selectedDrilldown.dataset.trim()
    )}/${encodeVariable(selectedDrilldown.table.trim())}`
    const promise = DEV_OFFLINE
      ? Promise.resolve(devOffline(resource))
      : api().get({ resource })
    promise
      .then((res) => {
        const adapter = new ColumnSampleAdapter()
        const samples = adapter.endpointToColumnSamples(
          res.body,
          dataset,
          table
        )
        resolve(samples)
      })
      .catch((err) => {
        console.log(err)
        resolve([])
      })
  })
}

const EMPTY_COL_COUNTS = {
  dataset: '',
  table: '',
  column: '',
  values: [],
}
const DEFAULT_COL_COUNT = 256
const fetchColumnCounts = (
  selectedDrilldown?: SelectedDrilldown,
  numRecords: number = DEFAULT_COL_COUNT
): Promise<Partial<ColumnCounts>> => {
  return new Promise((resolve) => {
    if (!selectedDrilldown) {
      return resolve(EMPTY_COL_COUNTS)
    }
    const { dataset, table, column } = selectedDrilldown
    if (!dataset || !table || !column) {
      return resolve({
        dataset,
        table,
        column,
        values: [],
      })
    }

    const postObject = {
      start: 0,
      end: numRecords,
      sort: 'CNT_DESC',
      normalized: false,
      dataset: selectedDrilldown.dataset,
      table: selectedDrilldown.table,
      column: selectedDrilldown.column,
    }
    const resource = `colcounts?dataset=${dataset}&table=${table}&column=${column}`
    api()
      .post({ resource, postObject })
      .then((res) => {
        const adapter = new ColumnCountsAdapter()
        const values = adapter.endpointToColumnCounts(
          res.body,
          dataset,
          table,
          column
        )
        resolve(values)
      })
      .catch((err) => {
        console.log(err)
        resolve(EMPTY_COL_COUNTS)
      })
  })
}

const EMPTY_ROWS_RESULT = {
  rows: [],
  columns: [],
  sortedColumns: [],
  endLocation: '',
  statistics: {},
  count: 0,
}
const fetchDatawaveRows = (
  selectedDrilldown?: SelectedDrilldown
): Promise<any> => {
  return new Promise((resolve) => {
    if (!selectedDrilldown) {
      return resolve(EMPTY_ROWS_RESULT)
    }
    const { dataset, table } = selectedDrilldown
    if (!dataset || !table) {
      return resolve(EMPTY_ROWS_RESULT)
    }

    const emptyReq = {
      dataset: null,
      table: null,
      column: null,
      filters: {},
      limit: 500,
      api_filter: false,
    }
    const postObject = {
      ...emptyReq,
      dataset,
      table,
    }
    const resource = 'data/rows'
    api()
      .post({ resource, postObject })
      .then((res) => {
        const adpater = new RowResultsAdapter()
        const rowResults = adpater.endpointToRowResults(res.body)
        return resolve(rowResults)
      })
      .catch((err) => {
        console.log(err)
        return resolve(EMPTY_ROWS_RESULT)
      })
  })
}

/**
 * metadata for a single given dataset and all its tables and columns
 *
 * @param dataset
 */
const fetchMetadataHierarchy = (
  datasetName: string
): Promise<MetadataHierarchy> => {
  return new Promise((resolve) => {
    const dataset = datasetName
    if (isEmpty(dataset)) {
      return resolve(undefined)
    }
    return api()
      .get({ resource: `metadata/${encodeVariable(dataset)}` })
      .then((resp) => resolve(resp.body))
      .catch((err) => {
        console.log('fetchMetadataHierarchy Failed')
        console.log(err)
        resolve(undefined)
      })
  })
}

export {
  loadFilteredSuggestions,
  fetchColumnSampleValues,
  fetchColumnCounts,
  fetchDatawaveRows,
  fetchMetadataHierarchy,
}
