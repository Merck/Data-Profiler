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

import static com.dataprofiler.datasetdelta.model.DeltaEnum.COLUMN_ADDED;
import static com.dataprofiler.datasetdelta.model.DeltaEnum.COLUMN_REMOVED;
import static com.dataprofiler.datasetdelta.model.DeltaEnum.DATASET_VALUES_DECREASED;
import static com.dataprofiler.datasetdelta.model.DeltaEnum.DATASET_VALUES_INCREASED;
import static com.dataprofiler.datasetdelta.model.DeltaEnum.TABLE_ADDED;
import static com.dataprofiler.datasetdelta.model.DeltaEnum.TABLE_REMOVED;
import static java.util.stream.Collectors.toSet;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public class DatasetDeltaStub {

  static long time = 1613757735916L;
  static String dataset = "ds1";
  static String table = "t1";
  static String column = "c1";

  public static DatasetDelta buildDatasetDelta() {
    return buildDatasetValuesIncreasedDelta();
  }

  public static DatasetDelta buildDatasetValuesIncreasedDelta() {
    DatasetDelta delta = partialDelta();
    delta.setDeltaEnum(DATASET_VALUES_INCREASED);
    delta.setValueFrom("11060");
    delta.setValueTo("431428915");
    return delta;
  }

  public static DatasetDelta buildDatasetValuesDecreasedDelta() {
    DatasetDelta delta = partialDelta();
    delta.setDeltaEnum(DATASET_VALUES_DECREASED);
    delta.setValueFrom("99000");
    delta.setValueTo("89000");
    return delta;
  }

  public static DatasetDelta buildColumnAddedDelta() {
    DatasetDelta delta = partialDelta();
    delta.setDeltaEnum(COLUMN_ADDED);
    delta.setValueFrom("");
    delta.setValueTo(column);
    return delta;
  }

  public static DatasetDelta buildColumnRemovedDelta() {
    DatasetDelta delta = partialDelta();
    delta.setDeltaEnum(COLUMN_REMOVED);
    delta.setValueFrom(column);
    delta.setValueTo("");
    return delta;
  }

  public static DatasetDelta buildTableAddedDelta() {
    DatasetDelta delta = partialDelta();
    delta.setTable("");
    delta.setColumn("");
    delta.setDeltaEnum(TABLE_ADDED);
    delta.setValueFrom("");
    delta.setValueTo(table);
    return delta;
  }

  public static DatasetDelta buildTableRemovedDelta() {
    DatasetDelta delta = partialDelta();
    delta.setTable("");
    delta.setColumn("");
    delta.setDeltaEnum(TABLE_REMOVED);
    delta.setValueFrom(table);
    delta.setValueTo("");
    return delta;
  }

  public static Set<DatasetDelta> buildDatasetDeltas() {
    return Stream.of(
            buildDatasetValuesIncreasedDelta(),
            buildDatasetValuesDecreasedDelta(),
            buildColumnRemovedDelta(),
            buildColumnAddedDelta(),
            buildTableAddedDelta(),
            buildTableRemovedDelta())
        .collect(toSet());
  }

  static DatasetDelta partialDelta() {
    DatasetDelta delta = new DatasetDelta();
    delta.setDatasetUpdatedOnMillis(time);
    delta.setUpdatedOnMillis(time);
    delta.setDataset(dataset);
    delta.setTable(table);
    delta.setColumn(column);
    delta.setFromVersion("0066510e-07fb-44e4-91e4-a5955720d7ca");
    delta.setTargetVersion("00580da7-2c73-4c2f-b733-0daf56e6febe");
    delta.setComment(new CommentUUID(UUID.fromString("8358ba35-5997-47b3-889f-0d038a3f6393")));
    return delta;
  }
}
