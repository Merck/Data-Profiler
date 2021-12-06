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
import { connect } from 'react-redux'
import { sortBy, uniqBy, orderBy } from 'lodash'
import {
  CircularProgress,
  Grid,
  IconButton,
  List,
  TextField,
  Typography,
} from '@material-ui/core'
import { withStyles } from '@material-ui/core/styles'
import DeleteIcon from '@material-ui/icons/Block'
import AddIcon from '@material-ui/icons/Add'
import ThumbUpIcon from '@material-ui/icons/ThumbUp'
import ThumbDownIcon from '@material-ui/icons/ThumbDown'
import SecurityBreadcrumb from './SecurityBreadcrumb'
import PersonListItem from './PersonListItem'
import {
  searchUsersWithAttribute,
  createUpdateUser,
  removeAttributesFromUser,
} from './actions'
import UserSearch from './UserSearch'

const styles = {
  card: {
    margin: '25px 0',
  },
  title: {
    fontSize: 16,
    textAlign: 'center',
    fontWeight: 'bold',
  },
  breadcrumb: {
    marginTop: 15,
  },
  breadcrumbSeparator: {
    margin: '0 10px',
  },
  breadcrumbClickable: {
    cursor: 'pointer',
  },
}

class WhitelistBuilder extends React.Component {
  state = {
    selectedSearch: '',
    selectedVals: [],
  }

  async componentDidMount() {
    const selectedVals = await searchUsersWithAttribute(
      this.props.match.params.listName
    )
    this.setState({ selectedVals })
  }

  changeSelectedSearchValue = (e) =>
    this.setState({ selectedSearch: e.target.value })

  filterSelectedSearchValues = () => {
    const { selectedVals, selectedSearch } = this.state
    const searchTest = selectedSearch.toLowerCase().trim()
    return sortBy(
      selectedVals.filter(
        (el) =>
          (el.username &&
            el.username.toLowerCase().trim().startsWith(searchTest)) ||
          (el.first_name &&
            el.first_name.toLowerCase().trim().startsWith(searchTest)) ||
          (el.last_name &&
            el.last_name.toLowerCase().trim().startsWith(searchTest))
      ),
      'username'
    )
  }

  addUserToAttribute = (user) => {
    const attribute = this.props.match.params.listName
    this.setState(
      {
        selectedVals: uniqBy(
          [...this.state.selectedVals, user],
          (el) => el.username
        ),
      },
      () => {
        createUpdateUser(user.username, attribute)
      }
    )
  }

  removeAttributeFromUser = (user) => {
    const attribute = this.props.match.params.listName
    this.setState(
      {
        selectedVals: this.state.selectedVals.filter(
          (el) => el.username !== user.username
        ),
      },
      () => {
        removeAttributesFromUser(user.username, attribute)
      }
    )
  }

  render() {
    const { props } = this
    const { selectedSearch, selectedVals } = this.state

    const filteredAvailable = this.filterSelectedSearchValues()
    const usernamesSelected = selectedVals.map((user) => user.username)
    return (
      <Grid container spacing={24}>
        <SecurityBreadcrumb
          currentLocation={props.match.params.listName}
          crumbs={[{ path: '/security', label: 'Security Management' }]}
        />
        <Grid item xs={12} sm={6}>
          <Grid container justify="center" direction="column">
            <ThumbUpIcon
              color="secondary"
              fontSize="large"
              style={{ margin: '8px auto' }}
            />
            <Typography className={props.classes.title} gutterBottom>
              Users with {props.match.params.listName}
            </Typography>
          </Grid>
          <TextField
            label="Search"
            fullWidth
            value={selectedSearch}
            onChange={this.changeSelectedSearchValue}
            margin="normal"
          />
          <List dense>
            {filteredAvailable && filteredAvailable.length > 0 ? (
              orderBy(
                filteredAvailable,
                (e) => e.username.toLowerCase().trim(),
                'asc'
              ).map((user) => (
                <PersonListItem
                  key={user.username}
                  user={user}
                  actions={
                    <IconButton
                      aria-label="Delete"
                      onClick={() => this.removeAttributeFromUser(user)}>
                      <DeleteIcon />
                    </IconButton>
                  }
                />
              ))
            ) : (
              <CircularProgress />
            )}
          </List>
        </Grid>
        <Grid item xs={12} sm={6}>
          <Grid container justify="center" direction="column">
            <ThumbDownIcon
              color="secondary"
              fontSize="large"
              style={{ margin: '8px auto' }}
            />
            <Typography className={props.classes.title} gutterBottom>
              Users without {props.match.params.listName}
            </Typography>
          </Grid>
          <UserSearch
            action={(user) => (
              <IconButton
                aria-label="Add"
                disabled={usernamesSelected.includes(user.username)}
                onClick={() => this.addUserToAttribute(user)}>
                <AddIcon />
              </IconButton>
            )}
          />
        </Grid>
      </Grid>
    )
  }
}

const mapStateToProps = (state) => ({
  loginWhitelist: state.requireLoginAttributeForAccess,
  numTotalUsers: state.numTotalUsers,
  attributes: state.attributes,
})

export default withStyles(styles)(connect(mapStateToProps)(WhitelistBuilder))
