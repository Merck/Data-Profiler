package com.dataprofiler.util.objects.response;

/*-
 * 
 * dataprofiler-util
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Rows {

  private List<Map<String, String>> rows;
  private List<String> sortedColumns;
  private String endLocation;

  Rows(List<Map<String, String>> list) {
    this.rows.addAll(list);
  }

  public Rows() {
    this.rows = new ArrayList<>();
  }

  public List<Map<String, String>> getRows() {
    return rows;
  }

  public void setRows(List<Map<String, String>> rows) {
    this.rows = rows;
  }

  public Integer getCount() {
    return this.rows.size();
  }

  public String getEndLocation() {
    return endLocation;
  }

  public void setEndLocation(String endLocation) {
    this.endLocation = endLocation;
  }

  public List<String> getSortedColumns() {
    return sortedColumns;
  }

  public void setSortedColumns(List<String> sortedColumns) {
    this.sortedColumns = sortedColumns;
  }

  public void addRecord(Map<String, String> row) {
    rows.add(row);
  }
}
