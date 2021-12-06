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
  up: async (queryInterface, Sequelize) => {
    await queryInterface.sequelize.query(
      'ALTER TABLE users ALTER COLUMN username TYPE TEXT;'
    )
    await queryInterface.sequelize.query(
      'ALTER TABLE attributes ALTER COLUMN value TYPE TEXT;'
    )
    await queryInterface.sequelize.query('DROP EXTENSION IF EXISTS citext;')
    return true
  },

  down: async (queryInterface, Sequelize) => {
    await queryInterface.sequelize.query(
      'ALTER TABLE users ALTER COLUMN username TYPE citext;'
    )
    await queryInterface.sequelize.query(
      'ALTER TABLE attributes ALTER COLUMN value TYPE citext;'
    )
    await queryInterface.sequelize.query(
      'CREATE EXTENSION IF NOT EXISTS citext WITH SCHEMA public;'
    )
    return true
  }
}
