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
import { DPContext, DPDownload } from '@dp-ui/lib'

const RootComponennt = (props) => {
  return (
    <div>
      <div>{`Hello World ${props.match.path}`}</div>
      <button
        onClick={() =>
          props.dataprofiler.download.addDownload(
            '822e49c3-b8e6-45e5-aa73-d881fdbdc549'
          )
        }>
        add download
      </button>
      <button
        onClick={() =>
          props.dataprofiler.setErrorModal(
            '822e49c3-b8e6-45e5-aa73-d881fdbdc549'
          )
        }>
        Modal
      </button>
    </div>
  )
}

export default DPDownload(DPContext(RootComponennt))
