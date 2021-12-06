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
'use strict'

const config = require('../config')
const handlers = require('../handlers')

module.exports = [
  {
    method: 'POST',
    path: '/unoconv/{format}',
    config: {
      payload: {
        output: 'stream',
        parse: true,
        allow: 'multipart/form-data',
        maxBytes: parseInt(config.PAYLOAD_MAX_SIZE, 10),
        timeout: parseInt(config.PAYLOAD_TIMEOUT, 10)
      },
      timeout: {
        server: parseInt(config.TIMEOUT_SERVER, 10),
        socket: parseInt(config.TIMEOUT_SOCKET, 10)
      },
      handler: handlers.handleUpload
    }
  },
  {
    method: 'GET',
    path: '/unoconv/formats',
    config: {
      handler: handlers.showFormats
    }
  },
  {
    method: 'GET',
    path: '/unoconv/formats/{type}',
    config: {
      handler: handlers.showFormat
    }
  },
  {
    method: 'GET',
    path: '/unoconv/versions',
    config: {
      handler: handlers.showVersions
    }
  },
  {
    method: 'GET',
    path: '/healthz',
    config: {
      handler: handlers.healthcheck
    }
  }
]
