package com.dataprofiler.datasetdelta.diff;

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

import static com.dataprofiler.datasetdelta.model.DeltaEnum.COLUMN_ADDED;
import static com.dataprofiler.datasetdelta.model.DeltaEnum.COLUMN_REMOVED;
import static com.dataprofiler.datasetdelta.model.DeltaEnum.TABLE_ADDED;
import static com.dataprofiler.datasetdelta.model.DeltaEnum.TABLE_REMOVED;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

import com.dataprofiler.datasetdelta.model.DatasetDelta;
import com.dataprofiler.datasetdelta.model.DeltaEnum;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.text.diff.CommandVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildDeltas implements CommandVisitor<Character> {
  private static final Logger logger = LoggerFactory.getLogger(BuildDeltas.class);
  //    protected Character deltaSymbol = '𝛥';
  protected String deltaSymbol = "\uD835\uDEAB";
  protected static Character delimChar = Character.MAX_VALUE;
  protected int DEFAULT_BUILDER_SIZE = 256;
  protected StringBuilder stringBuilder = new StringBuilder(DEFAULT_BUILDER_SIZE);
  protected List<DatasetDelta> deltas = new ArrayList<>();
  protected DeltaEnum insertEnum;
  protected DeltaEnum removeEnum;
  protected String dataset;
  protected String table;
  protected String column;
  protected String fromVersion;
  protected String targetVersion;
  protected long datasetUpdatedOnMillis;

  public BuildDeltas(
      String dataset,
      String table,
      String column,
      long datasetUpdatedOnMillis,
      String fromVersion,
      String targetVersion,
      DeltaEnum insert,
      DeltaEnum remove) {
    super();
    this.dataset = dataset;
    this.table = table;
    this.column = column;
    this.fromVersion = fromVersion;
    this.targetVersion = targetVersion;
    this.datasetUpdatedOnMillis = datasetUpdatedOnMillis;
    this.insertEnum = insert;
    this.removeEnum = remove;
  }

  @Override
  public void visitInsertCommand(Character s) {
    if (!delimChar.equals(s)) {
      stringBuilder.append(s);
      return;
    }

    if (logger.isTraceEnabled()) {
      logger.trace(format("insert found delimiter %s", s));
    }
    String insert = stringBuilder.toString();
    stringBuilder = new StringBuilder(DEFAULT_BUILDER_SIZE);
    if (logger.isDebugEnabled()) {
      logger.debug("insert: " + insert);
    }
    DatasetDelta delta = newDelta();
    delta.setDeltaEnum(insertEnum);
    delta.setValueFrom("");
    delta.setValueTo(insert);
    if (insertEnum.equals(TABLE_ADDED)) {
      delta.setTable(insert);
    } else if (insertEnum.equals(COLUMN_ADDED)) {
      delta.setColumn(insert);
    }
    deltas.add(delta);
  }

  @Override
  public void visitKeepCommand(Character s) {
    if (logger.isTraceEnabled()) {
      logger.trace("keep: " + s);
    }
  }

  @Override
  public void visitDeleteCommand(Character s) {
    if (!delimChar.equals(s)) {
      stringBuilder.append(s);
      return;
    }

    if (logger.isTraceEnabled()) {
      logger.trace(format("delete found delimiter %s", s));
    }
    String removed = stringBuilder.toString();
    stringBuilder = new StringBuilder(DEFAULT_BUILDER_SIZE);
    if (logger.isDebugEnabled()) {
      logger.debug("delete: " + removed);
    }
    DatasetDelta delta = newDelta();
    delta.setDeltaEnum(removeEnum);
    delta.setValueFrom(removed);
    delta.setValueTo("");
    if (removeEnum.equals(TABLE_REMOVED)) {
      delta.setTable(removed);
    } else if (removeEnum.equals(COLUMN_REMOVED)) {
      delta.setColumn(removed);
    }
    deltas.add(delta);
  }

  protected DatasetDelta newDelta() {
    DatasetDelta delta = new DatasetDelta();
    delta.setDataset(dataset);
    delta.setTable(table);
    delta.setColumn(column);
    delta.setTargetVersion(targetVersion);
    delta.setFromVersion(fromVersion);
    delta.setDatasetUpdatedOnMillis(datasetUpdatedOnMillis);
    return delta;
  }

  public List<DatasetDelta> getDeltas() {
    if (isNull(deltas)) {
      return null;
    }

    return deltas.stream().map(DatasetDelta::new).collect(toList());
  }

  public void setDeltas(List<DatasetDelta> deltas) {
    if (isNull(deltas)) {
      this.deltas = null;
      return;
    }

    this.deltas = deltas.stream().map(DatasetDelta::new).collect(toList());
  }
}
