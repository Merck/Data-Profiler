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

const ACTION_CREATE = 'create'
const ACTION_UPDATE = 'update'
const ACTION_DELETE = 'delete'
const VALID_ACTIONS = [ACTION_CREATE, ACTION_UPDATE, ACTION_DELETE]

module.exports = {
  up: async (queryInterface, Sequelize) => {
    await queryInterface.createTable('audit_log', {
      id: {
        allowNull: false,
        autoIncrement: true,
        primaryKey: true,
        type: Sequelize.BIGINT
      },
      // 'users', 'items', etc
      table_name: {
        type: Sequelize.STRING(255),
        allowNull: false
      },
      table_row_id: {
        type: Sequelize.BIGINT,
        allowNull: false
      },
      // 'create', 'update', 'delete'
      action: {
        type: Sequelize.STRING(32),
        allowNull: false,
        validate: {
          isIn: {
            args: [VALID_ACTIONS],
            msg: 'Action must be create, update, or delete'
          }
        }
      },
      // 2016-01-01T00:00:00Z
      timestamp: {
        type: Sequelize.DATE,
        allowNull: false
      },
      // DEV: We could store `changed_values_previous` and `changed_values_current`
      //   but for simplicity of querying, we are storing all values
      // DEV: We wanted to use JSONB since writes only occur once whereas reads can occur many times
      //   However, PostgreSQL@9.3 lacks JSONB =(
      //   https://www.postgresql.org/docs/9.3/static/datatype-json.html
      // {id: abc, email: abc1, password: hash1, ...}
      previous_values: {
        type: Sequelize.JSON,
        allowNull: false
      },
      // {id: abc, email: abc2, password: hash2, ...}
      current_values: {
        type: Sequelize.JSON,
        allowNull: false
      },
      application: {
        type: Sequelize.STRING(255),
        allowNull: false
      },
      action_performed_by_id: {
        type: Sequelize.BIGINT,
        references: {
          model: 'users',
          key: 'id'
        }
      }
    })
    return true
  },
  down: (queryInterface, Sequelize) => {
    return queryInterface.dropTable('audit_log')
  }
}
