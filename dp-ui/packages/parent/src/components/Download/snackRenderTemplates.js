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
import { withStyles } from '@material-ui/core/styles'
import Button from '@material-ui/core/Button'
import CheckCircleIcon from '@material-ui/icons/CheckCircle'
import CircularProgress from '@material-ui/core/CircularProgress'
import ErrorIcon from '@material-ui/icons/Error'
import formatter from '@dp-ui/lib/dist/helpers/formatter'
import { DPDownload } from '@dp-ui/lib'

import amber from '@material-ui/core/colors/amber'

const styles = (theme) => ({
  icon: {
    fontSize: 20,
    opacity: 0.9,
    marginRight: theme.spacing(1),
  },
  message: {
    paddingLeft: theme.spacing(1),
    paddingRight: theme.spacing(1),
    display: 'flex',
    alignItems: 'center',
  },
  progress: {
    marginRight: '5px',
    color: 'white',
  },
  cancel: {
    marginLeft: 'auto',
    marginRight: -theme.spacing(1),
    color: 'white',
  },
})

const Success = (props) => (
  <div className={props.classes.message}>
    <CheckCircleIcon className={props.classes.icon} />
    {props.entityForDownload
      ? `Download Ready for ${formatter.truncate(props.entityForDownload, 12)}`
      : 'Download Ready'}
  </div>
)

const Info = (props) => (
  <div className={props.classes.message}>
    <CircularProgress className={props.classes.progress} size={20} />
    <span
      style={{
        position: 'relative',
        display: 'flex',
        flexDirection: 'column',
      }}>
      {props.entityForDownload
        ? `Preparing Download of ${formatter.truncate(
            props.entityForDownload,
            12
          )}`
        : `Preparing Download`}
      {props.numDownloads > 1 && (
        <span
          style={{
            fontSize: 9,
            top: '100%',
            left: 0,
            opacity: 0.7,
          }}>
          (Downloading multiple tables may take a while)
        </span>
      )}
    </span>
    <Button
      size="small"
      onClick={() => {
        props.cancelJob()
      }}
      className={props.classes.cancel}>
      Cancel
    </Button>
  </div>
)

const Error = (props) => (
  <div className={props.classes.message}>
    <ErrorIcon className={props.classes.icon} />
    {props.entityForDownload
      ? `Error preparing download of ${formatter.truncate(
          props.entityForDownload,
          12
        )}`
      : `Error preparing Download`}
  </div>
)

const SuccessRender = withStyles(styles)(DPDownload(Success))
const InfoRender = withStyles(styles)(DPDownload(Info))
const ErrorRender = withStyles(styles)(DPDownload(Error))

export { SuccessRender, InfoRender, ErrorRender }
