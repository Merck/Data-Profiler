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
import React, { useContext } from 'react'
import { RowViewerContext } from '../store'
import { IconButton, Tooltip } from '@material-ui/core'
import FullScreen from '@material-ui/icons/Fullscreen'
import LaunchOffice365 from '../LaunchOffice365'
import LoadMoreRowsMenu from '../LoadMoreRowsMenu'
import { toggleFullscreen } from '../actions'
import * as dplib from '@dp-ui/lib'
import { CloudCircle } from '@material-ui/icons'

const HeaderControls = (props) => {
  const [rowViewerState, rowViewerDispatch] = useContext(RowViewerContext)
  const { dataprofiler } = props

  const handleDownloadTableClick = (event) => {
    if (event) {
      event.preventDefault()
    }

    const { dataprofiler } = props
    const { dataset, table, filters } = rowViewerState
    if (!dataprofiler || !dataprofiler?.download) {
      console.log(
        'download system is not initialized. Did you forget to wrap this component with the DPContext and DPDownload contexts?'
      )
    }
    if (!dataprofiler?.canDownload) {
      return
    }
    if (!dataset || !table) {
      console.log('please define both dataset and table to add a download')
    }
    const { download } = dataprofiler
    const downloadRequest = {
      type: 'row',
      limit: -1,
      table,
      dataset,
      filters: filters || {},
    }
    download.addDownload(downloadRequest)
  }

  return (
    <div>
      <LoadMoreRowsMenu />
      {dataprofiler?.canDownload && (
        <Tooltip title="Download">
          <span>
            <IconButton onClick={(e) => handleDownloadTableClick(e)}>
              <CloudCircle />
            </IconButton>
          </span>
        </Tooltip>
      )}
      <LaunchOffice365 />
      <Tooltip title="Fullscreen">
        <span>
          <IconButton onClick={() => toggleFullscreen(rowViewerDispatch)}>
            <FullScreen />
          </IconButton>
        </span>
      </Tooltip>
    </div>
  )
}

export default dplib.DPContext(dplib.DPDownload(HeaderControls))
