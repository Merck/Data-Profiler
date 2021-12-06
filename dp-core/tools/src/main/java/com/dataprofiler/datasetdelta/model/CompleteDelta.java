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

import static java.lang.String.format;
import static java.util.Comparator.comparingLong;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toSet;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompleteDelta {
  protected static final Logger logger = LoggerFactory.getLogger(CompleteDelta.class);

  protected long updatedOnMillis;
  protected DatasetVersion lastKnownVersions;
  protected Set<DatasetDelta> deltas;

  public CompleteDelta() {
    super();
    ZoneOffset zone = ZoneId.systemDefault().getRules().getOffset(Instant.now());
    this.updatedOnMillis = LocalDateTime.now().toEpochSecond(zone) * 1000;
    this.deltas = new TreeSet<>();
  }

  /**
   * merges dataset deltas sets across the two deltas
   *
   * <p>ignores linked comments
   *
   * @param rhsDelta
   * @return
   */
  public CompleteDelta mergeDeltas(CompleteDelta rhsDelta) {
    if (isNull(rhsDelta)) {
      return this;
    }

    String dataset = "";
    if (nonNull(lastKnownVersions) && nonNull(lastKnownVersions.getDataset())) {
      dataset = lastKnownVersions.getDataset();

      if (nonNull(rhsDelta.getLastKnownVersions())
          && nonNull(rhsDelta.getLastKnownVersions().getDataset())) {
        String rhsDataset = rhsDelta.getLastKnownVersions().getDataset();
        if (!dataset.equalsIgnoreCase(rhsDataset)) {
          throw new IllegalArgumentException(
              "cannot merge complete deltas from two different dataset found: "
                  + dataset
                  + " and rhs dataset: "
                  + rhsDataset);
        }
      }
    }

    SortedSet<DatasetDelta> mergedDeltas =
        new TreeSet<>(comparingLong(DatasetDelta::getDatasetUpdatedOnMillis).reversed());
    Set<DatasetDelta> lhsDeltas = this.getDeltas();
    Set<DatasetDelta> rhsDeltas = rhsDelta.getDeltas();
    int rhsDeltaNum = rhsDeltas.size();
    int newDeltaNum = lhsDeltas.size();
    for (DatasetDelta datasetDelta : lhsDeltas) {
      if (rhsDeltas.contains(datasetDelta)) {
        Optional<DatasetDelta> matchingDelta =
            rhsDeltas.stream().filter(current -> current.equals(datasetDelta)).findFirst();
        if (matchingDelta.isPresent() && matchingDelta.get().hasLinkedComment()) {
          logger.debug(
              format(
                  "found duplicate delta, using previously linked comment - %s",
                  matchingDelta.get().getComment().getUuid()));
          // favor deltas with existing comments linked
          mergedDeltas.add(matchingDelta.get());
        } else {
          logger.debug(format("found duplicate delta, no previously linked comment"));
          mergedDeltas.add(datasetDelta);
        }
      } else {
        logger.debug(format("no duplicate delta found %s", datasetDelta));
        mergedDeltas.add(datasetDelta);
      }
    }

    // favor deltas with existing comments linked
    // favor left hand side deltas over right hand side deltas
    Set<DatasetDelta> leftOverRhsDeltas =
        rhsDeltas.stream().filter(current -> !lhsDeltas.contains(current)).collect(toSet());
    mergedDeltas.addAll(leftOverRhsDeltas);
    int totalDeltas = mergedDeltas.size();

    if (logger.isInfoEnabled()) {
      logger.info(
          format(
              "(%s + %s = %s) %s dataset deltas merged and de-duplicated",
              rhsDeltaNum, newDeltaNum, totalDeltas, dataset));
    }
    this.setDeltas(mergedDeltas);

    DatasetVersion rhsDatasetVersion = rhsDelta.getLastKnownVersions();
    Map<String, TableVersion> currentTables = lastKnownVersions.getTables();
    Map<String, TableVersion> rhsTables = rhsDatasetVersion.getTables();
    for (String tableKey : rhsTables.keySet()) {
      TableVersion tableVersion = rhsTables.get(tableKey);
      currentTables.merge(
          tableKey,
          tableVersion,
          (oldTableVersion, newTableVersion) -> {
            Map<String, String> oldColumns = oldTableVersion.getColumns();
            Map<String, String> newColumns = newTableVersion.getColumns();
            if (isNull(newColumns)) {
              newColumns = new HashMap<>();
            }
            Set<String> columnKeys = newColumns.keySet();
            for (String column : columnKeys) {
              String value = newColumns.get(column);
              oldColumns.merge(column, value, (oldColumn, newColumn) -> oldColumn);
            }
            oldTableVersion.setColumns(oldColumns);
            return oldTableVersion;
          });
    }

    lastKnownVersions.setTables(currentTables);
    return this;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }

  @Override
  public boolean equals(Object rhs) {
    return EqualsBuilder.reflectionEquals(this, rhs, "updatedOnMillis");
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this, false);
  }

  public long getUpdatedOnMillis() {
    return updatedOnMillis;
  }

  public void setUpdatedOnMillis(long updatedOnMillis) {
    this.updatedOnMillis = updatedOnMillis;
  }

  public Set<DatasetDelta> getDeltas() {
    if (isNull(deltas)) {
      return null;
    }

    return deltas.stream().map(DatasetDelta::new).collect(toSet());
  }

  public void setDeltas(Collection<DatasetDelta> deltas) {
    if (isNull(deltas)) {
      this.deltas = null;
      return;
    }

    this.deltas = deltas.stream().map(DatasetDelta::new).collect(toSet());
  }

  public DatasetVersion getLastKnownVersions() {
    return lastKnownVersions;
  }

  public void setLastKnownVersions(DatasetVersion lastKnownVersions) {
    this.lastKnownVersions = lastKnownVersions;
  }
}
