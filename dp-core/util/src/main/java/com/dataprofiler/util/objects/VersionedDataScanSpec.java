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

import com.dataprofiler.util.Context;
import com.dataprofiler.util.objects.VersionedDatasetMetadata.MissingMetadataException;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;

/**
 * * This is just like a DataScanSpec except that it automatically transforms dataset, table, and
 * column names as needed so that data can be fetched with the correct version. It also adds
 * explicit methods to get names / ids.
 */
public class VersionedDataScanSpec extends DataScanSpec {
  private VersionedTableMetadata metadata;
  private Map<String, String> activeTables;

  private String table_display_name;
  private String table_id;

  private String column_display_name;

  protected VersionedDataScanSpec() {
    super();
  }

  public VersionedDataScanSpec(VersionedTableMetadata metadata, DataScanSpec spec)
      throws MissingMetadataException {
    super(spec);
    fromTableAndSpecIfNotSearch(metadata, spec);
  }

  public VersionedDataScanSpec(Context context, MetadataVersionObject version, DataScanSpec spec)
      throws MissingMetadataException {
    super(spec);
    init(context, version, spec);
  }

  public VersionedDataScanSpec(Context context, DataScanSpec spec) throws MissingMetadataException {
    super(spec);
    init(context, context.getCurrentMetadataVersion(), spec);
  }

  private void fromTableAndSpecIfNotSearch(VersionedTableMetadata metadata, DataScanSpec spec)
      throws MissingMetadataException {
    this.metadata = metadata;

    try {
      System.out.println(spec.toJson());
    } catch (JsonProcessingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    if (!metadata.getTable().dataset_name.equals(spec.dataset)) {
      throw new MissingMetadataException(
          String.format(
              "Provided metadata is for the wrong dataset: %s vs %s",
              metadata.getTable().dataset_name, dataset));
    }

    table_id = metadata.getTable().table_id;
    table_display_name = metadata.getTable().table_display_name;

    if (column != null) {
      VersionedMetadataObject cm = metadata.getColumns().get(column);
      if (cm != null) {
        column_display_name = metadata.getColumns().get(column).column_display_name;
      }
    }
  }

  private void init(Context context, MetadataVersionObject version, DataScanSpec spec)
      throws MissingMetadataException {
    if (spec.getDataset() != null && spec.getTable() != null) {
      VersionedTableMetadata metadata =
          new VersionedMetadataObject()
              .allMetadataForTable(context, version, spec.getDataset(), spec.getTable());
      if (metadata.getTable() == null) {
        throw new MissingMetadataException(spec.getTable() + " does not exist.");
      }
      fromTableAndSpecIfNotSearch(metadata, spec);
    } else if (spec.getType() == Type.SEARCH) {
      activeTables = new VersionedMetadataObject().allTableNamesForVersion(context, version);
    }
  }

  /**
   * * Return the table *id* - this is for compatibility because before the table name was used in
   * the key for all data. Now that table id is what is used, transform that here.
   *
   * @return
   */
  @Override
  public String getTable() {
    return table_id;
  }

  public String getTable_name() {
    return table;
  }

  public VersionedTableMetadata getMetadata() {
    return metadata;
  }

  public String getTable_display_name() {
    return table_display_name;
  }

  public String getTable_id() {
    return table_id;
  }

  public String getColumn_display_name() {
    return column_display_name;
  }

  public Map<String, String> getActiveTables() {
    return activeTables;
  }
}
