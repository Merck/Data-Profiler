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
import Button from '@material-ui/core/Button'
import Dialog from '@material-ui/core/Dialog'
import DialogActions from '@material-ui/core/DialogActions'
import DialogContent from '@material-ui/core/DialogContent'
import DialogContentText from '@material-ui/core/DialogContentText'
import DialogTitle from '@material-ui/core/DialogTitle'
import AlertIcon from '@material-ui/icons/Warning'
import Slide from '@material-ui/core/Slide'
import { useLocalStorage, deleteFromStorage } from '@rehooks/local-storage'

function Transition(props) {
  return <Slide direction="up" {...props} />
}

const close = () => deleteFromStorage('modal')

const Modal = () => {
  const [modalData] = useLocalStorage('modal', {})
  const { title, message } = modalData
  return (
    <Dialog
      open={Boolean(message)}
      TransitionComponent={Transition}
      keepMounted
      onClose={close}>
      <DialogTitle>
        {title || 'Alert'}
        {title === 'Application Error' && (
          <AlertIcon style={{ marginLeft: 25 }} />
        )}
      </DialogTitle>
      <DialogContent>
        <DialogContentText>{message}</DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={close} color="primary">
          Close
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default Modal
