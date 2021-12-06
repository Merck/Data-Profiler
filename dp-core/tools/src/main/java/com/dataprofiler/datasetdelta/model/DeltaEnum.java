package com.dataprofiler.datasetdelta.model;

/*-
 * 
 * dataprofiler-tools
 *
 * Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
 *
 * 	Licensed to the Apache Software Foundation (ASF) under one
 * 	or more contributor license agreements. See the NOTICE file
 * 	distributed with this work for additional information
 * 	regarding copyright ownership. The ASF licenses this file
 * 	to you under the Apache License, Version 2.0 (the
 * 	"License"); you may not use this file except in compliance
 * 	with the License. You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * 	Unless required by applicable law or agreed to in writing,
 * 	software distributed under the License is distributed on an
 * 	"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * 	KIND, either express or implied. See the License for the
 * 	specific language governing permissions and limitations
 * 	under the License.
 * 
 */

public enum DeltaEnum {
  DATASET_RENAMED,
  TABLE_ADDED,
  TABLE_REMOVED,
  COLUMN_ADDED,
  COLUMN_REMOVED,
  DATASET_VALUES_INCREASED,
  DATASET_VALUES_DECREASED,
  TABLE_VALUES_INCREASED,
  TABLE_VALUES_DECREASED,
  COLUMN_VALUES_INCREASED,
  COLUMN_VALUES_DECREASED,
  NO_OP;
  // *_RENAMED?

  public boolean isDatasetEnum() {
    return this.name().contains("DATASET");
  }

  public boolean isTableEnum() {
    return this.name().contains("TABLE");
  }

  public boolean isColumnEnum() {
    return this.name().contains("COLUMN");
  }

  public boolean isValueEnum() {
    return this.name().contains("VALUES");
  }

  public boolean isAddedEnum() {
    return this.name().contains("ADDED");
  }

  public boolean isRemovedEnum() {
    return this.name().contains("REMOVED");
  }
}
