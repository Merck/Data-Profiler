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
import { RowViewerContext } from './store'
import { makeStyles } from '@material-ui/core/styles'

const useStyles = makeStyles({
  container: {
    textAlign: 'center',
    marginTop: 8,
    marginBottom: 8,
  },
  columnName: {
    fontSize: '1rem',
    color: '#111',
    padding: 8,
  },
  columnHasSearchMatches: {
    backgroundColor: 'rgba(135,206,250,0.5)',
  },
  type: {
    fontSize: '0.75rem',
    color: '#666',
  },
})

const ColumnHeader = (props) => {
  const { types, searchColInfo } = useContext(RowViewerContext)[0]
  const classes = useStyles()

  const currentColumnName = props.columnName
    ? props.columnName.trim().toLowerCase()
    : ''
  const hasSearchMatch = searchColInfo[currentColumnName]

  return (
    <div className={classes.container}>
      <div
        className={`${classes.columnName} ${
          hasSearchMatch ? classes.columnHasSearchMatches : ''
        }`}>
        {props.columnName}
      </div>
      <div className={classes.type}>{types[props.columnName]}</div>
    </div>
  )
}

export default ColumnHeader
