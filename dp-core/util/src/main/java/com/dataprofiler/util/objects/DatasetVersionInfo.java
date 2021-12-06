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
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatasetVersionInfo {
  public String datasetName;
  public boolean deleted = true;
  public List<VersionedMetadataObject> metadata;
  private HashMap<String, VersionedMetadataObject> metadataMap = new HashMap<>();

  public DatasetVersionInfo(String datasetName) {
    this.datasetName = datasetName;
  }

  /**
   * * Find all datasets included deleted ones and put them into a map by dataset name.
   *
   * @param context
   * @return
   */
  public static Map<String, DatasetVersionInfo> allVersionsOfAllDatasets(Context context) {
    List<MetadataVersionObject> allVersions =
        new MetadataVersionObject().allMetadataVersions(context);

    HashMap<String, DatasetVersionInfo> datasets = new HashMap<>();

    for (VersionedMetadataObject m :
        new VersionedMetadataObject().scanAllDatasetsAllVersions(context).setBatch(true)) {
      if (!datasets.containsKey(m.dataset_name)) {
        datasets.put(m.dataset_name, new DatasetVersionInfo(m.dataset_name));
      }
      datasets.get(m.dataset_name).metadataMap.put(m.version_id, m);
    }

    for (DatasetVersionInfo d : datasets.values()) {
      d.metadata = MetadataVersionObject.sortByVersion(d.metadataMap, allVersions);
      d.metadataMap = null;
      // See if this dataset has been deleted. It's possible that after the sort above (which
      // excludes anything that is not in
      // all versions) that there is no metadata left for this dataset. This would happen only if
      // the metadata is in a
      // very screwy state, but that has happened on development. Let's say it's deleted in that
      // case.
      if (d.metadata.size() > 0 && d.metadata.get(0).version_id.equals(allVersions.get(0).id)) {
        d.deleted = false;
      }
    }

    return datasets;
  }

  private String tsToDate(long ts) {
    return new Date(new Timestamp(ts).getTime()).toString();
  }

  public String toString() {
    long num_tables = 0;
    String last_updated = "unknown";
    if (metadata.size() > 0) {
      last_updated = tsToDate(metadata.get(0).update_time);
      num_tables = metadata.get(0).num_tables;
    }
    return String.format(
        "%s deleted: %b last updated: %s tables: %d",
        datasetName, deleted, last_updated, num_tables);
  }
}
