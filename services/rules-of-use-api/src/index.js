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
import { graphqlKoa, graphiqlKoa } from 'apollo-server-koa'

import schema from './models/schema'
import koaBody from 'koa-bodyparser'
import './models/index'
import {
  authorization,
  getApplication,
  getCurrentUserId
} from './middleware/authorization'
import { cacheBustTokens } from './middleware/authenticationTokenCache'

var app = new Koa()
var router = new Router()

app.use(koaBody())
router.post(
  '/graphql',
  graphqlKoa(async req => {
    return {
      schema: schema,
      context: {
        application: await getApplication(req.headers.authorization),
        currentUserId: await getCurrentUserId(req.headers['x-username'])
      }
    }
  })
)
router.get('/graphiql', graphiqlKoa({ endpointURL: '/graphql' }))

app
  .use(cacheBustTokens)
  .use((ctx, next) => authorization(ctx, next, app.env))
  .use(router.routes())
  .use(router.allowedMethods())
  .listen(8081)
