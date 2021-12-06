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
import { api } from '@dp-ui/lib'
import { encodeVariable } from '@dp-ui/lib/dist/helpers/strings'
import { isEmpty } from 'lodash'

/**
 *
 * @param datasetName
 * @returns json obj
 */
const getMetadataProperties = (
  datasetName: string
): Promise<Record<string, any>> => {
  return new Promise((resolve) => {
    const dataset = datasetName.trim()
    if (isEmpty(dataset)) {
      return resolve({})
    }
    return api()
      .post({
        resource: 'v2/datasets/graphql',
        postObject: {
          query: `{metadataByDatasetName(id: "${encodeVariable(
            dataset
          )}"){properties{ 
          chartData 
          performance
          numUsersWithAttribute
          delta
          updatedOnMillis
          quality
          description
        }}}`,
        },
      })
      .then((res) => {
        resolve(res.body)
      })
      .catch((err) => {
        console.log(err)
        resolve({ err: 'getMetadataProperties Failed' })
      })
  })
}

/**
 * @param datasetName
 */
const getDatasetProperties = (
  datasetName?: string
): Promise<Record<string, any>> => {
  return new Promise((resolve) => {
    const dataset = datasetName.trim()
    if (isEmpty(dataset)) {
      return resolve({})
    }

    return api()
      .get({ resource: `${encodeVariable(dataset)}/properties` })
      .then((res) => resolve(res.body))
      .catch((err) => {
        console.log(err)
        resolve({ err: 'getDatasetProperties Failed' })
      })
  })
}

/**
 * @param datasetName
 */
const getDatasetDescription = (datasetName?: string): Promise<string> => {
  return new Promise((resolve) => {
    const dataset = datasetName.trim()
    if (isEmpty(dataset)) {
      return resolve('')
    }

    return getDatasetProperties(dataset).then((obj) =>
      resolve(obj?.description || '')
    )
  })
}

/**
 * @param datasetName
 */
const setDatasetDescription = (
  datasetName: string,
  description: string
): Promise<void> => {
  return new Promise((resolve) => {
    const dataset = datasetName
    if (isEmpty(dataset)) {
      return resolve()
    }

    const properties = {
      description,
    }
    return api()
      .post({
        resource: `${encodeVariable(dataset)}/properties`,
        postObject: properties,
      })
      .then(() => resolve())
      .catch((err) => {
        console.log('setDatasetDescription Failed')
        console.log(err)
        resolve()
      })
  })
}

export {
  getMetadataProperties,
  getDatasetProperties,
  getDatasetDescription,
  setDatasetDescription,
}
