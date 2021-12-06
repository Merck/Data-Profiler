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
import com.dataprofiler.util.objects.VersionedAllMetadata;
import com.dataprofiler.util.objects.VersionedDatasetMetadata;
import org.apache.accumulo.core.client.BatchWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// You can run this if you ever have metadata that is part of the current version that doesn't
// have dataset or table level metadata. It _should_ be relatively safe. I would recommend
// making a copy of the metadata table before you do it though.
public class FixMissingDatasetMetadata {

  private static final Logger logger =
      LoggerFactory.getLogger(FixMissingDatasetMetadataConfig.class);

  public static void main(String[] args) throws Exception {
    FixMissingDatasetMetadataConfig config = new FixMissingDatasetMetadataConfig();

    if (!config.parse(args)) {
      logger.error("Usage: <main_class> [options]");
      config.printHelp();
      System.exit(1);
    }

    Context context = new Context(args);

    MetadataVersionObject version;

    if (config.versionId != null && !config.versionId.isEmpty()) {
      version = context.getMetadataVersion(config.versionId);
    } else {
      version = context.getCurrentMetadataVersion();
    }

    System.out.println(version);

    VersionedAllMetadata allMetadata = new VersionedAllMetadata(context, version);

    CommitMetadata.createInitialDatasetAndTableLevelMetadata(allMetadata);

    for (VersionedDatasetMetadata d : allMetadata.metadata.values()) {
      d.setVersionForAll(version);
      d.calculateTableAndDatasetStatistics();
    }

    System.out.println("Checking consistency");
    allMetadata.checkMetadataConsistency();

    BatchWriter writer = context.createBatchWriter(version.getTable(context));
    allMetadata.putAll(context, writer);
    writer.close();
  }

  public static class FixMissingDatasetMetadataConfig extends Config {
    @Parameter(names = "--version-id", description = "Manually provide the version id")
    public String versionId = null;
  }
}
