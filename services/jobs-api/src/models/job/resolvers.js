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
import { Job } from '../index.js'
import { Sequelize } from '../../connectors.js'

export const resolvers = {
    Query: {
        job(_, args) {
            return Job.find({ where: args })
        },
        allJobs(_, args) {
            return Job.findAll()
        },
        jobs(_, args) {
            const where = Object.assign({}, args)
            delete where.limit
            return Job.findAll({
                where: where,
                order: [['createdAt', 'DESC']],
                limit: args.limit
            })
        },
        jobsForUser(_, args) {
            return Job.findAll({
                where: {
                    creatingUser: args.user
                },
                order: [['createdAt', 'DESC']],
                limit: args.limit
            })
        },
        executableJob(_, args) {
            return Job.findOne({
                where: {
                    status: 'new',
                    type: { [Sequelize.Op.or]: args.types }
            },
                order: [['createdAt', 'DESC']]
            })
        },
        jobsByDetails(_, args) {
            args.details = {["::text"]: args.details}  // Due to json storage, force to text to find
            const where = Object.assign({}, args)
            delete where.limit
            return Job.findAll({ 
                where: where,
                limit: args.limit,
                order: [['createdAt', 'DESC']]
            })
        }
    },
    Mutation: {
        createJob(_, args) {
            args.status = 'new'
            args.details = JSON.parse(decodeURI(args.details))
            return Job.create(args)
        },
        updateJobStatus(_, args) {
            return Job.update(
                {
                    status: args.status
                },
                {
                    where: { id: args.id }
                }
            ).then(() => {
                return Job.findById(args.id)
            })
        },
        updateJobStatusDetails(_, args) {
            return Job.update(
                {
                    // In the db schema it's statusDetails, but everywhere else it's not plural
                    statusDetails: args.statusDetails
                },
                {
                    where: { id: args.id }
                }
            ).then(() => {
                return Job.findById(args.id)
            })
        },
        updateJobDetails(_, args) {
            return Job.update(
                {
                    details: JSON.parse(decodeURI(args.details))
                },
                {
                    where: { id: args.id }
                }
            ).then(() => {
                return Job.findById(args.id)
            })
        }
    }
}
