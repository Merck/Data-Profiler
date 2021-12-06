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

import com.beust.jcommander.Parameter;
import com.dataprofiler.util.Config;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.objects.MetadataVersionObject;
import com.dataprofiler.util.objects.VersionedMetadataObject;
import java.util.ArrayList;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.data.Mutation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FixMissingPreviousLoad {
  private static final Logger logger = LoggerFactory.getLogger(FixMissingPreviousLoad.class);

  public static void main(String[] args) throws Exception {
    FixMissingPreviousLoadConfig config = new FixMissingPreviousLoadConfig();

    if (!config.parse(args)) {
      logger.error(
          "Usage: <main_class> [options] --dataset <dataset_name> --table <table_name> --prev-version-id <id> --version-id <id>");
      config.printHelp();
      System.exit(1);
    }

    Context context = new Context(args);

    // Get missing columns from previous version
    MetadataVersionObject prevVersion = new MetadataVersionObject(config.prevVersionId);

    ArrayList<VersionedMetadataObject> columnMetadata = new ArrayList<>();
    for (VersionedMetadataObject m : new VersionedMetadataObject().scan(context, prevVersion)) {
      if (m.metadata_level == VersionedMetadataObject.COLUMN
          && m.dataset_name.equals(config.datasetName)
          && m.table_name.equals(config.tableName)) {
        columnMetadata.add(m);
      }
    }

    System.out.println(prevVersion);

    BatchWriter writer = context.createBatchWriter(new VersionedMetadataObject().getTable(context));

    for (VersionedMetadataObject m : columnMetadata) {
      m.version_id = config.versionId;
      System.out.println(m);
      Mutation mutation = m.createMutation();
      writer.addMutation(mutation);
    }

    writer.close();

    // MetadataVersionObject prevVersion;

    // if(config.versionId != null && !config.versionId.isEmpty()) {
    //     version = context.getMetadataVersion(config.versionId);
    // } else {
    //    version = context.getCurrentMetadataVersion();
    // }

    // System.out.println(version);

    // VersionedAllMetadata allMetadata = new VersionedAllMetadata(context, version);

    // CommitMetadata.createInitialDatasetAndTableLevelMetadata(allMetadata);

    // for (VersionedDatasetMetadata d: allMetadata.metadata.values()) {
    //     d.setVersionForAll(version);
    //     d.calculateTableAndDatasetStatistics();
    // }

    // System.out.println("Checking consistency");
    // allMetadata.checkMetadataConsistency();

    // BatchWriter writer = context.createBatchWriter(version.getTable(context));
    // allMetadata.putAll(context, writer);
    // writer.close();
  }

  public static class FixMissingPreviousLoadConfig extends Config {

    @Parameter(names = "--dataset", description = "Name of the dataset", required = true)
    public String datasetName = null;

    @Parameter(names = "--table", description = "Name of the table", required = true)
    public String tableName = null;

    @Parameter(
        names = "--version-id",
        description = "ID of the version that contains data",
        required = true)
    public String versionId = null;

    @Parameter(
        names = "--prev-version-id",
        description = "ID of the version that is missing data",
        required = true)
    public String prevVersionId = null;
  }
}
