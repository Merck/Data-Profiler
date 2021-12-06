package com.dataprofiler.util.mapreduce;

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

import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Const.SortOrder;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.objects.ColumnCountObject;
import com.dataprofiler.util.objects.MetadataVersionObject;
import com.dataprofiler.util.objects.VersionedMetadataObject;
import org.apache.hadoop.mapreduce.JobContext;

public class ColumnCountInputFormat extends DataProfilerAccumuloInput {

  public static final String DATASET = "dataset";
  public static final String TABLE = "table";
  public static final String COLUMN = "column";
  public static final String SORT_ORDER = "sortOrder";
  public static final String BATCH = "batch";

  @Override
  public void applyJobConfiguration(JobContext jobContext, Context context)
      throws BasicAccumuloException {
    String dataset = jobContext.getConfiguration().get(DATASET);
    String table = jobContext.getConfiguration().get(TABLE);
    String column = jobContext.getConfiguration().get(COLUMN);
    String sort = jobContext.getConfiguration().get(SORT_ORDER);
    String batch = jobContext.getConfiguration().get(BATCH, "false");

    SortOrder sortOrder = sort != null ? SortOrder.getEnum(sort) : null;
    MetadataVersionObject version = context.getCurrentMetadataVersion();

    if (dataset != null && !dataset.isEmpty()) {
      if (table != null && !table.isEmpty()) {
        if (column != null && !column.isEmpty()) {
          boolean isBatch = Boolean.parseBoolean(batch);
          VersionedMetadataObject colMetadata =
              new VersionedMetadataObject().fetchColumn(context, version, dataset, table, column);
          new ColumnCountObject()
              .fetchColumn(context, colMetadata)
              .setBatch(isBatch)
              .applyInputConfiguration(jobContext.getConfiguration());

        } else {
          VersionedMetadataObject tableMetadata =
              new VersionedMetadataObject().fetchTable(context, version, dataset, table);
          new ColumnCountObject()
              .fetchTable(context, tableMetadata, sortOrder)
              .applyInputConfiguration(jobContext.getConfiguration());
        }
      } else {
        new ColumnCountObject()
            .fetchDataset(context, dataset, sortOrder)
            .applyInputConfiguration(jobContext.getConfiguration());
      }
    } else {
      new ColumnCountObject()
          .fetchEverything(context, sortOrder)
          .applyInputConfiguration(jobContext.getConfiguration());
    }
  }
}
