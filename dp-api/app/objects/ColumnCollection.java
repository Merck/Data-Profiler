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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ColumnCollection {

  private HashMap<String, HashMap<String, List<String>>> columnsHashmap;
  private LinkedList<ColumnInfo> columnsList;

  public ColumnCollection() {
    this.columnsHashmap = new HashMap<>();
  }

  public ColumnCollection(HashMap<String, HashMap<String, List<String>>> columnsHashmap) {
    this.columnsHashmap = columnsHashmap;
  }

  public static String cleanText(String text) {
    return text.replaceAll("[^\\x00-\\x7F]", "")
        .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
        .replaceAll("\\p{C}", "")
        .toLowerCase()
        .trim();
  }

  public void add(String dataset, String table, String column) {
    computeHashmaps(dataset, table, column);
  }

  private void computeHashmaps(String dataset, String table, String column) {
    HashMap<String, List<String>> columnObj = currentColumnObj(column);
    if (columnObj.containsKey(dataset)) {
      columnObj.get(dataset).add(table);
    } else {
      List<String> list = new ArrayList<>();
      list.add(table);
      columnObj.put(dataset, list);
    }
    columnsHashmap.put(column, columnObj);
  }

  public ColumnCollection prepareReturn(
      String dataset, Boolean asc, Boolean name, String searchTerm) {
    LinkedList<ColumnCollection.ColumnInfo> ret = new LinkedList<>();
    for (String columnName : this.columnsHashmap.keySet()) {
      HashMap<String, List<String>> locations = this.columnsHashmap.get(columnName);
      ColumnCollection.ColumnInfo potential =
          new ColumnCollection.ColumnInfo(columnName, locations, dataset);
      Boolean skipSearch =
          searchTerm != null
              && !ColumnCollection.cleanText(columnName)
                  .contains(ColumnCollection.cleanText(searchTerm));
      if (skipSearch == true) {
        continue;
      }
      if (potential.getCount() > 0) {
        ret.add(potential);
      }
    }
    this.columnsList = ret;
    this.sortColumnList(asc, name);
    return this;
  }

  private void sortColumnList(Boolean asc, Boolean name) {
    Collections.sort(
        this.columnsList,
        new Comparator<ColumnCollection.ColumnInfo>() {
          public int compare(ColumnCollection.ColumnInfo o1, ColumnCollection.ColumnInfo o2) {
            if (name) {
              return asc
                  ? (ColumnCollection.cleanText(o1.getName()))
                      .compareTo(ColumnCollection.cleanText(o2.getName()))
                  : (ColumnCollection.cleanText(o2.getName()))
                      .compareTo(ColumnCollection.cleanText(o1.getName()));
            }
            return asc
                ? (o1.getCount()).compareTo(o2.getCount())
                : (o2.getCount()).compareTo(o1.getCount());
          }
        });
  }

  private HashMap<String, List<String>> currentColumnObj(String column) {
    if (columnsHashmap.containsKey(column) == false) {
      return new HashMap<>();
    }
    return columnsHashmap.get(column);
  }

  public List<ColumnCollection.ColumnInfo> top(Integer limit) {
    return this.columnsList.stream().limit(limit).collect(Collectors.toList());
  }

  public HashMap<String, HashMap<String, List<String>>> getColumnsHashmap() {
    return columnsHashmap;
  }

  public void setColumnsHashmap(HashMap<String, HashMap<String, List<String>>> columnsHashmap) {
    this.columnsHashmap = columnsHashmap;
  }

  @Override
  public String toString() {
    ObjectMapper mapper = new ObjectMapper();

    String output = null;
    try {
      output = mapper.writeValueAsString(this);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return output;
  }

  public class ColumnInfo {
    private String name;
    private List<String> locations;
    private Integer count;

    public ColumnInfo(String name, HashMap<String, List<String>> locations, String dataset) {
      this.name = name;
      if (dataset == null) {
        this.locations = locations.keySet().stream().collect(Collectors.toList());
      } else {
        this.locations = locations.get(dataset);
      }
      calculateCount();
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Integer getCount() {
      return count;
    }

    public void setCount(Integer count) {
      this.count = count;
    }

    public List<String> getLocations() {
      return locations;
    }

    private void calculateCount() {
      if (this.locations == null) {
        this.count = 0;
      } else {
        this.count = this.locations.size();
      }
    }

    @java.lang.Override
    public java.lang.String toString() {
      return "ColumnInfo{"
          + "name='"
          + name
          + '\''
          + ", locations="
          + locations
          + ", count="
          + count
          + '}';
    }
  }
}
