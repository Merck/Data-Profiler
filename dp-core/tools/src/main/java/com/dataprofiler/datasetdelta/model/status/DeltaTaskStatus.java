package com.dataprofiler.datasetdelta.model.status;

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

import java.time.Duration;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class DeltaTaskStatus {
  public boolean success;
  public String dataset;
  public int deltasUpdated;
  public Duration duration;

  public DeltaTaskStatus() {
    super();
    deltasUpdated = 0;
    duration = Duration.ZERO;
  }

  public String getDataset() {
    return dataset;
  }

  public DeltaTaskStatus setDataset(String dataset) {
    this.dataset = dataset;
    return this;
  }

  public int getDeltasUpdated() {
    return deltasUpdated;
  }

  public DeltaTaskStatus setDeltasUpdated(int deltasUpdated) {
    this.deltasUpdated = deltasUpdated;
    return this;
  }

  public Duration getDuration() {
    return duration;
  }

  public DeltaTaskStatus setDuration(Duration duration) {
    this.duration = duration;
    return this;
  }

  public boolean isSuccess() {
    return success;
  }

  public DeltaTaskStatus setSuccess(boolean success) {
    this.success = success;
    return this;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }

  @Override
  public boolean equals(Object rhs) {
    return EqualsBuilder.reflectionEquals(this, rhs);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this, false);
  }
}
