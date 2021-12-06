/**
*  Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
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
**/
package objects;

import com.dataprofiler.util.objects.ColumnCountIndexObject;

public class SearchResult {
  private String dataset;
  private String table;
  private String column;
  private String value;
  private Long count;

  public SearchResult(ColumnCountIndexObject ccio) {
    this.dataset = ccio.getDataset();
    this.table = ccio.getTableId();
    this.column = ccio.getColumn();
    this.value = ccio.getValue();
    this.count = ccio.getCount();
  }

  public String getDataset() {
    return dataset;
  }

  public void setDataset(String dataset) {
    this.dataset = dataset;
  }

  public String getTable() {
    return table;
  }

  public void setTable(String table) {
    this.table = table;
  }

  public String getColumn() {
    return column;
  }

  public void setColumn(String column) {
    this.column = column;
  }

  public Long getCount() {
    return count;
  }

  public void setCount(Long count) {
    this.count = count;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  @java.lang.Override
  public java.lang.String toString() {
    return "SearchResult{"
        + "dataset='"
        + dataset
        + '\''
        + ", table='"
        + table
        + '\''
        + ", column='"
        + column
        + '\''
        + ", count="
        + count
        + '}';
  }
}
