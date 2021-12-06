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
import { debounce } from 'lodash'
import { CircularProgress, Grid, List, TextField } from '@material-ui/core'
import { withStyles } from '@material-ui/core/styles'
import { DPContext } from '@dp-ui/lib'
import PersonListItem from './PersonListItem'
import { searchForUsers } from './actions'

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

class UserSearch extends React.Component {
  state = {
    availableSearch: '',
    availableVals: [],
    availableLoading: false,
  }

  changeSelectedSearchValue = (e) =>
    this.setState({ selectedSearch: e.target.value })

  changeAvailableSearchValue = (e) => {
    const availableSearch = e.target.value
    this.setState({ availableSearch, availableLoading: true }, () => {
      this.debounceSearchUser(availableSearch)
    })
  }

  debounceSearchUser = debounce(async (searchQuery) => {
    this.setState({ availableVals: [] })
    const nonNullSearch = searchQuery && searchQuery.length > 0
    if (nonNullSearch) {
      const availableVals = await searchForUsers(searchQuery)
      this.setState({ availableVals, availableLoading: false })
    } else {
      this.setState({ availableVals: [], availableLoading: false })
    }
  }, 300)

  render() {
    const { props } = this
    const { availableSearch, availableVals, availableLoading } = this.state

    return (
      <Grid container justify="center" direction="column">
        <Grid item>
          <TextField
            label="Search by ID"
            fullWidth
            value={availableSearch}
            onChange={this.changeAvailableSearchValue}
            margin="normal"
          />
          <List dense>
            {availableLoading && <CircularProgress />}
            {availableSearch &&
              availableSearch.length > 0 &&
              !availableLoading &&
              availableVals &&
              availableVals.length === 0 && <span>No results</span>}
            {!availableLoading &&
              availableVals &&
              availableVals.length > 0 &&
              availableVals.map((user) => (
                <PersonListItem
                  key={user.username}
                  user={user}
                  onRowClick={
                    typeof props.onRowClick === 'function' && props.onRowClick
                  }
                  actions={
                    typeof props.action === 'function' && props.action(user)
                  }
                />
              ))}
          </List>
        </Grid>
      </Grid>
    )
  }
}

export default DPContext(withStyles(styles)(UserSearch), {
  requireAdmin: true,
})
