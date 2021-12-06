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
import { generateInitialState as previewDrawerInitialState } from '../../Drawers/reducer'
import { StoreState } from '../../index'
import { generateInitialState as listviewInitalState } from '../../ListView/reducer'
import SelectedDrilldown from '../../drilldown/models/SelectedDrilldown'
import { generateInitialState as multisearchInitialState } from '../../MultiSearch/reducer'
import { generateInitialState as orderbyInitialState } from '../../orderby/reducer'
import { generateInitialState as treeMapInitialState } from '../../reducer'
import { generateInitialState as commentsInitialState } from '../../comments/reducer'
import { generateInitialState as drilldownInitialState } from '../../drilldown/reducer'
import ColumnCountsAdapter from '../../services/ColumnCountsAdapter'
import MetadataAdapter from '../../services/MetadataAdapter'
import ColumnCountStub from './ColumnCount.stub'
import MetadataStub from './Metadata.stub'
import MetadataResultStub from './MetadataResult.stub'
import SearchResultStub from './SearchResult.stub'
import TreemapObjectStub from './TreemapObject.stub'
import CommentsStub from './Comments.stub'
import AggregratedSearchResultStub from './AggregratedSearchResult.stub'

export default class StoreStateStub {
  static genStoreState(...options: Readonly<Array<WITH_STATE>>): StoreState {
    const treemap = treeMapInitialState()
    const multisearch = multisearchInitialState()
    const previewdrawer = previewDrawerInitialState()
    const listview = listviewInitalState()
    const orderby = orderbyInitialState()
    const comments = commentsInitialState()
    const drilldown = drilldownInitialState()

    let store = {
      treemap,
      multisearch,
      previewdrawer,
      listview,
      orderby,
      comments,
      drilldown,
    }
    // WARNING: this is pretty specific to treemap view
    // WARNING: this is a little order specific, drilldowns should be applied before suggestions
    // TODO: make this list sorted and apply in the correct order
    // TODO: add the selected view and make less treemap view specific
    options.forEach((option) => {
      switch (option) {
        case WITH_STATE.DATA:
          store = StoreStateStub.addTreemapObject(store)
          break
        case WITH_STATE.SUGGESTIONS:
          store = StoreStateStub.addSuggestions(store)
          break
        case WITH_STATE.DATASET_DRILLDOWN:
          store = StoreStateStub.addDatasetDrilldown(store)
          break
        case WITH_STATE.TABLE_DRILLDOWN:
          store = StoreStateStub.addTableDrilldown(store)
          break
        case WITH_STATE.PREVIEW_DRAWER:
          store = StoreStateStub.addPreviewDrawer(store)
          break
        case WITH_STATE.COMMENTS:
          store = StoreStateStub.addComments(store)
          break
        default:
          const msg = 'unknown store state stub options flag, moving on...'
          console.warn(msg, option)
          break
      }
    })
    return store
  }

  static addTreemapObject(store: StoreState): StoreState {
    const data = TreemapObjectStub.genDataStub()
    store.treemap.treemapData = [data]
    return store
  }

  static addSuggestions(store: StoreState): StoreState {
    const multisearch = store.multisearch
    const selectedDrilldown = SelectedDrilldown.of(store.drilldown)
    multisearch.searchSuggestions = SearchResultStub.genMultiValueResults()
    multisearch.datasetSuggestions =
      AggregratedSearchResultStub.genDatasetHits(selectedDrilldown)
    multisearch.tableSuggestions =
      AggregratedSearchResultStub.genTableHits(selectedDrilldown)
    multisearch.columnSuggestions =
      AggregratedSearchResultStub.genColumnHits(selectedDrilldown)
    return store
  }

  static addDatasetDrilldown(store: StoreState): StoreState {
    store.drilldown = new SelectedDrilldown('d1').serialize()
    return store
  }

  static addTableDrilldown(store: StoreState): StoreState {
    store.drilldown = new SelectedDrilldown(
      'd1',
      't1'
    ).serialize()
    return store
  }

  static addPreviewDrawer(store: StoreState): StoreState {
    const drawer = store.previewdrawer
    drawer.isOpen = true
    const endpointData = ColumnCountStub.columnCountsEndpointResponse()
    const adapter = new ColumnCountsAdapter()
    drawer.columnCounts = adapter.endpointToColumnCounts(endpointData)
    const drilldown = new SelectedDrilldown(
      ''
    ).serialize()
    drawer.drawerSelectedDrilldown = drilldown
    // drawer.commentLevel = drilldown
    const metadataAdapter = new MetadataAdapter()
    const datasetMetadata = MetadataStub.genDatasetMetadata()
    const commonMetadata =
      metadataAdapter.columnMetadataToCommonFormat(datasetMetadata)
    drawer.datasetsMeta = commonMetadata
    return store
  }

  static addComments(store: StoreState): StoreState {
    store.comments.comments = CommentsStub.genSampleHierarchyEndpoint()
    const drilldown = new SelectedDrilldown(
      'd1'
    ).serialize()
    store.comments.commentLevel = drilldown
    return store
  }
}

export enum WITH_STATE {
  SUGGESTIONS,
  DATASET_DRILLDOWN,
  TABLE_DRILLDOWN,
  DATA,
  FILTER,
  PREVIEW_DRAWER,
  COMMENTS,
}
