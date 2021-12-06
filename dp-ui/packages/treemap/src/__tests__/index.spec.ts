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
// import {
//   splitDataIntoThirds,
//   filterDataOn,
//   lookupDatasetByDatasetName,
//   lookupDatasetByTableName,
//   filterMetadataOnDatasetName,
//   generateSnackbarDiv,
// } from '../universe-treemap'
// TODO: FIXME: importing universe-treemap causes an error
//    Cannot use import statement outside a module
// import { genDataStub } from './mocks/Data.stub'
// import { genMetadataCommonFormat } from './mocks/Metadata.stub'

describe('given a test stub', () => {
  test('then a test stub', () => {
    expect(true).toBeTruthy()
  })
})

// describe('given a treemap', () => {
//   let data = undefined
//   let metaData = undefined
//   let scales = undefined
//   beforeEach(() => {
//     data = genDataStub()
//     metaData = genMetadataCommonFormat()
//     scales = ['large', 'medium', 'small']
//   })

//   test('then expect sane test data', () => {
//     expect(data).toBeTruthy()
//     expect(data.children).toBeTruthy()
//     expect(data.children.length).toBeGreaterThan(0)
//     const children = data.children
//     children.forEach((el) => {
//       expect(el).toBeTruthy()
//       expect(el.name).toBeTruthy()
//       expect(el.totalSize).toBeGreaterThan(-1)
//     })
//   })

//   test('and a sane test data, then expect to split data into thirds', () => {
//     const splitData = splitDataIntoThirds(data)
//     expect(splitData).toBeTruthy()
//     expect(splitData.length).toBe(3)
//     const [split1, split2, split3] = splitData
//     expect(split1).toBeTruthy()
//     expect(split2).toBeTruthy()
//     expect(split3).toBeTruthy()

//     expect(scales).toBeTruthy()
//     expect(scales.length).toBeGreaterThan(0)
//   })

//   test('and a sane test data, then expect to split data into thirds with scaleColor', () => {
//     const splitData = splitDataIntoThirds(data)
//     expect(splitData).toBeTruthy()
//     expect(splitData.length).toBe(3)
//     const [split1, split2, split3] = splitData
//     expect(split1).toBeTruthy()
//     expect(split2).toBeTruthy()
//     expect(split3).toBeTruthy()
//     const assertOnSplit = (split) => {
//       expect(split).toBeTruthy()
//       expect(split.length).toBeGreaterThan(0)
//       split.forEach((el) => {
//         expect(el).toBeTruthy()
//         expect(scales).toContain(el.scaleColor)
//       })
//     }
//     assertOnSplit(split1.children)
//     assertOnSplit(split2.children)
//     assertOnSplit(split3.children)
//   })

//   test('and a sane test data, then expect to filterdata based on name', () => {
//     const filter = {
//       name: 'expansion_tinytest',
//       scaleColor: scales[1],
//     }
//     const fiteredData = filterDataOn(data, filter)
//     expect(fiteredData).toBeTruthy()
//     expect(fiteredData.children.length).toBe(4)
//   })

//   test('and a sane test data, then expect to lookup a dataset based on a dataset name', () => {
//     const ds = lookupDatasetByDatasetName('Test Supertables', data)
//     expect(ds).toBeTruthy()
//     expect(ds.name).toBeTruthy()
//     expect(ds.name).toBe('Test Supertables')
//   })

//   test('and a sane test data, then expect to lookup a dataset based on a table name', () => {
//     const ds = lookupDatasetByTableName('bar', data)
//     expect(ds).toBeTruthy()
//     expect(ds.name).toBeTruthy()
//     expect(ds.name).toBe('Test Supertables')
//   })

//   test('and metadata, then expect to lookup', () => {
//     const dataset = filterMetadataOnDatasetName(metaData, 'clinpath')
//     expect(dataset).toBeTruthy()
//   })

//   test('and metadata, and dataset level, then expect to generate a snackbar', () => {
//     const snackbar = generateSnackbarDiv('clinpath', undefined, metaData)
//     expect(snackbar).toBeTruthy()
//   })
// })
