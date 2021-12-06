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
import { CircularProgress } from '@material-ui/core'
import { makeStyles } from '@material-ui/core/styles'
import React, { Suspense, useState } from 'react'
import { Switch } from 'react-router-dom'
import Initialize from './components/Initialize'

import ErrorBoundary from './ErrorBoundary'
import Routes from './Routes'
import * as Sentry from '@sentry/react'

const useStyles = makeStyles((theme) => ({
  root: {
    padding: theme.spacing(3.5),
    paddingTop: theme.spacing(0),
    width: 'calc(100% - 72px)',
    '& > .MuiCircularProgress-root': {
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      margin: 'auto',
    },
    boxSizing: 'border-box',
  },
  defaultMargin: {
    marginLeft: 72,
  },
  noPadding: {
    padding: 0,
  },
  landingMargin: {
    marginLeft: 12,
  },
}))

// const defaultMargin = {
//   marginLeft: 32,
// }
// const landingMargin = {
//   marginLeft: 12,
// }

function Router(props) {
  const classes = useStyles()
  const [noPadding, setNoPadding] = useState(false)

  const noPaddingPages = ['/landing', '/treemap']

  const routeChange = () => {
    setNoPadding(noPaddingPages.includes(props.history.location.pathname))
  }

  return (
    <div
      className={`${classes.root} ${classes.defaultMargin} ${
        noPadding && classes.noPadding
      }`}>
      <Suspense fallback={<CircularProgress />}>
        <Sentry.ErrorBoundary fallback={<ErrorBoundary />}>
          <Initialize>
            <Switch>
              <Routes routeChange={routeChange} />
            </Switch>
          </Initialize>
        </Sentry.ErrorBoundary>
      </Suspense>
    </div>
  )
}

export default DPContext(Router)
