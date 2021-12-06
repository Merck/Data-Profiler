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
import { combineReducers, configureStore } from '@reduxjs/toolkit'
import React from 'react'
import { Provider } from 'react-redux'
import { Route } from 'react-router-dom'
import comments, { CommentsState } from './comments/reducer'
import previewdrawer, { PreviewDrawerState } from './Drawers/reducer'
import listview, { ListViewState } from './ListView/reducer'
import multisearch, { SearchState } from './MultiSearch/reducer'
import orderby, { OrderByState } from './orderby/reducer'
import treemap, { TreemapState } from './reducer'
import drilldown, { SelectedDrilldownState } from './drilldown/reducer'
import UniverseTreemap from './UniverseTreemap'

export interface StoreState {
  multisearch: SearchState
  treemap: TreemapState
  previewdrawer: PreviewDrawerState
  listview: ListViewState
  orderby: OrderByState
  comments: CommentsState
  drilldown: SelectedDrilldownState
}

export const store = configureStore({
  reducer: combineReducers({
    treemap,
    multisearch,
    previewdrawer,
    listview,
    orderby,
    comments,
    drilldown,
  }),
})

const Treemap = ({ match }) => {
  return (
    <Provider store={store}>
      <Route exact path={`${match.path}`} component={UniverseTreemap} />
    </Provider>
  )
}

export default Treemap
