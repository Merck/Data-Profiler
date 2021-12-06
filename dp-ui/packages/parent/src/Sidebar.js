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
import React, { useMemo } from 'react'
import { Toolbar, Tabs, Tab, Tooltip } from '@material-ui/core'
import { makeStyles } from '@material-ui/core/styles'
import DPSearchIcon from './components/icons/DPSearchIcon'
import DPInfoIcon from './components/icons/DPInfoIcon'
import DPApplicationIcon from './components/icons/DPApplicationIcon'
import { DPContext } from '@dp-ui/lib'
import { get, findIndex } from 'lodash'
import { NewDPLogo } from '@dp-ui/lib/dist/images'
import { useLocation } from 'react-router-dom'

// We have to map the content in an array like this because we want a tooltip on the tab bar
const sideBarContent = [
  { path: '/treemap', icon: <DPSearchIcon />, description: 'All Data' },
  {
    path: '/apps',
    icon: <DPApplicationIcon />,
    description: 'All Applications',
  },
  { path: '/landing', icon: <DPInfoIcon />, description: 'About Us' },
]

const useStyles = makeStyles({
  root: {
    display: 'flex',
    flexDirection: 'column',
    backgroundColor: '#fff',
    alignItems: 'center',
    width: 72,
    // margin: -8,
    // paddingTop: 8,
    position: 'fixed',
    top: 0,
    left: 0,
    bottom: 0,
    zIndex: 1,
  },
  toolBar: {
    display: 'block',
  },
  toggleTab: {
    width: 72,
    display: 'flex',
    minWidth: 0,
    alignItems: 'center',
    justifyContent: 'center',
  },
  flexContainer: {
    display: 'block',
  },
  tab: {
    // width: 72,
    height: 49,
  },
  tabBorders: {
    position: 'relative',
    borderTop: '1px solid #E5EBEE',
    '&:last-child': {
      borderBottom: '1px solid #E5EBEE',
    },
  },
  selectedToggleTab: {
    backgroundColor: '#f7f7f7',
    borderRadius: 0,
    position: 'relative',
  },
  selectedToggleIndicator: {
    backgroundColor: '#00857C',
    width: 5,
    height: 49,
    position: 'absolute',
    top: 0,
    right: 0,
    zIndex: 999999,
  },
  logoContainer: {
    // width: 35,
    // margin: 4,
    // marginTop: 14,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    // paddingBottom: 20,
    height: 86,
  },
  logo: {
    width: 35,
    // height: 35,
    '&:hover': {
      cursor: 'pointer',
    },
  },
  tooltipPopper: {
    zIndex: 999999,
  },
  tabContainer: {
    width: 72,
  },
})

function Entry(props) {
  const { path, icon, description, nav, selected } = props
  const classes = useStyles()
  return (
    <Tooltip
      title={description}
      placement="right"
      classes={{ popper: classes.tooltipPopper }}>
      <div className={classes.tabBorders}>
        {selected && <div className={classes.selectedToggleIndicator} />}
        <Tab
          value={path}
          icon={icon}
          selected={selected}
          onClick={() => nav(path)}
          classes={{ root: classes.tab, selected: classes.selectedToggleTab }}
          className={classes.toggleTab}
        />
      </div>
    </Tooltip>
  )
}

function Sidebar(props) {
  const classes = useStyles()
  const { pathname } = useLocation()
  const currentValue = useMemo(
    () => findIndex(sideBarContent, (e) => e.path === pathname) || 0,
    [pathname]
  )
  return (
    <div className={classes.root}>
      <div className={classes.logoContainer}>
        <Tooltip
          title="Data Profiler"
          placement="right"
          classes={{ popper: classes.tooltipPopper }}>
          <img
            alt="Data Profiler"
            src={NewDPLogo}
            className={classes.logo}
            onClick={() => props.router.navigate('/')}
          />
        </Tooltip>
      </div>
      {get(props, 'dataprofiler.state.session.authToken') && (
        <Toolbar disableGutters={true} className={classes.toolBar}>
          {/* We don't use the regular mui tab onChange handling due to the tooltips wrapping the tabs */}
          <Tabs
            TabIndicatorProps={{ style: { display: 'none' } }}
            value={currentValue}
            classes={{
              root: classes.tabContainer,
              flexContainer: classes.flexContainer,
            }}>
            {sideBarContent.map((e, ix) => (
              <Entry
                key={e.description}
                selected={currentValue === ix}
                nav={props.router.navigate}
                {...e}
              />
            ))}
          </Tabs>
        </Toolbar>
      )}
    </div>
  )
}

export default DPContext(Sidebar)
