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
import { makeStyles } from '@material-ui/core/styles'
import { SuccessRender, InfoRender, ErrorRender } from './snackRenderTemplates'
import { toast } from 'react-toastify'
import { blue, green } from '@material-ui/core/colors'
import { transformS3PresignedToProxyAndDownload } from '@dp-ui/lib/dist/helpers/download'
import { DPDownload } from '@dp-ui/lib'

const useStyles = makeStyles((theme) => ({
  successToast: {
    backgroundColor: green[600],
    borderRadius: 4,
  },
  errorToast: {
    backgroundColor: theme.palette.error.dark,
    borderRadius: 4,
  },
  infoToast: {
    backgroundColor: theme.palette.primary.main,
    borderRadius: 4,
  },
}))

const DownloadStatus = (props) => {
  const classes = useStyles()
  const [ready, setReady] = useState(false)

  const { dataprofiler, statusId } = props
  const pollDownload = () => {
    dataprofiler.download.refreshDownload(statusId)
  }
  const finishInterval = () => {
    if (dataprofiler.download.interval[statusId]) {
      clearInterval(dataprofiler.download.interval[statusId])
      dataprofiler.download.removeLeInterval(statusId)
    }
  }

  const cancelJob = () => {
    dataprofiler.download.cancelJob(statusId)
    dataprofiler.download.removeDownload(statusId)
    finishInterval()
  }

  useEffect(() => {
    const leInterval = setInterval(pollDownload, 15000)
    dataprofiler.download.setLeInterval(leInterval, statusId)
    return () => finishInterval()
  }, []) //eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (ready) {
      const { dataset, entityForDownload } =
        dataprofiler.download.downloads[statusId]
      window['_paq'].push([
        'trackEvent',
        'Dataset Download',
        dataset,
        entityForDownload,
      ])
    }
  }, [ready])

  const notifyInfo = () => {
    const { entityForDownload } = dataprofiler.download.downloads[statusId]
    const numDownloads =
      dataprofiler.download.downloads[statusId]?.postObject?.downloads?.length

    if (!toast.isActive(statusId)) {
      toast(
        <InfoRender
          statusId={statusId}
          entityForDownload={entityForDownload}
          numDownloads={numDownloads}
          cancelJob={cancelJob}
        />,
        {
          className: classes.infoToast,
          toastId: statusId,
          type: 'info',
        }
      )
    }
  }

  const notifySuccess = () => {
    const { entityForDownload } = dataprofiler.download.downloads[statusId]
    if (!ready) {
      setReady(true)
      transformS3PresignedToProxyAndDownload(
        dataprofiler.download.downloads[statusId].link
      )
      dataprofiler.download.removeDownload(statusId)
      finishInterval()
    }
    toast.update(statusId, {
      render: <SuccessRender entityForDownload={entityForDownload} />,
      className: classes.successToast,
      type: 'success',
      autoClose: 5000,
    })
  }

  const notifyError = () => {
    const { entityForDownload } = dataprofiler.download.downloads[statusId]
    toast.update(statusId, {
      render: (
        <ErrorRender
          entityForDownload={entityForDownload}
          statusId={statusId}
        />
      ),
      className: classes.errorToast,
      type: 'error',
      autoClose: 5000,
    })
  }

  const { inProgress, error, link } = dataprofiler.download.downloads[statusId]

  return (
    <div>
      {link && notifySuccess()}
      {error && notifyError()}
      {inProgress && notifyInfo()}
    </div>
  )
}

export default DPDownload(DownloadStatus)
