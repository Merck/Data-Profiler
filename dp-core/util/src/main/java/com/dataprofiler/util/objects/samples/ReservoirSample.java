package com.dataprofiler.util.objects.samples;

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

public class ReservoirSample implements Comparable<ReservoirSample> {

  private Long rand;
  private String value;
  private String count;

  public ReservoirSample() {}

  public ReservoirSample(Long rand, String value, String count) {
    this.rand = rand;
    this.value = value;
    this.count = count;
  }

  public void setRand(Long rand) {
    this.rand = rand;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public void setCount(String count) {
    this.count = count;
  }

  public Long getRand() {
    return this.rand;
  }

  public String getValue() {
    return this.value;
  }

  public String getCount() {
    return this.count;
  }

  @Override
  public int compareTo(ReservoirSample o) {
    int result = this.rand.compareTo(o.rand);
    return result;
  }
}
