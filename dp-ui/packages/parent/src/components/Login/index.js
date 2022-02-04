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
import LocalDeveloperAuthentication from './LocalDeveloperAuthentication'
import { DPContext, DPLoginPrep } from '@dp-ui/lib'
import OAuth from './OAuth'

const message =
  "The Data Profiler isn't connected to the API. Please refresh the page or contact the data profiler team for assistance"

const Login = (props) => {
  switch (props.dataprofiler.state.session.authenticationType) {
    case 'oauth':
      return <OAuth />
    case 'local-developer':
      return <LocalDeveloperAuthentication />
    default:
      return <div>{message}</div>
  }
}

export default DPContext(DPLoginPrep(Login))
