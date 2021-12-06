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
import { AuthenticationTokenCache } from './authenticationTokenCache.js'
import { Sequelize } from '../../connectors'
import moment from 'moment'
import _ from 'lodash'
import bcrypt from 'bcrypt'

const Op = Sequelize.Op
const { INACTIVE_TIMEOUT_MINUTES, TIMEOUT_MINUTES } = process.env

const touch = instance => {
  if (!instance) return null
  const now = new Date()
  instance.set('updated_at', now)
  instance.set(
    'inactive_at',
    moment()
      .add(Number(INACTIVE_TIMEOUT_MINUTES), 'minutes')
      .toDate()
  )
  return instance.save()
}

const validate = obj =>
  bcrypt
    .compare(obj.token, obj.instance.token)
    .then(isCorrect => (isCorrect ? obj.instance : null))

const findCache = (username, token) =>
  new Promise(resolve => {
    return AuthenticationTokenCache.findOne({
      where: { username }
    }).then(instance => resolve({ instance, token }))
  })

export const resolvers = {
  Query: {
    checkToken(_, args) {
      const { username, token } = args
      return findCache(username, token)
        .then(validate)
        .then(touch)
    }
  },
  Mutation: {
    tokenLogin(_, args) {
      const { username, token } = args
      return AuthenticationTokenCache.destroy({ where: { username } })
        .then(() => bcrypt.hash(token, 5))
        .then(hash =>
          AuthenticationTokenCache.create({
            username,
            token: hash,
            inactive_at: moment()
              .add(Number(INACTIVE_TIMEOUT_MINUTES), 'minutes')
              .toDate(),
            expires_at: moment()
              .add(Number(TIMEOUT_MINUTES), 'minutes')
              .toDate()
          })
        )
    }
  }
}
