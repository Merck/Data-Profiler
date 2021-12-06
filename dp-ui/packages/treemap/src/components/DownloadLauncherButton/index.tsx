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
import { Button } from '@material-ui/core'
import CloudCircleOutlined from '@material-ui/icons/CloudCircleOutlined'
import { createStyles, makeStyles } from '@material-ui/core/styles'
import DownloadRequest from '../../models/download/DownloadRequest'
import * as dplib from '@dp-ui/lib'

const styles = () =>
  createStyles({
    launchButton: {
      margin: 8,
      textTransform: 'unset',
    },
    icon: {
      marginRight: 8,
      width: '0.75em',
      height: '0.75em',
    },
  })

const useStyles = makeStyles(styles)

const DownloadLauncherButton = (props) => {
  const classes = useStyles()

  const handleDownloadTableClick = (
    event?: React.MouseEvent<Element>
  ): void => {
    if (event) {
      event.preventDefault()
    }

    const { dataprofiler, dataset, table } = props
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
    const downloadRequest: DownloadRequest = {
      type: 'row',
      limit: -1,
      table,
      dataset,
      filters: {},
    }
    let finalDownloadRequest

    if (Array.isArray(table)) {
      const requestArray = []
      for (const item of table) {
        requestArray.push({
          type: 'row',
          limit: -1,
          table: item,
          dataset,
          filters: {},
        })
      }
      finalDownloadRequest = [...requestArray]
    } else {
      finalDownloadRequest = downloadRequest
    }

    download.addDownload(finalDownloadRequest)
  }

  const generateDownloadBtn = (): JSX.Element => {
    const { dataprofiler, table } = props
    const buttonProps = {
      onClick: (e) => handleDownloadTableClick(e),
      className: classes.launchButton,
    }
    if (dataprofiler?.canDownload) {
      return Array.isArray(table) ? (
        <Button
          {...buttonProps}
          color="primary"
          variant="contained"
          style={{ margin: 0 }}>
          Download Data
        </Button>
      ) : (
        <Button
          {...buttonProps}
          color="primary"
          variant="contained"
          size="small">
          Download Table
        </Button>
      )
    } else {
      return <React.Fragment></React.Fragment>
    }
  }

  return generateDownloadBtn()
}

export default dplib.DPContext(dplib.DPDownload(DownloadLauncherButton))
