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

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/***
 * This class holds the metadata about a table and all of it's columns for a single version. There
 * is also a version object referencing the version. The columns are stored in a hashmap keyed
 * by the column name.
 */
public class VersionedTableMetadata {
  private MetadataVersionObject version;
  private VersionedMetadataObject table;
  private HashMap<String, VersionedMetadataObject> columns = new HashMap();

  public VersionedTableMetadata(
      MetadataVersionObject version, ObjectScannerIterable<VersionedMetadataObject> iterable) {
    this.version = version;
    iterable.setBatch(true);
    for (VersionedMetadataObject m : iterable) {
      putMetadata(m);
    }
  }

  public void putMetadata(VersionedMetadataObject m) {
    assert (m.version_id.equals(getVersion().getId()));
    switch (m.metadata_level) {
      case VersionedMetadataObject.TABLE:
        assert (table == null);
        table = m;
        break;
      case VersionedMetadataObject.COLUMN:
        columns.put(m.column_name, m);
        break;
      default:
        assert (false);
    }
  }

  public List<String> sortedColumnNames() {
    return StreamSupport.stream(getColumns().values().spliterator(), false)
        .sorted(Comparator.comparingInt(a -> a.column_num))
        .map(x -> x.column_name)
        .collect(Collectors.toList());
  }

  public MetadataVersionObject getVersion() {
    return version;
  }

  public void setVersion(MetadataVersionObject version) {
    this.version = version;
  }

  public VersionedMetadataObject getTable() {
    return table;
  }

  public void setTable(VersionedMetadataObject table) {
    this.table = table;
  }

  public HashMap<String, VersionedMetadataObject> getColumns() {
    return columns;
  }

  public void setColumns(HashMap<String, VersionedMetadataObject> columns) {
    this.columns = columns;
  }
}
