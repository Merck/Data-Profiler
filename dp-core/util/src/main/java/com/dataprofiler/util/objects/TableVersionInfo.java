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
import java.sql.Date;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class TableVersionInfo {
  public String dataset_name;
  public String table_id;
  public String table_name;
  public boolean deleted = true;
  // every single copy of the metadata for this table in version order
  public List<VersionedMetadataObject> metadata;
  // Temporary storage for the metadata for the versions - we store it like this so that we can sort
  // it before we put it in the TableVersionInfo
  private HashMap<String, VersionedMetadataObject> metadataMap = new HashMap<>();

  public TableVersionInfo(VersionedMetadataObject m) {
    this.dataset_name = m.dataset_name;
    this.table_id = m.table_id;
    this.table_name = m.table_name;
    addMetadataUnsorted(m);
  }

  private void addMetadataUnsorted(VersionedMetadataObject m) {
    metadataMap.put(m.version_id, m);
  }

  private void sortMetadata(List<MetadataVersionObject> allVersions) {
    metadata = MetadataVersionObject.sortByVersion(metadataMap, allVersions);
    metadataMap = null;
  }

  private static class TableVersionTracker {
    // This is a map of table id to TableVersionInfo
    HashMap<String, TableVersionInfo> versions = new HashMap<>();

    public void addMetadata(VersionedMetadataObject m) {
      if (!versions.containsKey(m.table_id)) {
        TableVersionInfo t = new TableVersionInfo(m);
        versions.put(m.table_id, t);
      } else {
        versions.get(m.table_id).addMetadataUnsorted(m);
      }
    }

    public List<TableVersionInfo> sort(List<MetadataVersionObject> allVersions) {
      // The way that this sorting works is by leveraging the fact that allVersions is already
      // sorted. We pretty much have to do that because the metadata ids are just uuids - there is
      // nothing in the data itself that gives any indication of sort order - so the only way to
      // determine sort order is to pull all of the metadata versions out of accumulo and follow
      // the linked list of versions. See MetadataVersionObject.allMetadataVersions for more info
      // on how that is done.

      // For each version, store it's metadata in sorted order and store it in another map by it's
      // most ecent metadata version.
      HashMap<String, TableVersionInfo> sortedTableVersions = new HashMap<>();
      for (TableVersionInfo t : versions.values()) {
        t.sortMetadata(allVersions);
        // Ugggh - on development we seem to have some rogue metadata versions that aren't showing
        // up
        // in allversions, meaning that after sort they don't show up in metadata. What we are going
        // to
        // do is just not put them in sortedTableVersions which means they will be lost to the sands
        // of
        // time. Go ahead and print a warning though.
        if (t.metadata.size() == 0) {
          System.err.println("Metadata found that was not part of all versions - ignoring");
          continue;
        }
        sortedTableVersions.put(t.metadata.get(0).version_id, t);
      }

      // Now put all of the metadata into the output array in sorted order.
      List<TableVersionInfo> output =
          MetadataVersionObject.sortByVersion(sortedTableVersions, allVersions);

      assert (output.size() == sortedTableVersions.size());

      if (output.size() == 0) {
        return output;
      }

      // Now see if the most recent table has metadata with the current version and, if it does,
      // mark that as not deleted.
      if (output.get(0).metadata.get(0).version_id.equals(allVersions.get(0).id)) {
        output.get(0).deleted = false;
      }

      return output;
    }
  }

  /***
   * Return all versions of all tables in a dataset.
   */
  public static Map<String, List<TableVersionInfo>> allVersionsOfAllTables(
      Context context, String datasetName) {

    List<MetadataVersionObject> allVersions =
        new MetadataVersionObject().allMetadataVersions(context);

    HashMap<String, TableVersionTracker> tables = new HashMap<>();

    for (VersionedMetadataObject m :
        new VersionedMetadataObject()
            .scanAllTablesAllVersions(context, datasetName)
            .setBatch(true)) {
      if (!tables.containsKey(m.table_name)) {
        tables.put(m.table_name, new TableVersionTracker());
      }
      tables.get(m.table_name).addMetadata(m);
    }

    HashMap<String, List<TableVersionInfo>> output = new HashMap<>();

    if (tables.size() == 0) {
      return output;
    }

    for (Entry<String, TableVersionTracker> e : tables.entrySet()) {
      output.put(e.getKey(), e.getValue().sort(allVersions));
    }

    return output;
  }

  /**
   * * Return all versions of a table. The list returned will be returned in order, with the most
   * recent version first.
   *
   * <p>This operation can be slow - it has to scan a lot of metadata to do its work.
   *
   * @param context
   * @param datasetName
   * @param tableName
   * @return
   */
  public static List<TableVersionInfo> allVersionsOfTable(
      Context context, String datasetName, String tableName) {
    List<MetadataVersionObject> allVersions =
        new MetadataVersionObject().allMetadataVersions(context);

    TableVersionTracker t = new TableVersionTracker();

    for (VersionedMetadataObject m :
        new VersionedMetadataObject()
            .scanTableAllVersions(context, datasetName, tableName)
            .setBatch(true)) {
      t.addMetadata(m);
    }

    return t.sort(allVersions);
  }

  private String tsToDate(long ts) {
    return new Date(new Timestamp(ts).getTime()).toString();
  }

  public String toString() {
    return String.format(
        "%s deleted: %b loaded: %s",
        this.table_id, this.deleted, tsToDate(this.metadata.get(0).load_time));
  }
}
