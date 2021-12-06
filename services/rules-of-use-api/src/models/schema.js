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
import { merge } from 'lodash'
import { makeExecutableSchema } from 'graphql-tools'

import { typeDef as ApplicationSchema } from './application/schema.js'
import { typeDef as AttributeSchema } from './attribute/schema.js'
import { typeDef as UserSchema } from './user/schema.js'
import { typeDef as StatementSchema } from './statement/schema.js'
import { typeDef as RuleSchema } from './rule/schema.js'
import { typeDef as AuthenticationTokenCacheSchema } from './authenticationTokenCache/schema.js'

import { resolvers as applicationResolvers } from './application/resolvers.js'
import { resolvers as attributeResolvers } from './attribute/resolvers.js'
import { resolvers as statementResolvers } from './statement/resolvers.js'
import { resolvers as ruleResolvers } from './rule/resolvers.js'
import { resolvers as userResolvers } from './user/resolvers.js'
import { resolvers as authenticationTokenCacheResolvers } from './authenticationTokenCache/resolvers.js'
import { GraphQLDateTime } from 'graphql-iso-date'

const typeDefs = `
  scalar Date
  type Query {
    _empty: String
  }

  type Mutation {
    _empty: String
  }
`
const customScalarResolver = {
  Date: GraphQLDateTime
}

const resolvers = {}
const schema = makeExecutableSchema({
  typeDefs: [
    typeDefs,
    ApplicationSchema,
    AttributeSchema,
    StatementSchema,
    RuleSchema,
    UserSchema,
    AuthenticationTokenCacheSchema
  ],
  resolvers: merge(
    resolvers,
    applicationResolvers,
    attributeResolvers,
    statementResolvers,
    ruleResolvers,
    userResolvers,
    authenticationTokenCacheResolvers,
    customScalarResolver
  )
})

export default schema
