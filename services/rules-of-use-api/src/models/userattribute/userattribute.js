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
import { db, Sequelize } from '../../connectors'
import { saveAuditLog } from '../helpers/auditlogger'

const UserAttributeModel = db.define(
  'userAttribute',
  {
    id: {
      allowNull: false,
      autoIncrement: true,
      primaryKey: true,
      type: Sequelize.BIGINT
    },
    user_id: {
      type: Sequelize.BIGINT,
      references: {
        model: 'users',
        key: 'id'
      }
    },
    attribute_id: {
      type: Sequelize.BIGINT,
      references: {
        model: 'attributes',
        key: 'id'
      }
    }
  },
  {
    tableName: 'user_attributes',
    createdAt: 'created_at',
    updatedAt: 'updated_at',
    underscored: true,
    indexes: [
      {
        unique: true,
        fields: ['user_id', 'attribute_id']
      }
    ],
    hooks: {
      afterCreate: (userAttribute, options) =>
        saveAuditLog('create', userAttribute, options),
      afterDestroy: (userAttribute, options) =>
        saveAuditLog('delete', userAttribute, options)
    }
  }
)

export { UserAttributeModel }
