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
import React, { useEffect, useState } from 'react'
import { CircularProgress } from '@material-ui/core'
import { DPContext } from '@dp-ui/lib'
import { useLocation } from 'react-router-dom'

function Initialize(props) {
  const [initialized, setInitialized] = useState(
    props.dataprofiler.state.app.initialized
  )
  const [fetching, setFetching] = useState(false)
  const { dataprofiler, api } = props
  const { bulkSetDPState } = dataprofiler
  const location = useLocation()

  async function initialize() {
    if (!initialized) {
      if (
        ['/login', '/logout', '/authcallback'].indexOf(location.pathname) === -1
      ) {
        if (!dataprofiler.state.app.datasets && !fetching) {
          setFetching(true)
          await api
            .post({
              resource: 'v2/datasets/graphql',
              postObject: {
                query: `{metadata {dataset_name 
                                 dataset_display_name 
                                 table_name column_name 
                                 data_type 
                                 num_columns 
                                 num_values 
                                 num_unique_values 
                                 load_time 
                                 update_time 
                                 visibility 
                                 version_id
                                 num_tables}}`,
              },
            })
            .then((res) => {
              bulkSetDPState('app', {
                datasets: res.body,
                initialized: true,
              })
              setFetching(false)
            })
            .catch((err) => console.log(err))
            .finally(() => {
              setInitialized(true)
            })
        }
      }
    }
  }

  useEffect(() => {
    if (!initialized && !fetching) {
      initialize()
    }
  }, [location.pathname, dataprofiler.state.app.datasets]) //  eslint-disable-line

  return initialized || !fetching ? props.children : <CircularProgress />
}

export default DPContext(Initialize)
