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
import React, { useState, useEffect } from 'react'
import SwaggerUI from 'swagger-ui-react'
import { Button, CircularProgress } from '@material-ui/core'
import { isEqual } from 'lodash'
import { downloadJsObj } from '@dp-ui/lib/dist/helpers/download'
import { DPContext } from '@dp-ui/lib'
import 'swagger-ui-react/swagger-ui.css'

const REQUEST_PARAM_TO_FILTER = {
  schema: { type: 'request' },
  in: 'query',
  name: 'request',
  required: true,
}

const getSpec = (api, setSpec) =>
  api
    .get({ resource: 'assets/swagger.json', withoutAuth: true })
    .then((res) => {
      setSpec({
        ...res.body,
        tags: res.body.tags.filter((e) => e.name !== 'routes'),
        paths: Object.keys(res.body.paths).reduce((pathsAcc, path) => {
          const output = Object.keys(res.body.paths[path]).reduce(
            (methodsAcc, method) => {
              const current = res.body.paths[path][method]
              const filtered = {
                ...current,
                parameters: (current.parameters || []).filter(
                  (e) => !isEqual(e, REQUEST_PARAM_TO_FILTER)
                ),
              }
              return { ...methodsAcc, [method]: filtered }
            },
            {}
          )

          return { ...pathsAcc, [path]: output }
        }, {}),
        servers: [
          { url: api.base_url_path, description: process.env.CLUSTER_NAME },
        ],
      })
    })

const Swagger = ({ api }) => {
  const [spec, setSpec] = useState(null)
  useEffect(() => {
    getSpec(api, setSpec)
  }, [])
  return spec ? (
    <React.Fragment>
      <SwaggerUI docExpansion="none" spec={spec} />
      <Button
        color="primary"
        variant="contained"
        onClick={() => downloadJsObj(spec, 'swagger.json')}>
        Download Swagger Schema
      </Button>
    </React.Fragment>
  ) : (
    <CircularProgress />
  )
}

export default DPContext(Swagger)
