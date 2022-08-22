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
import { User, Attribute, UserAttribute } from '../index.js'
import { Sequelize } from '../../connectors'
import { normalize, escapeAccumulo } from '../helpers/stringOps'
import _ from 'lodash'

const Op = Sequelize.Op
const LAST_NAME_ATTRIBUTE = 'hr.prmry_last_nm'
const FIRST_NAME_ATTRIBUTE = 'hr.prmry_first_nm'
const POSITION_ATTRIBUTE = 'hr.prmry_job_typ_desc'
const like = str => `${str}%`
const getAttributeContents = data => {
  console.log(JSON.stringify(data))
  if (data && data.length > 0 && data[0] && data[0].value) {
    const record = data[0].value
    const rawValue = record
      .split('.')
      .splice(2)
      .join('')
    const string = _.words(rawValue)
      .map(_.startCase)
      .join(' ')
      .trim()
    return string
  }
  return null
}

async function removeUser(_, args, context) {
  const { username } = args
  const user = await User.findOne({ where: { username: normalize(username) } })
  await UserAttribute.destroy({ where: [{ user_id: user.id }] })
  await user.destroy()
  console.log(`Removed user ${user.id}`)
}

export const resolvers = {
  Query: {
    user(_, args) {
      return User.find({ where: args })
    },
    usersLike(_, args) {
      return User.findAll({
        limit: 50,
        where: { username: { [Op.iLike]: args.username.trim() + '%' } }
      })
    },
    allUsers() {
      return User.findAll()
    },
    numUsers() {
      return User.count()
    },
    numUsersWithAttribute(_, args) {
      return new Promise(resolve => {
        User.findAndCountAll({
          include: [
            {
              model: Attribute,
              where: { value: args.value },
              required: true
            }
          ]
        }).then(res => resolve(res.count))
      })
    },
    usersWithAttribute(_, args) {
      return User.findAll({
        include: [
          {
            model: Attribute,
            where: {
              value: args.value
            },
            required: true
          }
        ]
      })
    }
  },
  Mutation: {
    createUpdateUser(_, args, context) {
      const { username, attributes } = args
      // console.log(`Add attributes: "${attributes}" to user: "${username}"`)
      return User.findOrCreate({ where: { username: normalize(username) } })
        .spread(async user => {
          const escapedAttributes = await escapeAccumulo(attributes)
          // console.log(`escaped attrs: ${escapedAttributes}`)
          await Promise.all(
            escapedAttributes.map(attribute =>
              Attribute.findOrCreate({ where: { value: attribute } }).spread(
                dbAttr => {
                  // console.log(`dbAttr: ${dbAttr.id}`)
                  // console.log(`dbAttr: ${dbAttr}`)
                  UserAttribute.findOrCreate({
                    where: { user_id: user.id, attribute_id: dbAttr.id },
                    action_performed_by_id: context.currentUserId,
                    application: context.application
                  })
                }
              )
            )
          )
          return user
        })
        .then(user => user.reload())
    },
    removeAttributesFromUser(_, args, context) {
      const { username, attributes } = args
      return User.findOne({ where: { username: normalize(username) } })
        .then(async user => {
          const foundAttributes = await Attribute.findAll({
            where: { value: { [Op.in]: attributes } }
          }).map(attr => attr.id)
          const userAttributes = await UserAttribute.findAll({
            where: {
              [Op.and]: [
                { user_id: user.id },
                { attribute_id: foundAttributes }
              ]
            }
          })
          //Auditlogger only supports individual deletions, not bulk
          userAttributes.forEach(async userAttribute => {
            await userAttribute.destroy({
              action_performed_by_id: context.currentUserId,
              application: context.application
            })
          })
          return user
        })
        .then(user => user.reload())
    },
    removeUser
  },
  User: {
    attributes(user) {
      return user.getAttributes()
    },
    rules(user) {
      return user.getRules()
    },
    first_name(user) {
      return user
        .getAttributes({
          where: {
            value: {
              [Op.iLike]: like(FIRST_NAME_ATTRIBUTE)
            }
          }
        })
        .then(getAttributeContents)
    },
    last_name(user) {
      return user
        .getAttributes({
          where: {
            value: {
              [Op.iLike]: like(LAST_NAME_ATTRIBUTE)
            }
          }
        })
        .then(getAttributeContents)
    },
    position(user) {
      return user
        .getAttributes({
          where: {
            value: {
              [Op.iLike]: like(POSITION_ATTRIBUTE)
            }
          }
        })
        .then(getAttributeContents)
    }
  }
}
