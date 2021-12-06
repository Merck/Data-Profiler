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
  type Job {
    id: Int,
    environment: String,
    type: String,
    status: String,
    statusDetails: String,
    creatingUser: String,
    details: JSON,
    createdAt: Date,
    updatedAt: Date
  }
  extend type Query {
    job(id: Int): Job,
    allJobs: [Job],
    jobs(type: String = Null, status: String = Null, creatingUser: String = Null, limit: Int! = 1000): [Job],
    jobsForUser(user: String, limit: Int = 1000): [Job],
    executableJob(types: [String]): Job
    jobsByDetails(type: String = Null, status: String = Null, creatingUser: String = Null, limit: Int! = 1000, details: JSON!, environment: String!): [Job],
  }
  extend type Mutation {
    createJob(
        environment: String,
        type: String,
        creatingUser: String,
        details: String): Job
    updateJobStatus(id: Int, status: String): Job,
    updateJobStatusDetails(id: Int, statusDetails: String): Job,
    updateJobDetails(id: Int, details: JSON): Job
  }
`