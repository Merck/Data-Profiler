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
import { IconButton, Menu, MenuItem } from '@material-ui/core'
import AccountCircleIcon from '@material-ui/icons/AccountCircle'
import { makeStyles } from '@material-ui/core/styles'
import PopupState, { bindTrigger, bindMenu } from 'material-ui-popup-state'
import { DPContext } from '@dp-ui/lib'

const useStyles = makeStyles((theme) => ({
  root: {
    position: 'fixed',
    top: 18,
    // right: theme.spacing(1),
    left: 'calc(100vw - 78px)', // use left to prevent shift when menu is opened and body scrollbar is removed
    zIndex: 2147483646,
  },
  profileBtn: {
    padding: 0,
    marginRight: 20,
    color: '#389D8F',
  },
  menu: {
    paddingTop: 26,
  },
}))

function FloatingMenu(props) {
  const classes = useStyles()
  const go = (path, popupState) => {
    popupState.close()
    props.router.navigate(path)
  }

  // className={classes.menu}
  return (
    <div className={classes.root}>
      <PopupState variant="popover" popupId="demo-popup-menu">
        {(popupState) => (
          <React.Fragment>
            <IconButton
              size="medium"
              aria-label="Menu"
              className={classes.profileBtn}
              {...bindTrigger(popupState)}>
              <AccountCircleIcon style={{ fontSize: 42 }} />
            </IconButton>
            <Menu
              classes={{
                paper: classes.menu,
              }}
              {...bindMenu(popupState)}>
              <MenuItem onClick={() => go(`/settings`, popupState)}>
                Settings
              </MenuItem>
              {props.dataprofiler.isAdmin && (
                <MenuItem onClick={() => go(`/settings/security`, popupState)}>
                  Admin: Access Control
                </MenuItem>
              )}
              {props.dataprofiler.isAdmin && (
                <MenuItem onClick={() => go(`/job_status`, popupState)}>
                  Job Status
                </MenuItem>
              )}
              <MenuItem onClick={() => go(`/logout`, popupState)}>
                Logout
              </MenuItem>
            </Menu>
          </React.Fragment>
        )}
      </PopupState>
    </div>
  )
}

export default DPContext(FloatingMenu)
