package com.dataprofiler.util.objects.ui;

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

import com.dataprofiler.util.objects.ColumnCountObject;

public class ValueCountObject {
  public String n = "";
  public String v = "";
  public long c = 0L;

  public ValueCountObject(
      ColumnCountObject colCntObj, Boolean normalize, Boolean includeVisibility) {
    this.n = normalize ? colCntObj.value.toLowerCase().trim() : colCntObj.value;
    this.c = colCntObj.count;
    this.v = includeVisibility ? colCntObj.getVisibility() : v;
  }

  @Override
  public String toString() {
    return "ValueCountObject{"
        + "n='"
        + n
        + '\''
        + ", c="
        + c
        + (!v.equals("") ? ", v='" + v + "'}" : '}');
  }
}
