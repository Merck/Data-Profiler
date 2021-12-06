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
import { SelectedViewState } from './SelectedViewState'

export enum SELECTED_VIEW_ENUM {
  TREEMAP = 'treemap',
  LIST = 'list',
  ROW_VIEWER = 'row_viewer',
}

/**
 * class represents the selected view, treemap, list view, etc
 */
export default class SelectedView {
  static DEFAULT_VIEW = SELECTED_VIEW_ENUM.LIST
  view: SELECTED_VIEW_ENUM

  constructor(view: SELECTED_VIEW_ENUM = SelectedView.DEFAULT_VIEW) {
    this.view = view
  }

  /**
   * factory method to construct a selected drill down from a javascript object
   * @param obj
   */
  static of(obj?: Readonly<Record<string, any>>): SelectedView {
    if (!obj) {
      return new SelectedView()
    }
    const { view, ...rest } = obj
    return new SelectedView(view)
  }

  /**
   * factory method to copy clone a selected view object
   * @param obj
   */
  static from(obj?: Readonly<SelectedView>): SelectedView {
    if (!obj) {
      return new SelectedView()
    }
    const { view, ...rest } = obj
    return new SelectedView(view)
  }

  /**
   * Attempt to satisfy serialization for redux store
   *
   * @see "A non-serializable value was detected in the state, in the path"
   * @see https://redux.js.org/faq/organizing-state#can-i-put-functions-promises-or-other-non-serializable-items-in-my-store-state
   */
  serialize(): Record<string, any> {
    const obj = this
    return {
      view: obj.view,
    }
  }

  /**
   * @return SELECTED_VIEW_ENUM
   */
  currentView(): SELECTED_VIEW_ENUM {
    return this.view
  }

  /**
   * @return
   * true iff this object has the same values as the given object
   */
  equals(selectedView?: SelectedView): boolean {
    if (!selectedView) {
      return false
    }
    const rhs = this
    const lhs = selectedView
    return rhs.view === lhs.view
  }

  calcToolbarState(rowViewer: any): SelectedViewState {
    const isTreemapView = this?.view === SELECTED_VIEW_ENUM.TREEMAP
    const isListView = this?.view === SELECTED_VIEW_ENUM.LIST
    const isRowView = this?.view === SELECTED_VIEW_ENUM.ROW_VIEWER
    const isTreemapLaunchedRowView =
      isRowView && rowViewer?.previousViewMode === SELECTED_VIEW_ENUM.TREEMAP
    const isListViewLaunchedRowView =
      isRowView && rowViewer?.previousViewMode === SELECTED_VIEW_ENUM.LIST
    return {
      isTreemapView,
      isListView,
      isRowView,
      isTreemapLaunchedRowView,
      isListViewLaunchedRowView,
    }
  }
}
