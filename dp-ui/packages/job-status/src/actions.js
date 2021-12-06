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
import { api } from '@dp-ui/lib'
import { SET_JOBS, SET_SEARCH } from './reducer'
import moment from 'moment'

const { KIBANA_PARTIAL_URL } = process.env

export const loadJobs = () => (dispatch) => {
  api()
    .get({ resource: `jobs` })
    .then((res) => {
      dispatch({
        type: SET_JOBS,
        jobs: res.body,
      })
    })
}

export const doSearch = (search) => (dispatch) => {
  dispatch({
    type: SET_SEARCH,
    search,
  })
}

export const getJobLogs = (jobid, createdAt) => {
  api()
    .get({ resource: `jobs/${jobid}/logs` })
    .then((res) => {
      const ele = document.createElement('a')
      ele.setAttribute(
        'href',
        'data:text/plain;charset=utf-8,' + encodeURIComponent(res.text)
      )
      ele.setAttribute('download', 'job_' + jobid + '_logs.txt')
      ele.style.display = 'none'
      document.body.appendChild(ele)
      ele.click()
      document.body.removeChild(ele)
    })
    .catch((e) => {
      // instead try to redirect to kibana and show any logs related to said job
      const fromed = KIBANA_PARTIAL_URL.replace(/STARTINGTIME/g, createdAt)
      const toed = fromed.replace(
        /ENDINGTIME/g,
        moment(createdAt).add(1, 'd').format()
      )
      const completed = toed.replace(/JOBID/g, jobid)
      window.open(completed)
    })
}
