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
import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Config;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.objects.MetadataVersionObject;
import com.dataprofiler.util.objects.VersionedAllMetadata;
import com.dataprofiler.util.objects.VersionedDatasetMetadata;
import com.dataprofiler.util.objects.VersionedMetadataObject;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommitMetadata {
  private static final Logger logger = LoggerFactory.getLogger(CommitMetadata.class);

  /**
   * Create the table and dataset level metadata from the already existing column metadata (which is
   * created on data ingest). This both creates and commits the metadata.
   *
   * <p>This process takes a nextMetadataVersionId, which is the UUID under which any metadata for
   * new data has been loaded. That metadata is then combined with any previous metadata to create a
   * full set of metadata under the nextMetadataVersionId.
   *
   * <p>If fullDatasetLoad is set to true, then it is assumed that the next metadata contains all of
   * the tables for any dataset that is present. So, for example, if dataset Foo is loaded with
   * tables A and B and then a new load is done with tables B and C if a) fullDatasetLoad is set to
   * false the metadata will contain tables A, B, and C or b) fullDatasetLoad is set to true the
   * metadata will only contain tables B and C. This logic only applies to datasets for which tables
   * have been loaded - for all other datasets all of the tables will be copied over.
   *
   * @param context
   * @param nextMetadataVersion Metadata version to use to create the metadata
   * @param fullDatasetLoad Assume that this is all of the tables in a dataset
   * @throws Exception
   */
  public static void commitMetadata(
      Context context,
      MetadataVersionObject nextMetadataVersion,
      Deletions deletions,
      boolean fullDatasetLoad)
      throws Exception {
    MetadataVersionObject currentVersion = context.getCurrentMetadataVersion();
    commitMetadata(context, nextMetadataVersion, currentVersion, deletions, fullDatasetLoad);
  }

  public static void commitMetadata(
      Context context,
      MetadataVersionObject nextMetadataVersion,
      MetadataVersionObject currentMetadataVersion,
      Deletions deletions,
      boolean fullDatasetLoad)
      throws Exception {

    if (deletions == null) {
      deletions = new Deletions();
    }

    // The basic idea here is that we are going to fetch the previous version of the metadata
    // along with the new metadata and merge them.
    VersionedAllMetadata newData = new VersionedAllMetadata(context, nextMetadataVersion);
    if (!newData.isEmpty()) {
      // newData may be empty when we are just deleting data
      createInitialDatasetAndTableLevelMetadata(newData);
    }

    VersionedAllMetadata existingData = filterDeletedMetadata(context, deletions);

    existingData.checkMetadataConsistency();

    // Loop through all of the new data and copy it into the existing metadata (which
    // we will then update later. There are 3 options for what happens:
    //    1. New dataset - just add it to the metadata and we are done.
    //    2. Full data load - completely replace the current dataset with the new one
    //       so that only the new tables are retained.
    //    3. Update - simply replace tables with the new ones but leave the old ones
    //       that are part of that dataset.
    for (Entry<String, VersionedDatasetMetadata> entry : newData.metadata.entrySet()) {
      VersionedDatasetMetadata nextDataset = entry.getValue();

      // New dataset
      if (!existingData.metadata.containsKey(entry.getKey())) {
        // This is a totally new dataset - just copy it over
        nextDataset.calculateTableAndDatasetStatistics();
        nextDataset.setVersionForAll(nextMetadataVersion);
        existingData.metadata.put(entry.getKey(), nextDataset);
        continue;
      }

      VersionedDatasetMetadata currentDataset = existingData.metadata.get(entry.getKey());

      // For a full or partial data set load, we want to preserve properties from existing
      // tables. This will do that.
      nextDataset.setMergedProperties(currentDataset);

      // Full data load
      if (fullDatasetLoad) {
        existingData.metadata.put(entry.getKey(), nextDataset);
      } else {
        // Update
        currentDataset.replaceTablesAndColumns(nextDataset);
      }

      VersionedDatasetMetadata datasetFinal = existingData.metadata.get(entry.getKey());
      datasetFinal.calculateTableAndDatasetStatistics();
      datasetFinal.setVersionForAll(nextMetadataVersion);
    }

    // Set the version for any existing data that we have not seen already
    for (Entry<String, VersionedDatasetMetadata> entry : existingData.metadata.entrySet()) {
      if (newData.metadata.containsKey(entry.getKey())) {
        continue;
      }
      entry.getValue().setVersionForAll(nextMetadataVersion);
    }

    existingData.checkMetadataConsistency();

    BatchWriter batchWriter = context.createBatchWriter(nextMetadataVersion.getTable(context));
    existingData.putAll(context, batchWriter);

    try {
      batchWriter.close();
    } catch (MutationsRejectedException e) {
      throw new BasicAccumuloException(e.toString());
    }

    nextMetadataVersion.setPrevious(currentMetadataVersion);
    nextMetadataVersion.updateCreationTime();
    nextMetadataVersion.put(context);

    nextMetadataVersion.putAsCurrentVersion(context);
  }

  public static void createInitialDatasetAndTableLevelMetadata(VersionedAllMetadata next) {
    for (VersionedDatasetMetadata dataset : next.metadata.values()) {
      // Create initial table metadata (and save one example for creating dataset metadata)
      VersionedMetadataObject table = null;
      for (VersionedMetadataObject m : dataset.getColumnMetadata().values()) {
        if (dataset.getTableMetadata().containsKey(m.table_name)) {
          table = dataset.getTableMetadata().get(m.table_name);
          continue;
        }

        table = new VersionedMetadataObject();
        table.metadata_level = VersionedMetadataObject.TABLE;
        table.dataset_name = m.dataset_name;
        table.dataset_display_name = m.dataset_name;
        table.table_name = m.table_name;
        table.table_id = m.table_id;
        table.table_display_name = m.table_name;
        table.version_id = m.version_id;
        table.setVisibility(m.getVisibility());
        dataset.getTableMetadata().put(table.table_name, table);
      }

      assert (table != null);

      if (dataset.getDatasetMetadata() != null) {
        continue;
      }

      dataset.setDatasetMetadata(new VersionedMetadataObject());
      dataset.getDatasetMetadata().version_id = table.version_id;
      dataset.getDatasetMetadata().dataset_name = table.dataset_name;
      dataset.getDatasetMetadata().dataset_display_name = table.dataset_name;
      dataset.getDatasetMetadata().metadata_level = VersionedMetadataObject.DATASET;
      dataset.getDatasetMetadata().num_columns = 0L;
      dataset.getDatasetMetadata().setVisibility(table.getVisibility());
    }
  }

  public static VersionedAllMetadata filterDeletedMetadata(Context context, Deletions deletions) {
    MetadataVersionObject version = context.getCurrentMetadataVersion();
    VersionedAllMetadata liveMetadata = new VersionedAllMetadata(version);
    // This is just for the very first data load
    if (version == null) {
      return liveMetadata;
    }
    for (VersionedMetadataObject m : new VersionedMetadataObject().scan(context, version)) {
      if (m.metadata_level == VersionedMetadataObject.DATASET) {
        if (!deletions.datasetDeletions.contains(m.dataset_name)) {
          liveMetadata.put(m);
        }
      } else {
        if (!(deletions.datasetDeletions.contains(m.dataset_name)
            || deletions.tableDeletions.contains(m.dataset_name + m.table_name))) {
          liveMetadata.put(m);
        }
      }
    }

    return liveMetadata;
  }

  public static void main(String[] argv) throws Exception {
    CommitMetadataConfig config = new CommitMetadataConfig();
    if (!config.parse(argv)) {
      logger.error("Usage: <main_class> [options] --next-version-id <next_version_id>");
      config.printHelp();
      System.exit(1);
    }

    Context context = new Context(config);

    logger.info("Passed in version is: " + config.nextVersionId);

    long start = System.currentTimeMillis();
    try {
      MetadataVersionObject nextVersion = new MetadataVersionObject(config.nextVersionId);
      if (config.currentVersionId != null) {
        MetadataVersionObject currentVersion = new MetadataVersionObject(config.currentVersionId);
        commitMetadata(context, nextVersion, currentVersion, null, config.fullDatasetLoad);
      } else {
        commitMetadata(context, nextVersion, null, config.fullDatasetLoad);
      }
    } finally {
      long stop = System.currentTimeMillis();
      logger.info("Commit time: {}ms", (stop - start));
    }
  }

  // This stores what we want to delete - we only communicate that by dataset and table.
  // What's actually being deleted is filtered below.
  public static class Deletions {
    public Set<String> datasetDeletions = new HashSet<>();
    // These are stored dataset_name + table_name
    public Set<String> tableDeletions = new HashSet<>();

    public void addTable(String dataset, String table) {
      tableDeletions.add(dataset + table);
    }
  }

  public static class CommitMetadataConfig extends Config {
    @Parameter(
        names = "--full-dataset-load",
        description = "Treat new tables as all of the tables in each dataset",
        arity = 1)
    public boolean fullDatasetLoad = false;

    @Parameter(
        names = "--current-version-id",
        description = "Manually provide the previous version id")
    public String currentVersionId = null;

    @Parameter(
        names = "--next-version-id",
        description = "Provide the next version id to commit",
        required = true)
    public String nextVersionId = null;
  }
}
