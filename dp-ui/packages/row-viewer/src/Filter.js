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
import React, { Component } from 'react'
import AsyncSelect from 'react-select/async'
import { components } from 'react-select'
import { CircularProgress } from '@material-ui/core'
import makeAnimated from 'react-select/animated'
import { DPContext } from '@dp-ui/lib'
import { RowViewerContext } from './store'
import { isNil, isEmpty, get, find } from 'lodash'
import format from '@dp-ui/lib/dist/helpers/formatter'
import { setFilter } from './actions'
import AwesomeDebouncePromise from 'awesome-debounce-promise'
import AllIcon from '@material-ui/icons/TableChart'
import './assets/row-viewer.css'

const animatedComponents = makeAnimated()

const Option = (props) => {
  if (props.data.selectAll) {
    return <components.Option {...props} className="react-select-all-option" />
  }
  return props.data.bold ? (
    <components.Option {...props} className="react-select-bold-option" />
  ) : (
    <components.Option {...props} />
  )
}

const loadingEntry = [
  {
    label: <CircularProgress size={10} />,
    value: <CircularProgress size={10} />,
    disabled: true,
  },
]

const failedLoadingEntry = [
  { label: 'Loading Failed', value: null, disabled: true },
]

const rewriteLabelToNull = (val) => (isNil(val) || val === '' ? '<null>' : val)

const loadOptions = (
  api,
  term,
  dataset,
  table,
  column,
  hideCounts,
  defaultOptions
) => {
  if (term && term.length > 0) {
    return new Promise((resolve) => {
      const params = {
        term: [term.toLowerCase()],
        begins_with: false,
        substring_match: true,
        limit: 1000,
        dataset,
        table,
        column,
      }
      const postBody = {
        resource: 'search',
        postObject: params,
      }
      api
        .post(postBody)
        .then((data) => {
          const res = data.body
            .sort((a, b) => b.count - a.count)
            .map((ret) => ({
              value: ret.value,
              label: hideCounts
                ? ret.value
                : `${rewriteLabelToNull(ret.value)} (${format.niceifyNumber(
                    ret.count
                  )} values)`,
            }))
          resolve(res)
        })
        .catch(() => resolve(failedLoadingEntry))
    })
  } else {
    return new Promise((resolve) => resolve(defaultOptions))
  }
}

const loadOptionsDebounced = AwesomeDebouncePromise(loadOptions, 500)

class Filter extends Component {
  static contextType = RowViewerContext

  state = {
    inputValue: '',
    defaultOptions: loadingEntry,
    loadedFromApi: false,
    inFlight: false,
  }

  getRealDefaultOptions = (forceReload = false) => {
    const { api } = this.props
    const { dataset, table, searchColInfo, searchTerms } = this.context[0]
    const colName = this.props.column.id

    if (!this.state.loadedFromApi || forceReload) {
      this.setState({
        inFlight: true,
        loadedFromApi: false,
        defaultOptions: loadingEntry,
      })

      const params = {
        dataset,
        table,
        column: colName,
        start: 0,
        end: 25,
        sort: 'CNT_DESC',
      }

      const postBody = {
        resource: 'colcounts',
        postObject: params,
      }

      api
        .post(postBody)
        .then((res) =>
          searchColInfo[colName]
            ? [
                { selectAll: searchTerms.join(', ') },
                ...searchColInfo[colName],
                ...res.body,
              ]
            : res.body
        )
        .then((res) => {
          const defaultOptions = res.map((value) =>
            value.selectAll
              ? {
                  label: (
                    <span style={{ display: 'flex', alignItems: 'center' }}>
                      <AllIcon style={{ height: 16, width: 16, margin: 4 }} />
                      {`All values with "${value.selectAll}"`}
                    </span>
                  ),
                  value: value.selectAll,
                  selectAll: searchColInfo[colName],
                  bold: true,
                }
              : {
                  value: value.n,
                  label: `${rewriteLabelToNull(
                    value.n
                  )} (${format.niceifyNumber(value.c)} values)`,
                  count: value.c,
                  bold: Boolean(value.bold),
                }
          )
          this.setState({
            defaultOptions,

            loadedFromApi: true,
            inFlight: false,
          })
        })
        .catch(() =>
          this.setState({
            defaultOptions: failedLoadingEntry,
            loadedFromApi: true,
            inFlight: false,
          })
        )
    }
  }

  render() {
    const [reducerState, dispatch, getReducerState] = this.context
    const colName = this.props.column.id
    const { api } = this.props
    const anyFiltersApplied = Object.keys(reducerState.filters).length > 0
    const { dataset, table } = reducerState
    return (
      <AsyncSelect
        isMulti
        placeholder="Filter"
        styles={{
          menuPortal: (base) => ({ ...base, zIndex: 999999 }),
        }}
        menuPortalTarget={document.body}
        cacheOptions
        value={(reducerState.filters[colName] || []).map((e) => ({
          label: rewriteLabelToNull(e),
          value: e,
        }))}
        defaultOptions={
          anyFiltersApplied
            ? this.state.defaultOptions
                .filter((e) => !e.selectAll)
                .map((e) => ({
                  value: e.value,
                  label: rewriteLabelToNull(e.value),
                }))
            : this.state.defaultOptions
        }
        onFocus={this.getRealDefaultOptions}
        components={{ ...animatedComponents, Option }}
        onChange={(selectedVals) => {
          if (
            isNil(reducerState.filters[colName]) ||
            isEmpty(reducerState.filters[colName])
          ) {
            this.getRealDefaultOptions(true)
          }

          const hasSelectAll = find(selectedVals, (e) => e.selectAll)

          const valsToSet = hasSelectAll
            ? [
                ...selectedVals.filter((el) => !el.selectAll),
                ...hasSelectAll.selectAll.map((element) => ({
                  value: element.n,
                  label: element.n,
                })),
              ]
            : selectedVals

          setFilter(dispatch, getReducerState, colName, valsToSet, api)
        }}
        loadOptions={(term) =>
          loadOptionsDebounced(
            api,
            term,
            dataset,
            table,
            colName,
            anyFiltersApplied,
            this.state.defaultOptions
          )
        }
      />
    )
  }
}

export default DPContext(Filter)
