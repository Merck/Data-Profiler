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
import { DPContext } from '@dp-ui/lib'
import { get } from 'lodash'
import React, { lazy, useEffect } from 'react'
import { Redirect, Route } from 'react-router-dom'
import Login from './components/Login'
import OAuthCallback from './components/Login/OAuthCallback'
import Logout from './components/Logout'
import Tutorial from './components/Tutorial'
import { useMatomo } from '@datapunt/matomo-tracker-react'
import { ensureFirstLoginSet } from './provider.js'
import { once } from 'lodash'

const Settings = lazy(() => import('@dp-ui/settings'))
const Treemap = lazy(() => import('@dp-ui/treemap'))
const DownloadTest = lazy(() => import('@dp-ui/download-test'))
const JobStatus = lazy(() => import('@dp-ui/job-status'))
const Office365Callback = lazy(() => import('@dp-ui/row-viewer/dist/Callback'))
const ApplicationsDisplayGrid = lazy(() =>
  import('./components/ApplicationsDisplayGrid')
)

const redir = (frm, to) => (
  <Route path={frm} render={() => <Redirect to={to} />} />
)

function Routes(props) {
  const { trackPageView, pushInstruction } = useMatomo()

  useEffect(() => {
    pushInstruction('setUserId', props.dataprofiler.state.session.username)
  }, [props.dataprofiler.state.session.username]) //eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    props.routeChange()
    trackPageView()
    window.scrollTo(0, 0)
  }, [props.history.location.pathname]) //eslint-disable-line react-hooks/exhaustive-deps

  return (
    <React.Fragment>
      <Route path="/treemap" component={Treemap} />
      <Route path="/office365" component={Office365Callback} />
      <Route path="/download" component={DownloadTest} />
      <Route path="/settings" component={Settings} />
      <Route path="/job_status" component={JobStatus} />
      <Route path="/tutorial" component={Tutorial} />
      <Route exact path="/login" component={Login} />
      <Route exact path="/logout" component={Logout} />
      <Route exact path="/apps" component={ApplicationsDisplayGrid} />
      <Route exact path="/authcallback" component={OAuthCallback} />
      <Route
        exact
        path="/"
        render={() => {
          const authToken = get(props, 'dataprofiler.state.authToken')
          if (authToken) {
            return props.router.navigateHard('/login')
          }
          return props.router.navigate('/treemap')
        }}
      />
      {redir('/swagger', '/settings/swagger')}
      {redir('/v5', '/treemap')}
      {redir('/v4', '/treemap')}
      {redir('/v3', '/treemap')}
    </React.Fragment>
  )
}

export default DPContext(Routes)
