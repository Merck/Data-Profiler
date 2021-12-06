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
import ReactDOM from 'react-dom'
import App from './App'
import { Router as ReactRouter, Route } from 'react-router-dom'
import { SetDPContext } from '@dp-ui/lib/dist/context/DPContext'
import { SetDPDownload } from '@dp-ui/lib/dist/context/DPDownload'
import { createBrowserHistory } from 'history'
import { QueryParamProvider } from 'use-query-params'
import { HTML5Backend } from 'react-dnd-html5-backend'
import { DndProvider } from 'react-dnd'
import * as Sentry from '@sentry/react';

import './fonts/fonts.css'
import './index.css'

Sentry.init({
  dsn: process.env.SENTRY_UI_DSN,
  release: process.env.RELEASE_VERSION,
});

const history = createBrowserHistory({ basename: process.env.PUBLIC_URL }) // this PUBLIC_URL is set in package.json#homepage

// padding: '8px 0px 8px 0px',
// padding: '8px 8px 8px 0px',
// backgroundColor: '#E5EBEE'
const rootStyle = {
  display: 'flex',
  alignItems: 'stretch',
  // minHeight: '100vh',
  // padding: 8,
}

ReactDOM.render(
  <React.StrictMode>
    <SetDPContext history={history}>
      <SetDPDownload>
        <div style={rootStyle}>
          <ReactRouter history={history}>
            <QueryParamProvider ReactRouterRoute={Route}>
              <DndProvider backend={HTML5Backend}>
                <App />
              </DndProvider>
            </QueryParamProvider>
          </ReactRouter>
        </div>
      </SetDPDownload>
    </SetDPContext>
  </React.StrictMode>,
  document.getElementById('root')
)
