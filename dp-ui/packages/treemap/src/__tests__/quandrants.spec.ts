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
import {
  isLowerNthRegion,
  isUpperNthRegion,
  isLeftNthQuadrant,
  isLowerRightNthQuadrant,
  isLowerQuadrant,
  isUpperQuadrant,
  isLeftQuadrant,
  isRightQuadrant,
  isLowerRightQuadrant,
} from '../quandrants'

describe('given a bounding box and mouse x/y coordinates', () => {
  const boundingBox = {
    x: 64,
    y: 125,
    width: 928,
    height: 779,
    top: 125,
    right: 992,
    bottom: 904,
    left: 64,
  }

  test('then expect tests to have sane data', () => {
    expect(boundingBox).toBeTruthy()
    expect(boundingBox.x).toBeGreaterThan(0)
    expect(boundingBox.y).toBeGreaterThan(0)
    expect(boundingBox.width).toBeGreaterThan(0)
    expect(boundingBox.height).toBeGreaterThan(0)
  })

  test('then expect to calculate lower nth quadrant', () => {
    const y = 900
    expect(boundingBox).toBeTruthy()
    const isLower = isLowerNthRegion(boundingBox, y, 12)
    expect(isLower).toBe(true)
  })

  test('then expect to calculate outside lower nth quadrant', () => {
    const y = 200
    expect(boundingBox).toBeTruthy()
    const isLower = isLowerNthRegion(boundingBox, y, 12)
    expect(isLower).toBe(false)
  })

  test('then expect to calculate upper nth quadrant', () => {
    const y = 200
    expect(boundingBox).toBeTruthy()
    const isLower = isUpperNthRegion(boundingBox, y, 12)
    expect(isLower).toBe(true)
  })

  test('then expect to calculate outside upper nth quadrant', () => {
    const y = 900
    expect(boundingBox).toBeTruthy()
    const isLower = isUpperNthRegion(boundingBox, y, 12)
    expect(isLower).toBe(false)
  })

  test('then expect to calculate lower quadrant', () => {
    const y = 800
    expect(boundingBox).toBeTruthy()
    const isLower = isLowerQuadrant(boundingBox, y)
    expect(isLower).toBe(true)
  })

  test('then expect to calculate outside lower quadrant', () => {
    const y = 0
    expect(boundingBox).toBeTruthy()
    const isLower = isLowerQuadrant(boundingBox, y)
    expect(isLower).toBe(false)
  })

  test('then expect to calculate upper quadrant', () => {
    const y = 0
    expect(boundingBox).toBeTruthy()
    const isUpper = isUpperQuadrant(boundingBox, y)
    expect(isUpper).toBe(true)
  })

  test('then expect to calculate outside upper quadrant', () => {
    const y = 900
    expect(boundingBox).toBeTruthy()
    const isUpper = isUpperQuadrant(boundingBox, y)
    expect(isUpper).toBe(false)
  })

  test('then expect to calculate left nth quadrant', () => {
    const x = 100
    expect(boundingBox).toBeTruthy()
    const isLeft = isLeftNthQuadrant(boundingBox, x, 12)
    expect(isLeft).toBe(true)
  })

  test('then expect to calculate outside left nth quadrant', () => {
    const x = 900
    expect(boundingBox).toBeTruthy()
    const isLeft = isLeftNthQuadrant(boundingBox, x, 12)
    expect(isLeft).toBe(true)
  })

  test('then expect to calculate left quadrant', () => {
    const x = 100
    expect(boundingBox).toBeTruthy()
    const isLeft = isLeftQuadrant(boundingBox, x)
    expect(isLeft).toBe(true)
  })

  test('then expect to calculate outside left quadrant', () => {
    const x = 800
    expect(boundingBox).toBeTruthy()
    const isLeft = isLeftQuadrant(boundingBox, x)
    expect(isLeft).toBe(false)
  })

  test('then expect to calculate right quadrant', () => {
    const x = 800
    expect(boundingBox).toBeTruthy()
    const isRight = isRightQuadrant(boundingBox, x)
    expect(isRight).toBe(true)
  })

  test('then expect to calculate outside right quadrant', () => {
    const x = 100
    expect(boundingBox).toBeTruthy()
    const isRight = isRightQuadrant(boundingBox, x)
    expect(isRight).toBe(false)
  })

  test('then expect to calculate lower right quadrant', () => {
    const x = 800
    const y = 800
    expect(boundingBox).toBeTruthy()
    const isLowerRight = isLowerRightQuadrant(boundingBox, x, y)
    expect(isLowerRight).toBe(true)
  })

  test('then expect to calculate lower right nth quadrant', () => {
    const x = 850
    const y = 850
    const numRegions = 12
    expect(boundingBox).toBeTruthy()
    const isLowerRightNth = isLowerRightNthQuadrant(
      boundingBox,
      x,
      y,
      numRegions
    )
    expect(isLowerRightNth).toBe(true)
  })
})
