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
import Koa from 'koa'
import Router from 'koa-router'
import { graphiqlKoa, graphqlKoa } from 'apollo-server-koa'
import { runQuery } from 'apollo-server-core'
import gql from 'graphql-tag'

import schema from './models/schema'
import koaBody from 'koa-bodyparser'
import './models/index'

var app = new Koa()
var router = new Router()

app.use(koaBody())

const createJobFinder = async (ctx) => {
  const parsedQuery = gql(ctx.request.body.query)
  for (const gqlidx in parsedQuery.definitions) {
    const gqldef = parsedQuery.definitions[gqlidx]
    if (gqldef.operation != 'mutation') {  // If not a mutation, continue
      continue
    }
    for (const selectionidx in gqldef.selectionSet.selections) {
      const selection  = gqldef.selectionSet.selections[selectionidx]
      if (selection.name.value === 'createJob') {  // If create job, then process
        const requiredArguments = ['creatingUser', 'details', 'environment']
        var jobByDetails = '{jobsByDetails(status:"new", limit:1, '
        for (const argidx in selection.arguments) {
          const arg = selection.arguments[argidx]
          if (requiredArguments.includes(arg.name.value)) {
            var val = arg.value.value
            if (arg.name.value === 'details') {
              val = arg.value.value.split('"').join('\\"')  // Special processing due to having a JSON entry field... see models.job.job, kill the person before me, not my fault! :D
            }
            jobByDetails += arg.name.value + ': "' + val + '" '  // Build a query for jobsByDetails
          }
        }
        jobByDetails += '){id environment type status creatingUser details}}'
        const gqlQuery = gql(jobByDetails)
        var jobByDetailsQuery = await runQuery({
          schema: schema,
          query: gqlQuery,
        })
        return jobByDetailsQuery
      }
    }
  }
  return {  // Catch if no job is found, return something queryable in the actual request
    data: {
      jobsByDetails: []
    }
  }
}


router.post('/graphql', async (ctx) => {
  const maybeJob = await createJobFinder(ctx)
  if (maybeJob.data.jobsByDetails.length === 1) {
    ctx.body = {
      'data': {
        'createJob': maybeJob.data.jobsByDetails[0]
      }
    }
  }
  else {
    return graphqlKoa({
      schema: schema
    })(ctx)
  }
})

router.get('/graphiql', graphiqlKoa({ endpointURL: '/graphql'}))

app
  .use(router.routes())
  .use(router.allowedMethods())
  .listen(8081)
