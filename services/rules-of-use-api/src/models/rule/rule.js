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
import { StatementModel } from '../statement/statement'

const RuleModel = db.define(
  'rule',
  {
    id: {
      allowNull: false,
      autoIncrement: true,
      primaryKey: true,
      type: Sequelize.BIGINT
    },
    name: Sequelize.TEXT,
    visible: Sequelize.BOOLEAN,
    created_at: {
      allowNull: false,
      type: Sequelize.DATE
    },
    updated_at: {
      allowNull: false,
      type: Sequelize.DATE
    },
    created_by_id: {
      allowNull: false,
      type: Sequelize.BIGINT,
      references: {
        model: 'user',
        key: 'id'
      }
    }
  },
  {
    tableName: 'rules',
    createdAt: 'created_at',
    updatedAt: 'updated_at',
    underscored: true
  }
)

RuleModel.hasMany(StatementModel)

export { RuleModel }
