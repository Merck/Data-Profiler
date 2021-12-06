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
import Filter from './Filter'
import Table from './Table'
import ColumnHeader from './ColumnHeader'
import Header from './Header'

const useStyles = makeStyles({
  body: {
    overflowX: 'scroll',
    position: 'relative',
    backgroundColor: '#FFF',
  },
  fullscreenBody: {
    width: '100vw',
    overflowX: 'scroll',
    position: 'relative',
  },
  loading: {
    position: 'absolute',
    top: 0,
    left: 0,
    width: '100%',
    height: '100%',
    cursor: 'progress',
    zIndex: 100000,
    backgroundColor: 'rgba(0,0,0,0)',
  },
})

const RowViewer = ({ width, tableBodyHeightPx }) => {
  const [state] = useContext(RowViewerContext)
  const classes = useStyles()

  const { rows, sortedColumns, loading } = state

  const tableColumns = (sortedColumns || []).map((e) => ({
    Header: <ColumnHeader columnName={e} />,
    accessor: (d) => d[e],
    id: e,
    Filter: Filter,
  }))

  const width = width || '100%'

  return (
    <div>
      <Header />
      <div className={classes.body} style={{ width }}>
        {loading && <div className={classes.loading} />}
        <Table
          columns={tableColumns}
          data={rows}
          tableBodyHeightPx={tableBodyHeightPx}
        />
      </div>
    </div>
  )
}

export default RowViewer
