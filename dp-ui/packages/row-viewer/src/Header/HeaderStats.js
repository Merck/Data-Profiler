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
import React, { useContext } from 'react'
import { isEmpty } from 'lodash'
import { RowViewerContext } from '../store'
import { makeStyles } from '@material-ui/core/styles'
import format from '@dp-ui/lib/dist/helpers/formatter'
import { CircularProgress } from '@material-ui/core'

const useStyles = makeStyles({
  cell: {
    padding: '0 7px',
    margin: 0,
    color: '#999',
  },
  table: {
    fontSize: 18,
    color: '#555',
  },
})

const HeaderStats = (props) => {
  const [state] = useContext(RowViewerContext)
  const classes = useStyles()

  const cols = state.sortedColumns
  if (!cols) return <span />
  const numColumns = format.niceifyNumber(cols.length)

  const { rows, totalRows, filters } = state
  const numRows = format.niceifyNumber((rows || []).length)

  const displayDenominator = isEmpty(filters)

  const numTotalRows = format.niceifyNumber(totalRows || 0)

  return (
    <div>
      <span className={`${classes.cell} ${classes.table}`}>
        {format.middleTruncate(state.table, 60)}
      </span>
      <span className={classes.cell}>|</span>
      <span className={classes.cell}>
        {`${numColumns} Column${cols.length === 1 ? '' : 's'}`}
      </span>
      <span className={classes.cell}>|</span>
      <span className={classes.cell}>
        {`${numRows}${
          displayDenominator ? `/ ${numTotalRows} ` : ' '
        }Rows Displayed`}
      </span>
      {!state.loading && !state.endLocation && (
        <React.Fragment>
          <span className={classes.cell}>|</span>
          <span className={classes.cell}>Entire Result Set Displayed</span>
        </React.Fragment>
      )}
      {state.loading && (
        <React.Fragment>
          <span className={classes.cell}>|</span>
          <span className={classes.cell}>
            <CircularProgress size={16} />
          </span>
        </React.Fragment>
      )}
    </div>
  )
}

export default HeaderStats
