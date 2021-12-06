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

const Job = db.define('Job', {
    id: {
      allowNull: false,
      autoIncrement: true,
      primaryKey: true,
      type: Sequelize.BIGINT
    },
    // production, development, or whatever
    environment: Sequelize.STRING,
    // new, running, cancelled, error, finished
    status: Sequelize.STRING,
    statusDetails: Sequelize.STRING,
    // This is the type of the job (and it determines what the options are).
    type: Sequelize.STRING,
    creatingUser: Sequelize.STRING,
    // ok - it is simply not worth storing the json details for the job in another table.
    // we are not going to process them here. It might be nice to enforce some schema, but
    // at least parts of this (like the loader options) are going to be schemaless anyway. So I'm going to 
    // go all mongo / web developer and throw a json blob in here. When this explodes you can kill me then.
    details: Sequelize.JSON
  },
  {
    tableName: 'jobs',
    createdAt: 'createdAt',
    updatedAt: 'updatedAt',
    timestamps: true
  }
)

export { Job }

