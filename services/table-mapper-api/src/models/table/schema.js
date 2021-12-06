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
export const typeDef = `
  type Table {
    id: Int,
    environment: String,
    datasetname: String,
    tablename: String,
    external_name: String,
    visibility: String,
    users: [User]
    created_at: Date,
    updated_at: Date,
    enabled: Boolean
  }
  extend type Query {
    tables(
      id: Int,
      datasetname: String,
      tablename: String,
      external_name: String,
      environment: String,
      visibility: String
    ): [Table]
    tablesLike(
      datasetname: String,
      tablename: String,
      external_name: String,
      environment: String,
      visibility: String): [Table]
    allTables: [Table]
  }
  extend type Mutation {
    createUpdateTable(
      environment: String!,
      users: [String]!,
      datasetname: String!,
      tablename: String!,
      external_name: String!,
      visibility: String!,
      enabled: Boolean!
    ): Table
    removeUsersFromTable(
      environment: String,
      users: [String],
      datasetname: String,
      tablename: String,
      visibility: String
    ): Table
    deleteTables(
      id: Int,
      datasetname: String,
      tablename: String,
      environment: String,
      visibility: String): Float
    deleteAllTables: [Table]
  }
`;
