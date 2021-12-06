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
/* eslint-disable @typescript-eslint/camelcase */

import React, { useState, useEffect } from 'react'
import { bindActionCreators } from 'redux'
import { connect } from 'react-redux'
import { orderBy } from 'lodash'
import {
  CircularProgress,
  Grid,
  Typography,
  Chip,
  ExpansionPanel,
  ExpansionPanelDetails,
  ExpansionPanelSummary,
  Tooltip,
} from '@material-ui/core'
import { withStyles } from '@material-ui/core/styles'
import RemoveIcon from '@material-ui/icons/Close'
import AddIcon from '@material-ui/icons/Add'
import ExpandMoreIcon from '@material-ui/icons/ExpandMore'
import SecurityBreadcrumb from './SecurityBreadcrumb'
import { getUser, createUpdateUser, removeAttributesFromUser } from './actions'
import { DPContext } from '@dp-ui/lib'

const styles = (theme) => ({
  chip: {
    margin: theme.spacing(1),
  },
  chipContainer: {
    display: 'flex',
    flexWrap: 'wrap',
    marginLeft: -theme.spacing(1),
  },
  header: {
    marginTop: theme.spacing(3),
  },
})

const sortLists = (source) =>
  orderBy(source, (e) => e.toLowerCase().trim(), 'asc').sort()

const UserManagement = (props) => {
  const [userInfo, setUserInfo] = useState(null)
  const { dataprofiler } = props
  const { setDPState } = dataprofiler
  useEffect(() => {
    getUser(props.match.params.username).then((val) => {
      setUserInfo(val)
    })
  }, [props.match.params.username])

  if (!userInfo) return <CircularProgress />

  const { first_name, last_name, position, username, attributes } = userInfo

  const attributeNames = (attributes || []).map((attr) => attr.value)

  const dataWhitelists = sortLists(props.activeDataWhitelists)
  const systemCapabilities = sortLists(props.activeSystemCapabilities)

  const add = (attribute) => {
    createUpdateUser(username, attribute).then((res) => {
      setUserInfo(res)
      setDPState('app', 'datasets', null)
    })
  }
  const remove = (attribute) => {
    removeAttributesFromUser(username, attribute).then((res) => {
      setUserInfo(res)
      setDPState('app', 'datasets', null)
    })
  }

  return (
    <Grid container>
      <SecurityBreadcrumb
        currentLocation={username}
        crumbs={[{ path: '/security', label: 'User Inspector' }]}
      />
      <Grid item xs={12} className={props.classes.header}>
        <Typography variant="h4">{`${first_name} ${last_name}`}</Typography>
        <Typography variant="h5" gutterBottom>
          {`${username} - ${position}`}
        </Typography>
      </Grid>
      <Grid item xs={12} className={props.classes.header}>
        <Typography variant="h6" gutterBottom>
          Data Access
        </Typography>
        <div className={props.classes.chipContainer}>
          {dataWhitelists.map((e) => {
            const positive = attributeNames.indexOf(e) > -1
            return (
              <Tooltip
                key={e}
                title={`${username} ${
                  positive ? 'DOES' : 'DOES NOT'
                } have access to ${e}`}>
                <Chip
                  label={e}
                  className={props.classes.chip}
                  color={positive ? 'primary' : undefined}
                  onDelete={positive ? () => remove(e) : () => add(e)}
                  deleteIcon={positive ? <RemoveIcon /> : <AddIcon />}
                />
              </Tooltip>
            )
          })}
        </div>
      </Grid>
      <Grid item xs={12}>
        <Typography variant="h6" gutterBottom>
          System Capabilities
        </Typography>
        <div className={props.classes.chipContainer}>
          {systemCapabilities.map((e) => {
            const positive = attributeNames.indexOf(e) > -1
            return (
              <Tooltip
                key={e}
                title={`${username} ${
                  positive ? 'DOES' : 'DOES NOT'
                } have access to ${e}`}>
                <Chip
                  label={e}
                  className={props.classes.chip}
                  color={positive ? 'primary' : undefined}
                  onDelete={positive ? () => remove(e) : () => add(e)}
                  deleteIcon={positive ? <RemoveIcon /> : <AddIcon />}
                />
              </Tooltip>
            )
          })}
        </div>
      </Grid>

      <Grid item xs={12} className={props.classes.header}>
        <ExpansionPanel>
          <ExpansionPanelSummary expandIcon={<ExpandMoreIcon />}>
            <Typography>Rules of Use API Raw Information</Typography>
          </ExpansionPanelSummary>
          <ExpansionPanelDetails>
            <code>{JSON.stringify(userInfo)}</code>
          </ExpansionPanelDetails>
        </ExpansionPanel>
      </Grid>
    </Grid>
  )
}

const mapStateToProps = (state) => ({
  activeDataWhitelists: state.activeDataWhitelists,
  activeSystemCapabilities: state.activeSystemCapabilities,
})

const mapDispatchToProps = (dispatch) => bindActionCreators({}, dispatch)

export default withStyles(styles)(
  connect(mapStateToProps, mapDispatchToProps)(DPContext(UserManagement))
)
