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
import { User, UserAttribute } from '../index'
import { Sequelize } from '../../connectors'

const Op = Sequelize.Op

export const queryExpression = statementsWithAttributes => {
  const havingStatement = {
    [Op.or]: statementsWithAttributes.map(statement => {
      const attrIds = statement.attributes.map(attr => attr.id)
      console.log(attrIds)
      return Sequelize.where(
        Sequelize.fn('ARRAY_AGG', Sequelize.col('attribute_id')),
        { $contains: `{${attrIds.join()}}` }
      )
    })
  }

  return User.findAll({
    attributes: [
      'id',
      'username',
      'created_at',
      'updated_at',
      [Sequelize.fn('ARRAY_AGG', Sequelize.col('attribute_id')), 'attributes']
    ],
    include: [
      {
        model: UserAttribute,
        attributes: [],
        required: true
      }
    ],
    group: ['user.id'],
    having: havingStatement
  })
}
