package com.dataprofiler.metadata;

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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Config;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.objects.DatasetVersionInfo;
import com.dataprofiler.util.objects.TableVersionInfo;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ManageMetadata {

  public void showTableVersions(Context context, String datasetName, String tableName) {
    List<TableVersionInfo> tableVersions =
        TableVersionInfo.allVersionsOfTable(context, datasetName, tableName);

    for (TableVersionInfo t : tableVersions) {
      System.out.println(t.toString());
    }
  }

  public void showDataset(Context context, String datasetName) {
    Map<String, List<TableVersionInfo>> tables =
        TableVersionInfo.allVersionsOfAllTables(context, datasetName);

    for (Entry<String, List<TableVersionInfo>> e : tables.entrySet()) {
      System.out.println(e.getKey());
      for (TableVersionInfo t : e.getValue()) {
        System.out.println("\t" + t.toString());
      }
    }
  }

  public void showAllDatasets(Context context) {
    Map<String, DatasetVersionInfo> datasets = DatasetVersionInfo.allVersionsOfAllDatasets(context);
    LinkedHashMap<String, DatasetVersionInfo> sortedMap = new LinkedHashMap<>();
    datasets.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));

    for (DatasetVersionInfo d : sortedMap.values()) {
      System.out.println(d.toString());
    }
  }

  @Parameters(commandDescription = "Show versions of a table")
  private class ShowTableVersions {
    @Parameter(names = "--dataset", description = "Dataset name", required = true)
    String datasetName;

    @Parameter(names = "--table", description = "Table name", required = true)
    String tableName;
  }

  @Parameters(commandDescription = "Show all tables in a dataset (and their versions)")
  private class ShowDataset {
    @Parameter(names = "--dataset", description = "Dataset name", required = true)
    String datasetName;
  }

  @Parameters(commandDescription = "Show all datasets (including deleted ones")
  private class ShowAllDatasets {}

  private class ManageMetatadataConfig extends Config {}

  public void execute(String[] args) throws BasicAccumuloException, IOException {
    ManageMetatadataConfig c = new ManageMetatadataConfig();
    ShowTableVersions showTableVersions = new ShowTableVersions();
    ShowDataset showDataset = new ShowDataset();
    ShowAllDatasets showAllDatasets = new ShowAllDatasets();
    JCommander jc =
        JCommander.newBuilder()
            .addObject(c)
            .addCommand("show-table", showTableVersions)
            .addCommand("show-dataset", showDataset)
            .addCommand("show-all-datasets", showAllDatasets)
            .build();

    c.parse(args);
    jc.parse(args);

    if (jc.getParsedCommand() == null) {
      jc.usage();
      System.exit(1);
    }

    Context context = new Context(c);

    switch (jc.getParsedCommand()) {
      case "show-table":
        showTableVersions(context, showTableVersions.datasetName, showTableVersions.tableName);
        break;
      case "show-dataset":
        showDataset(context, showDataset.datasetName);
        break;
      case "show-all-datasets":
        showAllDatasets(context);
        break;
      default:
        jc.usage();
        break;
    }
  }

  public static void main(String[] args) throws BasicAccumuloException, IOException {
    ManageMetadata m = new ManageMetadata();
    m.execute(args);
  }
}
