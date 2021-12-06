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
import React, { useState, useEffect, Fragment, useMemo } from 'react'
import { connect } from 'react-redux'
import { bindActionCreators } from 'redux'
import { CopyToClipboard } from 'react-copy-to-clipboard'
import { withStyles } from '@material-ui/core/styles'
import {
  Button,
  Checkbox,
  FormControlLabel,
  Grid,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@material-ui/core'
import ArrowDownwardIcon from '@material-ui/icons/ArrowDownward'
import ArrowUpwardIcon from '@material-ui/icons/ArrowUpward'
import moment from 'moment'
import { isEmpty, isUndefined } from 'lodash'
import {
  getAllJobs,
  getCurrentUsersJobs,
  getSearchedJobs,
  getOnlyFailedJobs,
} from './selectors'
import { loadJobs, doSearch, getJobLogs } from './actions'
import { DPContext } from '@dp-ui/lib'

const styles = (theme) => ({
  container: {
    margin: `7vh 3vw`,
    width: '85vw',
  },
  table: {
    marginTop: 25,
    width: '85vw',
  },
  cell: {
    padding: 2,
    minWidth: '80px',
    overflow: 'hidden',
    maxWidth: '15vw',
  },
  textField: {
    marginLeft: 0,
    marginTop: 0,
    marginRight: theme.spacing(1),
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    width: '88vw',
    alignItems: 'center',
  },
  searchButton: {
    lineHeight: 3.4,
    marginTop: 31,
    marginRight: theme.spacing(1),
  },
  searchBox: {
    display: 'inline-block',
    width: '50vw',
    marginRight: theme.spacing(1),
  },
})

function TableLoaderDestinationCell(props) {
  const { classes, row } = props
  if (isUndefined(row) || row.type !== 'tableLoad') {
    return <Fragment />
  }
  const details = row.details
  return (
    <TableCell className={classes.cell}>
      {
        <div>
          {details.datasetName} / {details.tableName} [
          {details.tableVisibilityExpression}]
        </div>
      }
    </TableCell>
  )
}

function TableLoaderSourceCell(props) {
  const { classes, row } = props
  if (isUndefined(row) || row.type !== 'tableLoad') {
    return <Fragment />
  }
  const details = row.details
  return (
    <TableCell className={classes.cell}>
      {
        <div>
          {details.sourceS3Bucket}/{details.sourceS3Key}
        </div>
      }
    </TableCell>
  )
}

function CommitDestinationCell(props) {
  const { classes, row } = props
  if (isUndefined(row) || row.type !== 'commit') {
    return <Fragment />
  }
  const details = row.details
  const { fullDatasetLoad } = details
  const loadType =
    fullDatasetLoad && (fullDatasetLoad === 'true' || fullDatasetLoad === true)
      ? '   (Full Dataset Load)'
      : ''

  const dataset = details.dataset || details.datasetName || ''
  return (
    <TableCell className={classes.cell}>
      {
        <div>
          {dataset} {loadType} [{details.datasetVisibilityExpression}]
        </div>
      }
    </TableCell>
  )
}

function CommitSourceCell(props) {
  const { classes, row } = props
  if (isUndefined(row) || row.type !== 'commit') {
    return <Fragment />
  }
  const details = row.details
  return (
    <TableCell className={classes.cell}>
      {<div>{details.metadataVersion}</div>}
    </TableCell>
  )
}

function SqlsyncDestinationCell(props) {
  const { classes, row } = props
  if (isUndefined(row) || row.type !== 'sqlsync') {
    return <Fragment />
  }
  const { jdbcConnection, downloads } = row?.details
  const visibilities = row?.details?.visibilities || []
  return (
    <Fragment>
      <TableCell variant="body" size="medium" className={classes.cell}>
        {jdbcConnection && jdbcConnection.url && (
          <p>
            <i>destination url:</i>
            {jdbcConnection.url}
          </p>
        )}
        {downloads &&
          downloads.map((download, i) => {
            return (
              <p key={`${download.dataset}-download-${i}`}>
                {download.dataset} / {download.table} / [
                {visibilities.join(',')}]
              </p>
            )
          })}
      </TableCell>
    </Fragment>
  )
}

function SqlsyncSourceCell(props) {
  const { classes, row } = props
  if (isUndefined(row) || row.type !== 'sqlsync') {
    return <Fragment />
  }
  const { downloads } = row?.details
  const visibilities = row?.details?.visibilities || []
  return (
    <TableCell className={classes.cell}>
      {downloads &&
        downloads.map((download, i) => {
          return (
            <p key={`${download.dataset}-download-${i}`}>
              {download.dataset} / {download.table} / [{visibilities.join(',')}]
            </p>
          )
        })}
    </TableCell>
  )
}

function GenericDestinationCell(props) {
  // destination and source info
  const { classes, row } = props
  if (isUndefined(row)) {
    return <Fragment />
  }
  const details = row.details
  return (
    <TableCell className={classes.cell}>
      {<pre>{JSON.stringify(details, undefined, '\t')}</pre>}
    </TableCell>
  )
}

function DetailsDestinationCellRender(props) {
  const row = props.row
  if (isUndefined(row) || isUndefined(row.type)) {
    return <Fragment />
  }
  switch (row.type) {
    case 'tableLoad':
      return (
        <TableLoaderDestinationCell {...props}></TableLoaderDestinationCell>
      )
    case 'sqlsync':
      return <SqlsyncDestinationCell {...props}></SqlsyncDestinationCell>
    case 'commit':
      return <CommitDestinationCell {...props}></CommitDestinationCell>
    default:
      return <GenericDestinationCell {...props}></GenericDestinationCell>
  }
}

function DetailsSourceCellRender(props) {
  const row = props.row
  if (isUndefined(row) || isUndefined(row.type)) {
    return <Fragment />
  }
  switch (row.type) {
    case 'tableLoad':
      return <TableLoaderSourceCell {...props}></TableLoaderSourceCell>
    case 'sqlsync':
      return <SqlsyncSourceCell {...props}></SqlsyncSourceCell>
    case 'commit':
      return <CommitSourceCell {...props}></CommitSourceCell>
    default:
      return (
        <TableCell className={props.classes.cell}>
          <Fragment />
        </TableCell>
      )
  }
}

const Jobs = (props) => {
  const [onlyMine, setOnlyMine] = useState(true)
  const [search, setSearch] = useState('')
  const [onlyFailed, setOnlyFailed] = useState(false)

  const sortedData = (items, config = null) => {
    const [sortConf, setSortConf] = useState(config)

    const sortedItems = useMemo(() => {
      const sortableItems = [...items]
      if (sortConf !== null) {
        sortableItems.sort((a, b) => {
          if (a[sortConf.key] < b[sortConf.key]) {
            return sortConf.direction === 'ascending' ? -1 : 1
          }
          if (a[sortConf.key] > b[sortConf.key]) {
            return sortConf.direction === 'ascending' ? 1 : -1
          }
          // a === b
          return 0
        })
      }
      return sortableItems
    }, [items, sortConf])
    const requestSort = (key) => {
      let direction = 'ascending'
      if (
        sortConf &&
        sortConf.key === key &&
        sortConf.direction === 'ascending'
      ) {
        direction = 'descending'
      }
      setSortConf({ key, direction })
    }
    return { items: sortedItems, requestSort, sortConf }
  }

  useEffect(() => {
    props.loadJobs()
  }, [])

  let jobs = onlyMine
    ? props.currentUserJobs
    : props.hasSearch
    ? props.searchedJobs
    : props.allJobs

  jobs = onlyFailed ? props.failedJobs : jobs

  const { items, requestSort, sortConf } = sortedData(jobs, {
    key: 'id',
    direction: 'descending',
  })

  return (
    <Grid
      container
      direction="column"
      justify="flex-start"
      alignItems="flex-start"
      className={props.classes.container}>
      <Grid item>
        <div className={props.classes.header}>
          <Typography variant="h4" gutterBottom>
            Job Status
          </Typography>
          <span>
            <Button
              variant="contained"
              color="primary"
              onClick={props.loadJobs}>
              Refresh
            </Button>
          </span>
        </div>
      </Grid>
      {props.dataprofiler.isAdmin && (
        <Grid item>
          {!onlyFailed && (
            <FormControlLabel
              control={
                <Checkbox
                  checked={onlyMine}
                  onChange={() => setOnlyMine(!onlyMine)}
                  value="onlyMine"
                  color="primary"
                />
              }
              label="Show only my jobs"
            />
          )}
          {!onlyMine && (
            <FormControlLabel
              control={
                <Checkbox
                  checked={onlyFailed}
                  onChange={() => setOnlyFailed(!onlyFailed)}
                  value="onlyFailed"
                  color="primary"
                />
              }
              label="Only failed jobs"
            />
          )}
        </Grid>
      )}
      {!onlyMine === !onlyFailed && props.dataprofiler.isAdmin && (
        <Grid item>
          <div className={props.classes.searchBox}>
            <TextField
              label="Search"
              placeholder="Search for anything inside the job"
              className={props.classes.textField}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              margin="normal"
              fullWidth
              variant="outlined"
            />
          </div>
          <Button
            variant="outlined"
            color="primary"
            size="small"
            onClick={() => props.doSearch(search)}
            className={props.classes.searchButton}>
            Search
          </Button>
          {props.hasSearch && (
            <Button
              variant="outlined"
              color="secondary"
              size="small"
              onClick={() => props.doSearch('')}
              className={props.classes.searchButton}>
              Clear
            </Button>
          )}
        </Grid>
      )}
      <Grid item>
        <Table className={props.classes.table}>
          <TableHead>
            <TableRow>
              <TableCell
                size="small"
                className={props.classes.cell}
                onClick={() => requestSort('id')}>
                Job ID
                {sortConf.key === 'id' &&
                  sortConf.direction === 'ascending' && <ArrowUpwardIcon />}
                {sortConf.key === 'id' &&
                  sortConf.direction === 'descending' && <ArrowDownwardIcon />}
              </TableCell>
              <TableCell
                className={props.classes.cell}
                onClick={() => requestSort('createdAt')}>
                Submission Date (UTC)
                {sortConf.key === 'createdAt' &&
                  sortConf.direction === 'ascending' && <ArrowUpwardIcon />}
                {sortConf.key === 'createdAt' &&
                  sortConf.direction === 'descending' && <ArrowDownwardIcon />}
              </TableCell>
              <TableCell
                className={props.classes.cell}
                onClick={() => requestSort('type')}>
                Type
                {sortConf.key === 'type' &&
                  sortConf.direction === 'ascending' && <ArrowUpwardIcon />}
                {sortConf.key === 'type' &&
                  sortConf.direction === 'descending' && <ArrowDownwardIcon />}
              </TableCell>
              <TableCell
                className={props.classes.cell}
                onClick={() => requestSort('status')}>
                Status
                {sortConf.key === 'status' &&
                  sortConf.direction === 'ascending' && <ArrowUpwardIcon />}
                {sortConf.key === 'status' &&
                  sortConf.direction === 'descending' && <ArrowDownwardIcon />}
              </TableCell>
              {!onlyMine && (
                <TableCell
                  className={props.classes.cell}
                  onClick={() => requestSort('creatingUser')}>
                  Username
                  {sortConf.key === 'creatingUser' &&
                    sortConf.direction === 'ascending' && <ArrowUpwardIcon />}
                  {sortConf.key === 'creatingUser' &&
                    sortConf.direction === 'descending' && (
                      <ArrowDownwardIcon />
                    )}
                </TableCell>
              )}
              <TableCell className={props.classes.cell}>
                Dataset / Table [Visibility]
              </TableCell>
              <TableCell className={props.classes.cell}>Source</TableCell>
              <TableCell className={props.classes.cell}>Copy</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {items.map((row) => (
              <TableRow key={row.id}>
                <TableCell className={props.classes.cell}>
                  <button onClick={() => getJobLogs(row.id, row.createdAt)}>
                    {row.id}
                  </button>
                </TableCell>
                <TableCell className={props.classes.cell}>
                  {moment(row.createdAt).format('MM/DD/YY HH:mm')}
                </TableCell>
                <TableCell className={props.classes.cell}>{row.type}</TableCell>
                <TableCell className={props.classes.cell}>
                  {row.status}
                </TableCell>
                {!onlyMine && (
                  <TableCell className={props.classes.cell}>
                    {row.creatingUser}
                  </TableCell>
                )}
                <DetailsDestinationCellRender
                  row={row}
                  {...props}></DetailsDestinationCellRender>
                <DetailsSourceCellRender
                  row={row}
                  {...props}></DetailsSourceCellRender>
                <TableCell className={props.classes.cell}>
                  <Tooltip
                    title="Copy all job info to clipboard"
                    placement="top">
                    <CopyToClipboard text={JSON.stringify(row)}>
                      <Button variant="outlined" color="primary" size="small">
                        Copy
                      </Button>
                    </CopyToClipboard>
                  </Tooltip>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Grid>
    </Grid>
  )
}

const mapStateToProps = (state) => ({
  allJobs: getAllJobs(state),
  currentUserJobs: getCurrentUsersJobs(state),
  searchedJobs: getSearchedJobs(state),
  hasSearch: !isEmpty(state.searchQuery),
  failedJobs: getOnlyFailedJobs(state),
})

const mapDispatchToProps = (dispatch) =>
  bindActionCreators({ loadJobs, doSearch }, dispatch)

export default withStyles(styles)(
  connect(mapStateToProps, mapDispatchToProps)(DPContext(Jobs))
)
