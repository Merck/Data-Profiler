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

module.exports = {
  SERVER_PORT: process.env.SERVER_PORT || 3000,
  PAYLOAD_MAX_SIZE: process.env.PAYLOAD_MAX_SIZE || 1048576,
  PAYLOAD_TIMEOUT: process.env.PAYLOAD_TIMEOUT || 60000 * 2,
  TIMEOUT_SERVER: process.env.TIMEOUT_SERVER || 60000 * 2,
  TIMEOUT_SOCKET: process.env.TIMEOUT_SOCKET || 70000 * 2
}
