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
// import { Sequelize } from '../../connectors'
import moment from 'moment'
import _ from 'lodash'
import bcrypt from 'bcrypt'

// const Op = Sequelize.Op
const { INACTIVE_TIMEOUT_MINUTES, TIMEOUT_MINUTES } = process.env

// console.log(`Inactive minutes: ${INACTIVE_TIMEOUT_MINUTES}`)
// console.log(`timeout minutes: ${TIMEOUT_MINUTES}`)

const touch = instance => {
  // console.log(`intance (intouch): ${JSON.stringify(instance)}`)
  if (!instance) return null
  // console.log("here")
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
    .then(isCorrect => (isCorrect ? obj.instance : null));
// .then(isCorrect => {

//   console.log(`isCorrect ${isCorrect}`)
//   console.log(`isCorrect ${JSON.stringify(obj.instance)}`)
//   return isCorrect ? obj.instance : null
// })

const findCache = (username, token) =>
  new Promise(resolve => {
    return AuthenticationTokenCache.findOne({
      where: { username }
    }).then(instance => resolve({ instance, token }))
    // const tok = AuthenticationTokenCache.findOne({
    //   where: { username }
    // }).then(instance => resolve({ instance, token }))

    console.log(`tok: ${tok}`)
    return tok;
  })

export const resolvers = {
  Query: {
    checkToken(_, args) {
      const { username, token } = args
      console.log(`username: ${username}`)
      console.log(`token: ${token}`)
      return findCache(username, token)
        .then(validate)
        // .then(obj => {
        //   const str = JSON.stringify(obj)
        //   console.log(`obj: ${str}`)
        //   console.log(`toke: ${obj.token}`)
        //   console.log(`instance.token: ${obj.instance.token}`)
        //   return validate(obj)
        // })
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
