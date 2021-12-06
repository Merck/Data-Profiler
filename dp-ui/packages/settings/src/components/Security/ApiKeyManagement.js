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
import React from 'react'
import { bindActionCreators } from 'redux'
import {
  Typography,
  Table,
  TableCell,
  TableRow,
  TableHead,
  TableBody,
  IconButton,
  ExpansionPanel,
  ExpansionPanelSummary,
  ExpansionPanelDetails,
  Button,
  Grid,
  CircularProgress,
  TextField,
  List,
} from '@material-ui/core'
import { connect } from 'react-redux'
import { withStyles } from '@material-ui/core/styles'
import ExpandMoreIcon from '@material-ui/icons/ExpandMore'
import KeyIcon from '@material-ui/icons/Add'
import SecurityBreadcrumb from './SecurityBreadcrumb'
import PersonListItem from './PersonListItem'
import ApiDocsLinkButton from '../Settings/ApiDocsLinkButton'
import {
  getApiKeys,
  regenerateApiKeyForUser,
  deleteApiKeyFromUser,
  searchForUsers,
  createNewApiKeyForUser,
} from './actions'
import { debounce } from 'lodash'

const styles = (theme) => ({
  table: {
    width: '100%',
  },
  gutterTop: {
    marginTop: theme.spacing(3),
  },
  addSection: {
    width: '100%',
  },
  heading: {
    fontSize: theme.typography.pxToRem(18),
    fontWeight: theme.typography.fontWeightBold,
  },
})

const push = window.alert

class ApiKeyManagement extends React.Component {
  state = {
    initialized: false,
    showUsernames: [],
    searchValue: '',
    searchResults: [],
  }

  async componentDidMount() {
    await this.props.getApiKeys()
    this.setState({ initialized: true })
  }

  handleSearchChange = (e) => {
    const searchValue = e.target.value
    this.setState({ searchValue }, () => {
      if (searchValue && searchValue.length > 0) {
        this.debounceSearchUser(searchValue)
      }
    })
  }

  debounceSearchUser = debounce(async (searchQuery) => {
    this.setState({ searchResults: [] })
    const searchResults = await searchForUsers(searchQuery)
    this.setState({ searchResults })
  }, 300)

  render() {
    const { searchResults, searchValue } = this.state
    if (!this.state.initialized) return <CircularProgress />
    const { props } = this
    const { classes, apiKeys } = props
    return (
      <Grid container>
        <SecurityBreadcrumb currentLocation="Static API Keys" />
        <Grid item xs={12} className={classes.gutterTop}>
          <Typography>
            API Keys should be sent in a request header named{' '}
            <code>X-Api-Key</code>
          </Typography>
          <ApiDocsLinkButton />
        </Grid>
        <Grid item xs={12} className={classes.gutterTop}>
          <ExpansionPanel defaultExpanded>
            <ExpansionPanelSummary expandIcon={<ExpandMoreIcon />}>
              <Typography className={classes.heading}>
                Current API Keys
              </Typography>
            </ExpansionPanelSummary>
            <ExpansionPanelDetails>
              <Grid item xs={12}>
                <Table className={classes.table}>
                  <TableHead>
                    <TableRow>
                      <TableCell>Username</TableCell>
                      <TableCell>API Key</TableCell>
                      <TableCell>Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {apiKeys.map((row) => {
                      return (
                        <TableRow key={row.username}>
                          <TableCell>{row.username}</TableCell>
                          <TableCell>
                            {this.state.showUsernames.includes(row.username) ? (
                              row.token
                            ) : (
                              <Button
                                onClick={() =>
                                  this.setState({
                                    showUsernames: [
                                      ...this.state.showUsernames,
                                      row.username,
                                    ],
                                  })
                                }
                                size="small">
                                show
                              </Button>
                            )}
                          </TableCell>
                          <TableCell>
                            <Button
                              size="small"
                              onClick={() =>
                                props.regenerateApiKeyForUser(
                                  row.token,
                                  row.username
                                )
                              }>
                              Rotate
                            </Button>
                            <Button
                              size="small"
                              onClick={() =>
                                props.deleteApiKeyFromUser(row.token)
                              }>
                              Delete
                            </Button>
                          </TableCell>
                        </TableRow>
                      )
                    })}
                  </TableBody>
                </Table>
              </Grid>
            </ExpansionPanelDetails>
          </ExpansionPanel>
        </Grid>
        <Grid item xs={12} className={classes.gutterTop}>
          <ExpansionPanel>
            <ExpansionPanelSummary expandIcon={<ExpandMoreIcon />}>
              <Typography className={classes.heading}>
                Create API Key
              </Typography>
            </ExpansionPanelSummary>
            <ExpansionPanelDetails>
              <Grid item xs={12}>
                <TextField
                  label="Search for user ID"
                  fullWidth
                  value={searchValue}
                  onChange={this.handleSearchChange}
                  margin="normal"
                />
                <List dense>
                  {searchValue &&
                    searchValue.length > 0 &&
                    searchResults &&
                    (searchResults.length === 0 ? (
                      <CircularProgress />
                    ) : (
                      searchResults.map((user) => (
                        <PersonListItem
                          key={user}
                          user={user}
                          actions={
                            <IconButton
                              aria-label="Create"
                              onClick={() =>
                                props.createNewApiKeyForUser(user.username)
                              }>
                              <KeyIcon />
                            </IconButton>
                          }
                        />
                      ))
                    ))}
                </List>
              </Grid>
            </ExpansionPanelDetails>
          </ExpansionPanel>
        </Grid>
      </Grid>
    )
  }
}

const mapStateToProps = (state) => ({
  apiKeys: state.apiKeys,
})

const mapDispatchToProps = (dispatch) =>
  bindActionCreators(
    {
      getApiKeys,
      regenerateApiKeyForUser,
      deleteApiKeyFromUser,
      createNewApiKeyForUser,
    },
    dispatch
  )

export default withStyles(styles)(
  connect(mapStateToProps, mapDispatchToProps)(ApiKeyManagement)
)
