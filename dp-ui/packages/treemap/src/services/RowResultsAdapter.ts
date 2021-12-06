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
import { isEmpty } from 'lodash'
import ColumnCounts from '../models/ColumnCounts'
import Row from '../models/rows/Row'
import RowResults from '../models/rows/RowResults'
import ValueCount from '../models/ValueCount'

export type RowResultsEndpointShape = {
  rows: Array<any>
  columns: Record<string, Record<string, number>>
  sortedColumns: Array<string>
  endLocation: string
  statistics: Record<string, Record<string, number>>
  count: number
}

export default class RowResultsAdapter {
  endpointToRowResults(data: Readonly<RowResultsEndpointShape>): RowResults {
    if (!data) {
      return {
        rows: [],
        columns: [],
        sortedColumns: [],
        endLocation: '',
        statistics: {},
        count: 0,
      }
    }

    const rows = this.convertAllRows(data?.rows)
    const columns = this.convertColumns(data?.columns)
    return {
      ...data,
      rows,
      columns,
    }
  }

  convertAllRows(rows: Readonly<any[]>): Row[] {
    if (!rows || isEmpty(rows)) {
      return []
    }

    return rows?.map(this.convertRow)
  }

  convertRow(row: Readonly<any>): Row {
    const columns = Object.keys(row)
    const columnValues = columns?.map((column) => ({
      column,
      value: row[column],
    }))
    return {
      columns: columnValues,
    }
  }

  convertColumns(
    columns: Readonly<Record<string, Record<string, number>>>
  ): Array<Partial<ColumnCounts>> {
    if (!columns || isEmpty(columns)) {
      return []
    }

    const columnNames = Object.keys(columns)
    const columnCounts = columnNames?.map((columnName) => {
      const columnRecord = columns[columnName]
      const valueKeys = Object.keys(columnRecord)
      const valueCounts = valueKeys?.map((valueKey) => {
        const count = columnRecord[valueKey]
        return {
          value: valueKey,
          count,
        } as ValueCount
      })
      return {
        column: columnName,
        values: valueCounts,
      } as ColumnCounts
    })
    return columnCounts
  }
}
