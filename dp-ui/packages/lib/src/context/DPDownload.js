import React, { createContext, useReducer, useCallback } from 'react'
import { persistingDispatch, hydrate } from '../helpers/reducers'
import { s3NamesForDocs } from '../helpers/download'
import uuid from 'uuid/v4'
import api from '../api'

const initialDownloads = {}

function downloadsReducer(state, action) {
  switch (action.type) {
    case 'ADD':
      return {
        ...state,
        ...{
          [action.id]: {
            ...state[action.id],
            ...action.payload,
          },
        },
      }
    case 'UPDATE':
      return {
        ...state,
        ...{
          [action.id]: {
            ...state[action.id],
            ...action.payload,
          },
        },
      }
    case 'REMOVE':
      //state[action.id] = {}
      delete state[action.id]
      return state
    default:
      return state
  }
}

function intervalReducer(state, action) {
  switch (action.type) {
    case 'ADD':
      state[action.id] = action.payload
      return state
    case 'REMOVE':
      delete state[action.id]
      return state
    default:
      return state
  }
}

export const SetDPDownload = ({ children }) => {
  const [downloads, downloadsDispatch] = useReducer(
    persistingDispatch(downloadsReducer, 'downloads'),
    hydrate('downloads', initialDownloads)
  )

  const [interval, intervalDispatch] = useReducer(
    persistingDispatch(intervalReducer, 'dlinterval'),
    hydrate('dlinterval', {})
  )

  const setLeInterval = (intervalid, statusId) => {
    intervalDispatch({
      type: 'ADD',
      id: statusId,
      payload: intervalid,
    })
  }

  const removeLeInterval = (statusId) => {
    intervalDispatch({ type: 'REMOVE', id: statusId })
  }


  const addDownload = (downloadRequest, statusId = uuid()) => {
    const downloads = []
    if (Array.isArray(downloadRequest)) {
      for (const item of downloadRequest) {
        downloads.push(item)
      }
    } else {
      downloads = [downloadRequest]
    }
    const postObj = {
      downloads,
    }
    const entityType =
      downloadRequest.table ||
      downloadRequest.dataset ||
      downloads?.[0]?.dataset ||
      'n/a'
    api()
      .post({
        resource: 'jobs/download',
        postObject: postObj,
      })
      .then((res) => {
        const jobResp = res.body
        downloadsDispatch({
          type: 'ADD',
          id: statusId,
          payload: {
            jobId: jobResp.id,
            inProgress: true,
            link: null,
            error: null,
            entityForDownload: entityType,
            dataset: downloadRequest.dataset,
            postObject: postObj,
          },
        })
      })
      .catch((e) => {
        console.log(e)
        downloadsDispatch({
          type: 'UPDATE',
          id: statusId,
          payload: {
            error: true,
            inProgress: false,
          },
        })
        removeDownload(statusId)
      })
  }
  const removeDownload = (statusId) => {
    downloadsDispatch({ type: 'REMOVE', id: statusId })
  }

  const getJobId = (statusId) => {
    const job = downloads[statusId]
    if (job === undefined) {
      return null
    }
    return job.jobId
  }

  const cancelJob = (statusId) => {
    api()
      .post({
        resource: `jobs/canceller`,
        postObject: {
          to_cancel: getJobId(statusId),
        },
      })
      .then((res) => {
        console.log(res)
      })
      .catch((e) => {
        console.log(e)
      })
  }

  const refreshDownload = (statusId) => {
    const job = downloads[statusId]
    if (job === undefined) {
      return false
    }
    if (job.jobId && job.inProgress) {
      const { jobId } = job
      api()
        .get({
          resource: `jobs/${jobId}`,
        })
        .then((res) => {
          const { status, details } = res.body
          if (status === 'complete' || status === 7) {
            // 7 is complete from data-profiler/dp-core/util/src/main/java/com/dataprofiler/util/objects/Job.java
            downloadsDispatch({
              type: 'UPDATE',
              id: statusId,
              payload: {
                link: details?.s3Path,
                inProgress: false,
                error: false,
              },
            })
            removeDownload(statusId)
          } else if (status === 'error' || status === 8 || status === 9) {
            // 8 is cancelled, 9 is error from data-profiler/dp-core/util/src/main/java/com/dataprofiler/util/objects/Job.java
            downloadsDispatch({
              type: 'UPDATE',
              id: statusId,
              payload: {
                error: true,
                inProgress: false,
              },
            })
            removeDownload(statusId)
          }
        })
        .catch((e) => {
          console.log(e)
          downloadsDispatch({
            type: 'UPDATE',
            id: statusId,
            payload: {
              error: true,
              inProgress: false,
            },
          })
          removeDownload(statusId)
        })
    }
  }

  // call back needed to update context and sync with redux calls
  const getDownloads = useCallback(
    () => (downloads ? { ...downloads } : {}),
    [downloads, addDownload, removeDownload, refreshDownload]
  ) // react-hooks/exhaustive-deps

  const download = {
    downloads,
    interval,
    setLeInterval,
    removeLeInterval,
    getDownloads,
    addDownload,
    removeDownload,
    refreshDownload,
    getJobId,
    cancelJob,
  }

  return (
    <DownloadContext.Provider value={download}>
      {children}
    </DownloadContext.Provider>
  )
}

export const DownloadContext = createContext({})

export default (InputComponent) => (props) =>
  (
    <DownloadContext.Consumer>
      {(download) => {
        return (
          <InputComponent
            {...props}
            dataprofiler={{ ...props.dataprofiler, download }}
          />
        )
      }}
    </DownloadContext.Consumer>
  )
