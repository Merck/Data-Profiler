package com.dataprofiler.loader.config;

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

public class JsonFileParams {

  private String inputFilename;
  private String explodeBlackList;
  private Boolean multiLine = true;

  public JsonFileParams() {}

  public JsonFileParams(String inputFilename) {
    this.inputFilename = inputFilename;
  }

  public JsonFileParams(String inputFilename, Boolean multiLine, String explodeBlackList) {
    this.inputFilename = inputFilename;
    this.explodeBlackList = explodeBlackList;
    this.multiLine = multiLine;
  }

  public String getExplodeBlacklist() {
    return this.explodeBlackList;
  }

  public void setExplodeBlackList(String explodeBlackList) {
    this.explodeBlackList = explodeBlackList;
  }

  public String getInputFilename() {
    return this.inputFilename;
  }

  public void setInputFilename(String inputFilename) {
    this.inputFilename = inputFilename;
  }

  public Boolean getMultiLine() {
    return this.multiLine;
  }

  public void setMultiLine(Boolean multiLine) {
    this.multiLine = multiLine;
  }
}
