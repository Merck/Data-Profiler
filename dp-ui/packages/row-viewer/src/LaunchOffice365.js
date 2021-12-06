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
import React, { useState, useContext } from 'react'
import request from 'superagent'
import { RowViewerContext } from './store'
import uuid from 'uuid/v4'
import blankExcel from './assets/blank.xlsx'
import ExcelIcon from './ExcelLogo.js'
import {
  IconButton,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Button,
  LinearProgress,
  Tooltip,
} from '@material-ui/core'
import { DPContext } from '@dp-ui/lib'
import hello from 'hellojs'
import { SET_OFFICE_AUTH } from './store'


hello.init(
  {
    msft: {
      id:
        process.env.MICROSOFT_AZURE_APP_ID ||
        '',
      oauth: {
        version: 2,
        auth: 'https://login.microsoftonline.com/common/oauth2/v2.0/authorize',
      },
      scope_delim: ' ',
      form: false,
    },
  },
  {
    redirect_uri: process.env.USER_FACING_UI_HTTP_PATH
      ? `${process.env.USER_FACING_UI_HTTP_PATH}${process.env.PUBLIC_URL}/office365`
      : `http://localhost:3000${process.env.PUBLIC_URL}/office365`,
  }
)

const LaunchOffice365 = (props) => {
  const [inFlight, setInFlight] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [state, dispatch, getState] = useContext(RowViewerContext)
  const filename = `dataprofiler_live/${state.dataset}_${state.table}_${uuid()}`

  const { rows, sortedColumns } = state
  const data =
    !rows || rows.length === 0
      ? []
      : [
          sortedColumns,
          ...rows.map((row) => sortedColumns.map((col) => row[col])),
        ]

  const setReq = (req) => {
    req.timeout({ deadline: 120000 })
    const authToken = getState().office365AuthToken
    req.set('Authorization', `Bearer ${authToken}`)
    return req
  }

  const office365Api = {
    get: (path) => {
      const get = request.get(path)
      return setReq(get)
    },
    put: (path) => {
      const put = request.put(path)
      return setReq(put)
    },
    patch: (path) => {
      const put = request.patch(path)
      return setReq(put)
    },
    post: (path) => {
      const post = request.post(path)
      return setReq(post)
    },
  }

  const getMsOAuthCreds = () =>
    new Promise((resolve) => {
      const auth = hello('msft').login({
        scope: 'user.read,files.readwrite',
      })
      resolve(auth)
    })

  const getColumnName = (num) => {
    for (let ret = '', a = 1, b = 26; (num -= a) >= 0; a = b, b *= 26) {
      ret = String.fromCharCode(parseInt((num % b) / a) + 65) + ret
    }
    return ret
  }

  const getSession = ({ fileId, fileUrl }) =>
    office365Api
      .post(
        `https://graph.microsoft.com/v1.0/me/drive/items/${fileId}/workbook/createSession`
      )
      .then((res) => ({ sessionId: res.body.id, fileId, fileUrl }))

  const getBlob = (file) =>
    request
      .get(file)
      .responseType('blob')
      .then((r) => r.body)

  const uploadBlankExcelSheet = async () => {
    const blob = await getBlob(blankExcel)
    return office365Api
      .put(
        `https://graph.microsoft.com/v1.0/me/drive/items/root:/${filename}.xlsx:/content`
      )
      .set({
        'Content-Type': `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`,
      })
      .send(blob)
      .then((res) => ({ fileId: res.body.id, fileUrl: res.body.webUrl }))
  }

  const insertTable = ({ fileId, sessionId, fileUrl }) => {
    return office365Api
      .post(
        `https://graph.microsoft.com/v1.0/me/drive/items/${fileId}/workbook/worksheets/Sheet1/tables/add`
      )
      .set('Workbook-Session-Id', sessionId)
      .send({
        address: `A1:${getColumnName(data[0].length)}1`,
        hasHeaders: false,
      })
      .then((res) => ({ fileId, tableId: res.body.id, sessionId, fileUrl }))
  }

  const updateTableView = ({ fileId, tableId, sessionId, fileUrl }) => {
    return office365Api
      .patch(
        `https://graph.microsoft.com/v1.0/me/drive/items/${fileId}/workbook/tables/${tableId}`
      )
      .set('Workbook-Session-Id', sessionId)
      .send({ showHeaders: false })
      .then(() => ({ fileId, tableId, sessionId, fileUrl }))
  }

  const populateTable = ({ fileId, tableId, sessionId, fileUrl }) => {
    return office365Api
      .post(
        `https://graph.microsoft.com/v1.0/me/drive/items/${fileId}/workbook/tables/${tableId}/rows/add`
      )
      .set('Workbook-Session-Id', sessionId)
      .send({ index: null, values: data })
      .then(() => {
        setInFlight(false)
        setModalOpen(false)
        window.open(fileUrl, '_blank')
        return true
      })
  }

  const setAuthToken = (res) =>
    new Promise((resolve) => {
      dispatch({
        type: SET_OFFICE_AUTH,
        office365AuthToken: res.authResponse.access_token,
      })
      resolve(res.authResponse.access_token)
    })

  const generateExcel = () => {
    setInFlight(true)
    getMsOAuthCreds()
      .then(setAuthToken)
      .then(uploadBlankExcelSheet)
      .then(getSession)
      .then(insertTable)
      .then(updateTableView)
      .then(populateTable)
      .catch((e) => {
        console.error(e)
        props.dataprofiler.setErrorModal(
          'There was an error opening this table in Excel.'
        )
        setInFlight(false)
        setModalOpen(false)
        throw e
      })
  }

  return (
    <>
      <Tooltip title="Open In Excel (Office365)">
        <span>
          <IconButton
            onClick={() => setModalOpen(true)}
            disabled={!data || data.length === 0}>
            {inFlight ? (
              <CircularProgress size={20} />
            ) : (
              <ExcelIcon width={20} height={20} />
            )}
          </IconButton>
        </span>
      </Tooltip>
      <Dialog open={modalOpen} keepMounted onClose={() => setModalOpen(false)}>
        <DialogTitle>Open{inFlight ? 'ing' : ''} with Office365</DialogTitle>
        <DialogContent>
          <DialogContentText>
            {inFlight ? (
              <React.Fragment>
                <LinearProgress />
                <div
                  style={{
                    marginTop: 16,
                  }}>{`Please note: you may need to allow popups from ${
                  process.env.USER_FACING_UI_HTTP_PATH || 'this url'
                }`}</div>
              </React.Fragment>
            ) : (
              'You are about to open the current row viewer contents in Office365. The data will be a copy; any changes you make WILL NOT be reflected in Data Profiler. By clicking OK, you agree to properly handle the data with respect to classification and confidentiality.'
            )}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          {!inFlight && (
            <React.Fragment>
              <Button onClick={() => setModalOpen(false)} color="inherit">
                Cancel
              </Button>
              <Button onClick={generateExcel} color="primary">
                OK
              </Button>
            </React.Fragment>
          )}
        </DialogActions>
      </Dialog>
    </>
  )
}

export default DPContext(LaunchOffice365)
