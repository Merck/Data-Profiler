package com.dataprofiler.datasetperformance;

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

import static java.lang.String.format;
import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.Objects.isNull;

import com.dataprofiler.datasetperformance.model.DatasetPerformance;
import com.dataprofiler.matomo.MatomoHelper;
import com.dataprofiler.rulesofuse.RulesOfUseException;
import com.dataprofiler.rulesofuse.RulesOfUseHelper;
import com.dataprofiler.rulesofuse.response.RouResponse;
import com.dataprofiler.rulesofuse.response.RouUser;
import com.dataprofiler.rulesofuse.response.RouUserData;
import com.dataprofiler.util.objects.VersionedDatasetMetadata;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatasetPerformanceProvider {
  private static final Logger logger = LoggerFactory.getLogger(DatasetPerformanceProvider.class);

  protected RulesOfUseHelper rouHelper;
  protected MatomoHelper matomoHelper;

  public DatasetPerformanceProvider() {
    super();
  }

  public DatasetPerformance calculateRouPerformance(
      VersionedDatasetMetadata metadata, Set<RouUser> contractors)
      throws DatasetPerformanceException {
    if (isNull(metadata)) {
      throw new IllegalArgumentException("metadata is null, cannot calculate dataset performance");
    }

    Instant start = now();
    String dataset = metadata.getDatasetMetadata().getDataset_name();
    if (logger.isDebugEnabled()) {
      logger.debug(format("calculating rou performance for %s", dataset));
    }

    RouResponse rouResponse = numberOfUsersWithAccess(metadata);
    if (logger.isTraceEnabled()) {
      logger.trace(rouResponse.toString());
    }
    DatasetPerformance performance = new DatasetPerformance();
    performance.setDataset(dataset);
    performance.setNumUsersWithAttribute(rouResponse.getData().getNumUsersWithAttribute());
    performance.setNumUserWithAttributeAndContractor(
        numberOfContractorsWithAccess(metadata, contractors));

    Instant end = now();
    if (logger.isInfoEnabled()) {
      Duration duration = between(start, end);
      logger.info(
          format("rou performance calculation finished for %s time: %s", dataset, duration));
    }
    return performance;
  }

  protected RouResponse numberOfUsersWithAccess(VersionedDatasetMetadata metadata)
      throws DatasetPerformanceException {
    Instant start = now();
    String vizExpression = metadata.getDatasetMetadata().getVisibility();
    // TODO: vizExpression might come in the form of an expression eg.
    // "LIST.PUBLIC&LIST.PUBLIC_DATA"
    // call rou for number of users with numUsersWithAttribute
    try {

      RouResponse activeUsers = rouHelper.queryNumUsersWithAttribute(vizExpression);

      Instant end = now();
      if (logger.isInfoEnabled()) {
        Duration duration = between(start, end);
        logger.info(format("query num users with: %s  time: %s", vizExpression, duration));
      }
      return activeUsers;
    } catch (RulesOfUseException e) {
      throw new DatasetPerformanceException(e);
    }
  }

  protected long numberOfContractorsWithAccess(
      VersionedDatasetMetadata metadata, Set<RouUser> contractors)
      throws DatasetPerformanceException {
    Instant start = now();
    String vizExpression = metadata.getDatasetMetadata().getVisibility();
    try {

      RouUserData data = rouHelper.queryUsersWithAttribute(vizExpression);
      Set<RouUser> activeUsers = data.getData().getImmutableUsersWithAttribute();
      if (logger.isDebugEnabled()) {
        logger.debug("calculating numberOfContractorsWithAccess");
        logger.debug(
            format("comparing %s contractors to %s users", contractors.size(), activeUsers.size()));
        activeUsers.stream().limit(10).forEach(el -> logger.debug(el.toString()));
        contractors.stream().limit(10).forEach(el -> logger.debug(el.toString()));
      }
      long numContractorsWithAccess = activeUsers.stream().filter(contractors::contains).count();

      Instant end = now();
      if (logger.isInfoEnabled()) {
        Duration duration = between(start, end);
        logger.info(
            format(
                "found %s contractors with: %s time: %s",
                numContractorsWithAccess, vizExpression, duration));
      }
      return numContractorsWithAccess;
    } catch (RulesOfUseException e) {
      throw new DatasetPerformanceException(e);
    }
  }

  public RulesOfUseHelper getRouHelper() {
    return rouHelper;
  }

  public void setRouHelper(RulesOfUseHelper rouHelper) {
    this.rouHelper = rouHelper;
  }

  public MatomoHelper getMatomoHelper() {
    return matomoHelper;
  }

  public void setMatomoHelper(MatomoHelper matomoHelper) {
    this.matomoHelper = matomoHelper;
  }
}
