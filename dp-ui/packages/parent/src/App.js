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
import Sidebar from './Sidebar'
import Router from './Router'
import FloatingMenu from './components/FloatingMenu'
import Download from './components/Download'
import Modal from './components/Modal'
import { createMuiTheme, ThemeProvider } from '@material-ui/core/styles'
import { MatomoProvider, createInstance } from '@datapunt/matomo-tracker-react'

const matomo = createInstance({
  urlBase: 'https://admin.dataprofiler.com/analytics',
  siteId: process.env.ANALYTICS_UI_SITE_ID || 1, // optional, default value: `1`
  trackerUrl: 'https://admin.dataprofiler.com/analytics/matomo.php', // optional, default value: `${urlBase}matomo.php`
  srcUrl: 'https://admin.dataprofiler.com/analytics/matomo.js', // optional, default value: `${urlBase}matomo.js`
})

function App(props) {
  const theme = createMuiTheme({
    typography: {
      fontFamily: ['Invention'].join(','),
    },
    palette: {
      primary: {
        main: '#389D8F',
      },
    },
    props: {
      MuiButton: {
        disableElevation: true,
      },
    },
  })

  return (
    <ThemeProvider theme={theme}>
      <MatomoProvider value={matomo}>
        <Modal />
        <Download />
        <FloatingMenu />
        <Sidebar />
        <Router />
      </MatomoProvider>
    </ThemeProvider>
  )
}

export default App
