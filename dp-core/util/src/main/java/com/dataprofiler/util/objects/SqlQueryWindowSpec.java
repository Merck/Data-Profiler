package com.dataprofiler.util.objects;

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

public class SqlQueryWindowSpec {
  private String name;
  private String startDifference;
  private String startTimestamp;
  private String endDifference;
  private String endTimestamp;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getStartDifference() {
    return startDifference;
  }

  public void setStartDifference(String start_difference) {
    this.startDifference = start_difference;
  }

  public String getStartTimestamp() {
    return startTimestamp;
  }

  public void setStartTimestamp(String start_timestamp) {
    this.startTimestamp = start_timestamp;
  }

  public String getEndDifference() {
    return endDifference;
  }

  public void setEndDifference(String end_difference) {
    this.endDifference = end_difference;
  }

  public String getEndTimestamp() {
    return endTimestamp;
  }

  public void setEndTimestamp(String end_timestamp) {
    this.endTimestamp = end_timestamp;
  }

  @Override
  public String toString() {
    return "SqlQueryWindowSpec{"
        + "name='"
        + name
        + '\''
        + ", startDifference='"
        + startDifference
        + '\''
        + ", startTimestamp='"
        + startTimestamp
        + '\''
        + ", endDifference='"
        + endDifference
        + '\''
        + ", endTimestamp='"
        + endTimestamp
        + '\''
        + '}';
  }
}
