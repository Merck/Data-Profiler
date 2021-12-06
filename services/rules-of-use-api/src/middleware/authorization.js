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
import { Application, User } from '../models/index'
import _ from 'lodash'

export async function getApplication(token) {
  let application = await Application.find({ where: { key: token } })
  return _.get(application, 'app', null)
}

export async function authorization(ctx, next) {
  const token = ctx.request.headers.authorization
  let application
  if (token) {
    application = await getApplication(token)
  }

  if (!application) {
    throw ctx.throw(401, 'access_denied')
  } else {
    await next()
  }
}

export async function getCurrentUserId(username) {
  let user = await User.find({ where: { username: username } })
  return _.get(user, 'id', null)
}
