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
import { createSelector } from 'reselect'
import { StoreState } from '../index'
import ReorderCommonMetadata from '../models/ReorderCommonMetadata'
import ReorderMetadataService from '../services/ReorderMetadataService'

export const getDatasetsMeta = (state: StoreState) =>
  state.previewdrawer.datasetsMeta
export const getFilter = (state: StoreState) => state.treemap.filter
export const getHovered = (state: StoreState) => state.treemap.hoveredValue
export const getUserSelectedOrderBy = (state: StoreState) =>
  state.orderby.selectedOrderBy

export const getReorderCommonMetadata = createSelector(
  [getDatasetsMeta, getUserSelectedOrderBy, getFilter, getHovered],
  (datasetsMeta, selectedOrderBy, filter, hovered): ReorderCommonMetadata[] => {
    const reorderService = new ReorderMetadataService()
    const metadata = reorderService.filterHits(datasetsMeta, filter)
    const hits = hovered || filter
    return reorderService.reorderDatasets(
      metadata,
      hits,
      selectedOrderBy?.orderBy
    )
  }
)
