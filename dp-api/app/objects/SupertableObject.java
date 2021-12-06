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
import com.dataprofiler.util.objects.VersionedMetadataObject;
import com.dataprofiler.util.objects.ObjectScannerIterable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SupertableObject {

  @JsonInclude(Include.NON_NULL)
  private Map<String, List<SupertableObject>> tracking = new HashMap<>();

  private String name;
  private Long size;
  private String year;
  public static final String SUPERTAG_KEY = "supertag";
  public static final String SUPERTABLE_YEAR_KEY = "supertableYear";
  public static final String SUPERTABLE_DATASET_KEY = "containsSupertable";

  public SupertableObject() {
    this.tracking = new HashMap<>();
  }

  public SupertableObject(String name, Long size) {
    this.name = name;
    this.size = size;
  }

  public SupertableObject(String name, Long size, String year) {
    this.name = name;
    this.size = size;
    this.year = year;
  }

  public void add(VersionedMetadataObject metadataum) {
    String supertag = metadataum.properties.get(SUPERTAG_KEY);
    String year = metadataum.properties.get(SUPERTABLE_YEAR_KEY);
    Long numCols = metadataum.getNum_columns();
    Long numVals = metadataum.getNum_values();
    SupertableObject el = new SupertableObject(metadataum.getTable_name(), numVals / numCols, year);
    if (tracking.containsKey(supertag)) {
      tracking.get(supertag).add(el);
    } else {
      ArrayList<SupertableObject> arr = new ArrayList<>();
      arr.add(el);
      tracking.put(supertag,arr);
    }
  }

  private static SupertableObject fromIterable(ObjectScannerIterable<VersionedMetadataObject> iterable) {
    SupertableObject tracking = new SupertableObject();

    for (VersionedMetadataObject m: iterable) {
      if (m.properties.containsKey(SupertableObject.SUPERTAG_KEY)) {
        tracking.add(m);
      }
    }

    return tracking;
  }

  public static SupertableObject fromVersion(
      Context context, MetadataVersionObject version) {
      return fromIterable(new VersionedMetadataObject().scanAllTables(context, version));
  }

  public static SupertableObject fromDataset(Context context, MetadataVersionObject version, String dataset) {
    return fromIterable(new VersionedMetadataObject().scanTables(context, version, dataset));
  }

  public Map<String, List<SupertableObject>> getTracking() {
    return tracking;
  }

  public void setTracking(Map<String, List<SupertableObject>> tracking) {
    this.tracking = tracking;
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

  public String getYear() {
    return year;
  }

  public void setYear(String year) {
    this.year = year;
  }

  @java.lang.Override
  public java.lang.String toString() {
    return "SupertableObject{" +
    "tracking=" + tracking +
    ", name='" + name + '\'' +
    ", size=" + size +
    '}';
  }
}
