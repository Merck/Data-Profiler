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
import { isBoolean, isEmpty } from 'lodash'

/**
 * class represents the clicked drill down of treemap cells, datasets and tables
 * also represents the current state of the breadcrumb trail
 */
export default class SelectedDrilldown {
  dataset?: string
  table?: string
  column?: string
  // show comments in the preview drawer?
  // TODO: clean up needed, these drawer comments booleans are not always needed
  //  sand are copied to redux in too many places
  showComments?: boolean
  showAddComment?: boolean

  constructor(
    dataset?: string,
    table?: string,
    column?: string,
    showComments = false,
    showAddComment = false
  ) {
    this.dataset = dataset
    this.table = table
    this.column = column
    this.showComments = showComments
    this.showAddComment = showAddComment
  }

  /**
   * factory method to construct a selected drill down from a javascript object
   * @param obj
   */
  static of(obj?: Readonly<Record<string, any>>): SelectedDrilldown {
    if (!obj) {
      return new SelectedDrilldown()
    }
    const { dataset, table, column, showComments, showAddComment, ...rest } =
      obj
    return new SelectedDrilldown(
      dataset,
      table,
      column,
      showComments,
      showAddComment
    )
  }

  /**
   * factory method to copy clone a selected drilldown object
   * @param obj
   */
  static from(obj?: Readonly<SelectedDrilldown>): SelectedDrilldown {
    if (!obj) {
      return new SelectedDrilldown()
    }
    const { dataset, table, column, showComments, showAddComment, ...rest } =
      obj
    return new SelectedDrilldown(
      dataset,
      table,
      column,
      showComments,
      showAddComment
    )
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
      dataset: obj.dataset,
      table: obj.table,
      column: obj.column,
      showComments: obj.showComments,
      showAddComment: obj.showAddComment,
    }
  }

  /**
   * a dataset, table and column are all selected
   * this represents a specific column view, as maybe seen in the preview drawer
   *
   * NOTE: this should not be confused with a general column level view as seen in the treemap
   *
   * @return true iff a dataset, table, and column are selected
   */
  isSpecificColumnView(): boolean {
    if (
      !isEmpty(this.dataset) &&
      !isEmpty(this.table) &&
      !isEmpty(this.column)
    ) {
      return true
    }
    return false
  }

  /**
   * a dataset is selected and a table is selected, but not a column
   * this represents a general column level view from the treemap perspective
   *
   * NOTE: this should not to be confused with a specific column level view
   *
   * @return true iff a dataset and table are selected. note: a column element is not selected
   */
  isColumnView(): boolean {
    if (
      !isEmpty(this.dataset) &&
      !isEmpty(this.table) &&
      isEmpty(this.column)
    ) {
      return true
    }
    return false
  }

  /**
   * a dataset is selected but no tables are selected
   * @return true iff a dataset is selected
   */
  isTableView(): boolean {
    if (!isEmpty(this.dataset) && isEmpty(this.table)) {
      return true
    }
    return false
  }

  /**
   * no dataset is selected and no tables are selected
   * @return true iff no dataset(s) are selected
   */
  isDatasetView(): boolean {
    if (isEmpty(this.dataset) && isEmpty(this.table)) {
      return true
    }
    return false
  }

  isMoreSpecificThanTableView(): boolean {
    return this.isColumnView() || this.isSpecificColumnView()
  }

  /**
   * @return TREEMAP_VIEW_ENUM
   */
  currentView(): TREEMAP_VIEW_ENUM {
    // TODO: maybe delete this all datasets view state
    // i have to switch tasks and this might need clean up
    // if (this.isEmpty()) {
    //   return TREEMAP_VIEW_ENUM.ALL_DATASETS
    if (this.isSpecificColumnView()) {
      return TREEMAP_VIEW_ENUM.SPECIFIC_COLUMN
    } else if (this.isColumnView()) {
      return TREEMAP_VIEW_ENUM.COLUMN
    } else if (this.isTableView()) {
      return TREEMAP_VIEW_ENUM.TABLE
    } else {
      // view all datasets probably
      return TREEMAP_VIEW_ENUM.DATASET
    }
  }

  /**
   * @return
   * true iff this object has no selected elements
   */
  isEmpty(): boolean {
    if (isEmpty(this.dataset) && isEmpty(this.table) && isEmpty(this.column)) {
      return true
    }
    return false
  }

  /**
   * @return
   * true iff this object has selected elements, and the showComments flag to true
   */
  shouldShowComments(): boolean {
    return isBoolean(this.showComments) && this.showComments
  }

  /**
   * @return
   * true iff this object has selected elements, and the showAddComment flag to true
   */
  shouldShowAddComment(): boolean {
    return isBoolean(this.showAddComment) && this.showAddComment
  }

  /**
   * @return
   * true iff this object has the same values as the given object
   */
  equals(drilldown?: SelectedDrilldown): boolean {
    if (!drilldown) {
      return false
    }
    const rhs = this
    const lhs = drilldown
    return (
      rhs.dataset === lhs.dataset &&
      rhs.table === lhs.table &&
      rhs.column === lhs.column &&
      rhs.showComments === lhs.showComments &&
      rhs.showAddComment === lhs.showAddComment
    )
  }
}

export enum TREEMAP_VIEW_ENUM {
  // ALL_DATASETS = 'all datasets',
  DATASET = 'dataset',
  TABLE = 'table',
  COLUMN = 'column',
  SPECIFIC_COLUMN = 'specific column',
}
