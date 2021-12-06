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
import React, { useEffect } from 'react'
import { connect } from 'react-redux'
import { bindActionCreators } from 'redux'
import { orderBy } from 'lodash'
import {
  Avatar,
  Button,
  Chip,
  ExpansionPanel,
  ExpansionPanelDetails,
  ExpansionPanelSummary,
  Grid,
  IconButton,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@material-ui/core'
import { withStyles } from '@material-ui/core/styles'
import ExpandMoreIcon from '@material-ui/icons/ExpandMore'
import ListIcon from '@material-ui/icons/List'
import LaunchIcon from '@material-ui/icons/Launch'
import UserSearch from './UserSearch'
import { DPContext } from '@dp-ui/lib'
import { loadWhitelists, getTotalNumberUsers, getApiKeys } from './actions'
import SettingsIcon from '@material-ui/icons/Settings'

const styles = (theme) => ({
  card: {
    margin: '25px 0',
  },
  heading: {
    fontSize: theme.typography.pxToRem(18),
    fontWeight: theme.typography.fontWeightBold,
  },
  root: {
    // width: '100%',
    marginTop: 25,
    marginBottom: 25,
    // padding: '0 28px'
  },
  chipContainer: {
    display: 'flex',
    flexWrap: 'wrap',
  },
  chip: {
    margin: theme.spacing(1),
  },
  icon: {
    marginLeft: 10,
  },
})

const Security = (props) => {
  useEffect(() => {
    props.loadWhitelists()
    props.getTotalNumberUsers()
    props.getApiKeys()
  }, [])

  const renderChips = (source) => (
    <div className={props.classes.chipContainer}>
      {orderBy(source, (e) => e.toLowerCase().trim(), 'asc')
        .sort()
        .map((element) => (
          <Chip
            key={element}
            avatar={
              <Avatar style={{ borderRadius: 5 }}>
                <ListIcon />
              </Avatar>
            }
            label={element}
            onClick={() =>
              props.router.navigate(`/settings/security/lists/${element}`)
            }
            className={props.classes.chip}
          />
        ))}
    </div>
  )

  return (
    <div className={props.classes.root}>
      <Typography variant="h4">
        Admin: Access Control
        <SettingsIcon className={props.classes.icon} />
      </Typography>
      <ExpansionPanel defaultExpanded>
        <ExpansionPanelSummary expandIcon={<ExpandMoreIcon />}>
          <Typography className={props.classes.heading}>
            Environment Information
          </Typography>
        </ExpansionPanelSummary>
        <ExpansionPanelDetails>
          <Table>
            <TableHead>
              <TableRow style={{ display: 'none' }}>
                <TableCell>Statistic</TableCell>
                <TableCell>Value</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              <TableRow>
                <TableCell>Login Scheme for Environment</TableCell>
                <TableCell>
                  {props.loginWhitelist
                    ? 'Access Control List Only'
                    : 'Any Authenticated Account'}
                </TableCell>
              </TableRow>
              <TableRow>
                <TableCell>Users with Access</TableCell>
                <TableCell>{props.numTotalUsers}</TableCell>
              </TableRow>
              <TableRow>
                <TableCell>Static API Keys active</TableCell>
                <TableCell>{props.apiKeys.length}</TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </ExpansionPanelDetails>
      </ExpansionPanel>
      <ExpansionPanel defaultExpanded>
        <ExpansionPanelSummary expandIcon={<ExpandMoreIcon />}>
          <Typography className={props.classes.heading}>
            Data Access Management
          </Typography>
        </ExpansionPanelSummary>
        <ExpansionPanelDetails>
          {renderChips(props.activeDataWhitelists)}
        </ExpansionPanelDetails>
      </ExpansionPanel>
      <ExpansionPanel defaultExpanded>
        <ExpansionPanelSummary expandIcon={<ExpandMoreIcon />}>
          <Typography className={props.classes.heading}>
            Environment Capability Management
          </Typography>
        </ExpansionPanelSummary>
        <ExpansionPanelDetails>
          {renderChips(props.activeSystemCapabilities)}
        </ExpansionPanelDetails>
      </ExpansionPanel>
      <ExpansionPanel>
        <ExpansionPanelSummary expandIcon={<ExpandMoreIcon />}>
          <Typography className={props.classes.heading}>
            User Inspector
          </Typography>
        </ExpansionPanelSummary>
        <ExpansionPanelDetails>
          <Grid container>
            <Grid item xs={12} sm={9}>
              <UserSearch
                onRowClick={(user) =>
                  props.router.navigate(
                    `/settings/security/user/${user.username}`
                  )
                }
                action={(user) => (
                  <IconButton
                    onClick={() =>
                      props.router.navigate(
                        `/settings/security/user/${user.username}`
                      )
                    }>
                    <LaunchIcon />
                  </IconButton>
                )}
              />
            </Grid>
          </Grid>
        </ExpansionPanelDetails>
      </ExpansionPanel>
      <ExpansionPanel>
        <ExpansionPanelSummary expandIcon={<ExpandMoreIcon />}>
          <Typography className={props.classes.heading}>
            Static API Keys
          </Typography>
        </ExpansionPanelSummary>
        <ExpansionPanelDetails>
          <Button
            variant="outlined"
            onClick={() => props.router.navigate('/settings/security/apiKeys')}>
            Manage
          </Button>
        </ExpansionPanelDetails>
      </ExpansionPanel>
    </div>
  )
}

const mapStateToProps = (state) => ({
  loginWhitelist: state.requireLoginAttributeForAccess,
  numTotalUsers: state.numTotalUsers,
  attributes: state.attributes,
  activeDataWhitelists: state.activeDataWhitelists,
  activeSystemCapabilities: state.activeSystemCapabilities,
  apiKeys: state.apiKeys,
})

const mapDispatchToProps = (dispatch) =>
  bindActionCreators(
    { loadWhitelists, getTotalNumberUsers, getApiKeys },
    dispatch
  )

export default DPContext(
  withStyles(styles)(connect(mapStateToProps, mapDispatchToProps)(Security)),
  { requireAdmin: true }
)
