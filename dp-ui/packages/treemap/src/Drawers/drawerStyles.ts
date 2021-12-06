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
import { createStyles } from '@material-ui/core'
import { ACTION_BTN_COLOR } from '../dpColors'

export const genCommonDrawerStyles = () => {
  return createStyles({
    header: {
      color: 'rgba(0,0,0,.82)',
      fontWeight: 400,
    },
    name: {
      fontSize: '1em',
      fontWeight: 700,
      wordWrap: 'break-word',
    },
    selectDrilldown: {
      marginTop: '16px',
      marginBottom: '42px',
    },
    select: {
      minWidth: '100px',
    },
    menuPaper: {
      maxHeight: 400,
    },
    resultCountContent: {
      marginTop: '8px',
      marginBottom: '16px',
    },
    actionBtn: {
      color: ACTION_BTN_COLOR,
    },
  })
}
