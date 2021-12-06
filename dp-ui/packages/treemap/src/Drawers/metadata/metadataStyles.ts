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
import { ACTION_BTN_COLOR, BLACK_60 } from '../../dpColors'

export const genCommonMetadataStyles = () => {
  return createStyles({
    name: {
      flex: '1 1 auto',
      fontSize: '1em',
      fontWeight: 700,
      wordWrap: 'break-word',
    },
    metadataRow: {
      display: 'flex',
      marginBottom: '8px',
      // '&:last-child': {
      //   paddingTop: '24px',
      // },
    },
    metadataRowLeft: {
      flex: '1 1 auto',
    },
    metadataRowRight: {
      flex: '0 0 auto',
      margin: 'auto auto',
      textAlign: 'end',
      display: 'flex',
    },
    resultCountContent: {
      marginTop: '8px',
      marginBottom: '16px',
    },
    viewCommentsLink: {
      marginLeft: '10px',
      // color: BLACK_80,
      fontSize: '.9em',
      borderBottom: 'dashed 1px',
      color: ACTION_BTN_COLOR,
      cursor: 'pointer',
      textDecoration: 'none',
    },
    commentsRow: {
      marginTop: 16,
      marginBottom: 16,
    },
    commentsIcon: {
      fontSize: '.95em',
      color: BLACK_60,
      verticalAlign: 'middle',
    },
    metadataPanel: {
      marginTop: 8,
      background: '#eee',
      padding: 18,
      borderRadius: '5%',
      fontSize: '.95em',
    },
  })
}
