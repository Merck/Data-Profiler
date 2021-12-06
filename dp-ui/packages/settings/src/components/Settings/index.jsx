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
import { withStyles } from '@material-ui/core/styles'
import SettingsIcon from '@material-ui/icons/Settings'
import { Typography } from '@material-ui/core'
import ApiDocsLinkButton from './ApiDocsLinkButton'
import AttributePicker from './AttributePicker'
import ExperimentalSettings from './ExperimentalSettings'
import * as Sentry from '@sentry/react'

const styles = (theme) => ({
  container: {
    marginTop: 25,
    marginBottom: 50,
    // padding: '0 28px'
  },
  icon: {
    marginLeft: 10,
  },
  experimental: {
    marginTop: 30,
  },
  button: {
    display: 'block',
    marginTop: 8,
    marginLeft: 0,
  },
  heading: {
    fontSize: theme.typography.pxToRem(18),
    fontWeight: theme.typography.fontWeightBold,
  },
})

const Settings = ({ classes }) => (
  <div className={classes.container}>
    <Typography variant="h4">
      Settings
      <SettingsIcon className={classes.icon} />
    </Typography>
    <AttributePicker />
    <ExperimentalSettings />
    <ApiDocsLinkButton />
    <button // this is for testing Sentry
      style={{ display: 'none' }}
      onClick={() =>
        Sentry.captureException(new Error('This is my fake error message'))
      }>
      Test Sentry Error
    </button>
  </div>
)

export default withStyles(styles)(Settings)
