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
/**
 *
 * @param {*} bounds
 *   expects a DOMRect of the HTML Element bounds
 * @param {*} y - mouse y
 * @param {*} numRegions - number of horizontal regions to split the plane
 */
function isLowerNthRegion(
  bounds: Partial<DOMRect>,
  y: number,
  numRegions: number
): boolean {
  // DOM Rect sample
  // x: 64
  // y: 125
  // width: 928
  // height: 779
  // top: 125
  // right: 992
  // bottom: 904
  // left: 64
  const trueHeight = bounds.y + bounds.height
  const regionSize = trueHeight / numRegions
  const upperRegion = trueHeight - regionSize
  return y >= upperRegion
}

/**
 *
 * @param {*} bounds
 *   expects a DOMRect of the HTML Element bounds
 * @param {*} y - mouse y
 * @param {*} numRegions - number of horizontal regions to split the plane
 */
function isUpperNthRegion(
  bounds: Partial<DOMRect>,
  y: number,
  numRegions: number
): boolean {
  return !isLowerNthRegion(bounds, y, numRegions)
}

/**
 *
 * @param {*} bounds
 *   expects a DOMRect of the HTML Element bounds
 * @param {*} y - mouse y
 */
function isLowerQuadrant(bounds: Partial<DOMRect>, y: number): boolean {
  return isLowerNthRegion(bounds, y, 2)
}

/**
 *
 * @param {*} bounds
 *   expects a DOMRect of the HTML Element bounds
 * @param {*} y - mouse y
 */
function isUpperQuadrant(bounds: Partial<DOMRect>, y: number): boolean {
  return !isLowerQuadrant(bounds, y)
}

/**
 *
 * @param {*} bounds
 *   expects a DOMRect of the HTML Element bounds
 * @param {*} x - mouse x
 * @param {*} numRegions - number of vertical regions through the plane
 */
function isRightNthQuadrant(
  bounds: Partial<DOMRect>,
  x: number,
  numRegions: number
): boolean {
  const trueWidth = bounds.x + bounds.width
  const regionSize = trueWidth / numRegions
  const leftRegion = trueWidth - regionSize
  return x >= leftRegion
}

/**
 *
 * @param {*} bounds
 *   expects a DOMRect of the HTML Element bounds
 * @param {*} x - mouse x
 * @param {*} numRegions - number of vertical regions through the plane
 */
function isLeftNthQuadrant(
  bounds: Partial<DOMRect>,
  x: number,
  numRegions: number
): boolean {
  return !isRightNthQuadrant(bounds, x, numRegions)
}

/**
 *
 * @param {*} bounds
 *   expects a DOMRect of the HTML Element bounds
 * @param {*} x - mouse x
 */
function isRightQuadrant(bounds: Partial<DOMRect>, x: number): boolean {
  return isRightNthQuadrant(bounds, x, 2)
}

/**
 *
 * @param {*} bounds
 *   expects a DOMRect of the HTML Element bounds
 * @param {*} x - mouse x
 */
function isLeftQuadrant(bounds: Partial<DOMRect>, x: number): boolean {
  return !isRightQuadrant(bounds, x)
}

/**
 *
 * @param {*} bounds
 *   expects a DOMRect of the HTML Element bounds
 * @param {*} x - mouse x
 * @param {*} y - mouse y
 */
function isLowerRightQuadrant(
  bounds: Partial<DOMRect>,
  x: number,
  y: number
): boolean {
  return isLowerQuadrant(bounds, y) && isRightQuadrant(bounds, x)
}

/**
 *
 * @param {*} bounds
 *   expects a DOMRect of the HTML Element bounds
 * @param {*} x - mouse x
 * @param {*} y - mouse y
 * @param {*} numRegions - number of vertical and horizontal regions to split the plane
 */
function isLowerRightNthQuadrant(
  bounds: Partial<DOMRect>,
  x: number,
  y: number,
  numRegions: number
): boolean {
  return isLowerNthRegion(bounds, y, numRegions) && isRightQuadrant(bounds, x)
}

export {
  isLowerNthRegion,
  isUpperNthRegion,
  isLeftNthQuadrant,
  isRightNthQuadrant,
  isLowerQuadrant,
  isUpperQuadrant,
  isRightQuadrant,
  isLeftQuadrant,
  isLowerRightQuadrant,
  isLowerRightNthQuadrant,
}
