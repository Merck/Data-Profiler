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
import {
  Tooltip,
  IconButton,
  Popover,
  Typography,
  TextField,
  Button,
} from '@material-ui/core'
import { makeStyles } from '@material-ui/core/styles'
import { RowViewerContext } from './store'
import { DPContext } from '@dp-ui/lib'
import RestorePage from '@material-ui/icons/RestorePage'
import LoadIcon from '@material-ui/icons/Input'
import Popover from '@material-ui/core/Popover'
import {
  usePopupState,
  bindTrigger,
  bindPopover,
} from 'material-ui-popup-state/hooks'
import { loadRows } from './actions'

const useStyles = makeStyles({
  container: {
    padding: 24,
    display: 'flex',
    flexDirection: 'column',
  },
  textField: {
    width: 225,
  },
  button: {
    margin: 8,
  },
})

const max = 5000
const min = 100

const LoadMoreRowsMenu = (props) => {
  const popupState = usePopupState({
    variant: 'popover',
    popupId: 'demoPopover',
  })
  const classes = useStyles()
  const [count, setCount] = useState(500)
  const [state, dispatch, getState] = useContext(RowViewerContext)

  return (
    <>
      <Tooltip title="Fetch more rows">
        <span>
          <IconButton
            disabled={!state.endLocation}
            {...bindTrigger(popupState)}>
            <RestorePage />
          </IconButton>
        </span>
      </Tooltip>
      <Popover
        style={{ zIndex: 990001 }}
        {...bindPopover(popupState)}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'center',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'center',
        }}>
        <div className={classes.container}>
          <TextField
            className={classes.textField}
            id="number"
            type="number"
            label="Addtional Rows to Load"
            value={count}
            onChange={(e) =>
              setCount(Math.min(max, Math.max(min, e.target.value)))
            }
            InputProps={{
              inputProps: {
                max,
                min,
                step: 100,
              },
            }}
            InputLabelProps={{
              shrink: true,
            }}
          />
          <Button
            size="small"
            variant="outlined"
            color="primary"
            onClick={() =>
              loadRows(dispatch, getState(), props.api, true, count)
            }
            disabled={state.loading || !state.endLocation}
            className={classes.button}
            startIcon={<LoadIcon />}>
            Load
          </Button>
        </div>
      </Popover>
    </>
  )
}

export default DPContext(LoadMoreRowsMenu)
