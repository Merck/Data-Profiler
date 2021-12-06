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

const fs = require('fs')
const uuid = require('uuid')
const unoconv = require('unoconv2')
const formats = require('../lib/data/formats.json')
const pkg = require('../package.json')

module.exports.handleUpload = (request, reply) => {
  const convertToFormat = request.params.format
  const data = request.payload
  if (data.file) {
    const nameArray = data.file.hapi.filename.split('.')
    const fileEndingOriginal = nameArray.pop()
    const temporaryName = uuid.v4()
    const pathPre = process.cwd() + '/uploads/' + temporaryName
    const fileNameTempOriginal = pathPre + '.' + fileEndingOriginal
    const file = fs.createWriteStream(fileNameTempOriginal)

    file.on('error', (error) => {
      console.error(error)
    })

    data.file.pipe(file)

    data.file.on('end', (err) => {
      if (err) {
        console.error(err)
        reply(err)
      } else {
        unoconv.convert(fileNameTempOriginal, convertToFormat, (err, result) => {
          if (err) {
            console.error(err)
            fs.unlink(fileNameTempOriginal, error => {
              if (error) {
                console.error(error)
              } else {
                console.log(`${fileNameTempOriginal} deleted`)
              }
            })
            reply(err)
          } else {
            console.log('finished converting')
            reply(result)
              .on('finish', () => {
                fs.unlink(fileNameTempOriginal, error => {
                  if (error) {
                    console.error(error)
                  } else {
                    console.log(`${fileNameTempOriginal} deleted`)
                  }
                })
              })
          }
        })
      }
    })
  }
}

module.exports.showFormats = (request, reply) => {
  reply(formats)
}

module.exports.showFormat = (request, reply) => {
  const params = request.params
  const format = params ? formats[request.params.type] : false
  if (!format) {
    reply('Format type not found').code(404)
  } else {
    reply(format)
  }
}

module.exports.showVersions = (request, reply) => {
  let versions = {}
  Object.keys(pkg.dependencies).forEach((item) => {
    versions[item] = pkg.dependencies[item]
  })
  reply(versions)
}

module.exports.healthcheck = (request, reply) => {
  reply({ uptime: process.uptime() })
}
