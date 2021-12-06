/**
*  Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
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
**/
package objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.objects.MetadataVersionObject;
import com.dataprofiler.util.objects.VersionedAllMetadata;
import com.dataprofiler.util.objects.VersionedDatasetMetadata;
import com.dataprofiler.util.objects.VersionedMetadataObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TreemapObject {

  private static final String THIS_WEEK = "week";
  private static final String THIS_MONTH = "month";
  private static final String THIS_YEAR = "year";
  private static final String YEAR_PLUS = "year_plus";

  @JsonInclude(Include.NON_NULL)
  private List<TreemapObject> children;

  private String name;
  private Long size;
  private String updated;

  public TreemapObject(String name, Long size, Long updated) {
    this.name = name;
    this.size = size;
    this.updated = determineTime(updated);
  }

  public TreemapObject(String name, Long size, Long updated, List<TreemapObject> children) {
    this.name = name;
    this.size = size;
    this.updated = determineTime(updated);
    this.children = children;
  }

  private static String determineTime(Long time) {
    Long currentTime = System.currentTimeMillis();
    Long diff = currentTime - time;
    Long week = 604800000L;
    Long month = week * 4;
    Long year = week * 52;
    if (diff > 0 && diff <= week) {
      return THIS_WEEK;
    }
    if (diff > week && diff <= month) {
      return THIS_MONTH;
    }
    if (diff > month && diff <= year) {
      return THIS_YEAR;
    }
    return YEAR_PLUS;
  }

  public void addChild(TreemapObject child) {
    if (this.children == null) {
      List<TreemapObject> ret = new ArrayList<>();
      ret.add(child);
      this.children = ret;
    } else {
      this.children.add(child);
    }
  }

  public static List<TreemapObject> fromAllMetdata(Context context, MetadataVersionObject version) {
    HashMap<String, TreemapObject> tracking = new HashMap<>();

    for (VersionedMetadataObject m: new VersionedMetadataObject().scanDatasetsAndTables(context, version)) {
      TreemapObject lookup = tracking.get(m.dataset_name);
      if (m.metadata_level == VersionedMetadataObject.DATASET) {
        if (lookup == null) {
          TreemapObject datasetTree =
              new TreemapObject(m.dataset_name, m.num_values, m.update_time);
          tracking.put(m.dataset_name, datasetTree);
        } else {
          lookup.setSize(m.num_values);
          lookup.setUpdatedByLong(m.update_time);
          tracking.put(m.dataset_name, lookup);
        }
      } else if (m.metadata_level == VersionedMetadataObject.TABLE) {
        lookup = tracking.get(m.dataset_name);
        TreemapObject tableTree = new TreemapObject(m.table_name, m.num_values, m.update_time);
        if (lookup == null) {
          TreemapObject datasetTree = new TreemapObject(m.dataset_name, 0L, 0L);
          datasetTree.addChild(tableTree);
          tracking.put(m.dataset_name, datasetTree);
        } else {
          lookup.addChild(tableTree);
          tracking.put(m.dataset_name, lookup);
        }
      }
    }

    return new ArrayList<>(tracking.values());
  }

  public List<TreemapObject> getChildren() {
    return children;
  }

  public void setChildren(List<TreemapObject> children) {
    this.children = children;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Long getSize() {
    return size;
  }

  public void setSize(Long size) {
    this.size = size;
  }

  public String getUpdated() {
    return updated;
  }

  public void setUpdated(String updated) {
    this.updated = updated;
  }

  public void setUpdatedByLong(Long updated) {
    this.updated = determineTime(updated);
  }

  @java.lang.Override
  public java.lang.String toString() {
    return "TreemapObject{"
        + "children="
        + children
        + ", name='"
        + name
        + '\''
        + ", size="
        + size
        + ", updated='"
        + updated
        + '\''
        + '}';
  }
}
