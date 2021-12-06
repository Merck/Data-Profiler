package com.dataprofiler.delete;

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

import com.dataprofiler.metadata.CommitMetadata;
import com.dataprofiler.metadata.CommitMetadata.Deletions;
import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.objects.ColumnCountIndexObject;
import com.dataprofiler.util.objects.ColumnCountObject;
import com.dataprofiler.util.objects.ColumnCountPaginationObject;
import com.dataprofiler.util.objects.ColumnSampleObject;
import com.dataprofiler.util.objects.DatawaveRowObject;
import com.dataprofiler.util.objects.DatawaveRowShardIndexObject;
import com.dataprofiler.util.objects.ElementAlias;
import com.dataprofiler.util.objects.MetadataVersionObject;
import com.dataprofiler.util.objects.TableVersionInfo;
import com.dataprofiler.util.objects.VersionedMetadataObject;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

public class DeleteData implements Serializable {

  private static final Logger logger = Logger.getLogger(DeleteData.class.getName());

  public static void purgeDatasetAllButCurrentVersions(
      Context context, String dataset, boolean deleteIndex, boolean deleteAliases)
      throws BasicAccumuloException {
    Map<String, List<TableVersionInfo>> allTables =
        TableVersionInfo.allVersionsOfAllTables(context, dataset);

    for (List<TableVersionInfo> l : allTables.values()) {
      for (TableVersionInfo t : l) {
        if (!t.deleted) {
          continue;
        }
        purgeTable(context, t, true, deleteIndex, deleteAliases);
      }
    }
  }

  public static void purgeDatasetAllVersions(
      Context context, String dataset, boolean deleteIndex, boolean deleteAliases)
      throws BasicAccumuloException {
    // We are actually going to purge table by table - it's basically the same thing
    // for almost everything but metadata (but we pass the flag to purge the dataset metadata)
    Map<String, List<TableVersionInfo>> allTables =
        TableVersionInfo.allVersionsOfAllTables(context, dataset);

    int numVersions = allTables.values().stream().map(c -> c.size()).reduce(0, Integer::sum);
    logger.warn(String.format("Found %d versions of tables to be purged", numVersions));

    int i = 1;
    for (List<TableVersionInfo> l : allTables.values()) {
      for (TableVersionInfo t : l) {
        logger.warn(String.format("Purging version %d/%d", i++, numVersions));
        purgeTable(context, t, true, deleteIndex, deleteAliases);
      }
    }
  }

  /**
   * * Delete all of the data from a dataset in a specific version. This means that only the tables
   * that are present in that version are deleted by this action.
   *
   * @param context
   * @param version
   * @param dataset
   * @param deleteIndex
   * @param deleteAliases
   * @return
   * @throws BasicAccumuloException
   */
  public static boolean purgeDataset(
      Context context,
      MetadataVersionObject version,
      String dataset,
      boolean deleteIndex,
      boolean deleteAliases)
      throws BasicAccumuloException {

    logger.warn(String.format("Deleting all data from dataset '%s'", dataset));
    // Delete from the Accumulo column counts table
    displayDeleteDataset(dataset, context.getConfig().accumuloColumnCountsTable);
    new ColumnCountObject().bulkPurgeDataset(context, version, dataset);
    new ColumnCountPaginationObject().bulkPurgeDataset(context, version, dataset);

    // Delete from the Accumulo datawave rows table
    displayDeleteDataset(dataset, context.getConfig().accumuloDatawaveRowsTable);
    new DatawaveRowObject().bulkPurgeDataset(context, version, dataset);
    new DatawaveRowShardIndexObject().bulkPurgeDataset(context, version, dataset);

    // Delete from the index table
    if (deleteIndex) {
      displayDeleteDataset(dataset, context.getConfig().accumuloIndexTable);
      new ColumnCountIndexObject().bulkPurgeDataset(context, version, dataset);
    } else {
      logger.warn("Skipping deletion of index");
    }

    // Delete from the Alias table
    if (deleteAliases) {
      displayDeleteDataset(dataset, context.getConfig().accumuloElementAliasesTable);
      new ElementAlias().bulkPurgeDataset(context, dataset);
    } else {
      logger.warn("Skipping deletion of aliases");
    }

    // Delete from the samples table
    displayDeleteDataset(dataset, context.getConfig().accumuloSamplesTable);
    new ColumnSampleObject().bulkPurgeDataset(context, version, dataset);

    // We bulkDelete the metadata last so that if there is an error we can still get
    // the info we need to bulkDelete the columns.
    displayDeleteDataset(dataset, context.getConfig().accumuloMetadataTable);
    new VersionedMetadataObject().bulkPurgeDataset(context, version, dataset);

    logger.warn(String.format("Successfully deleted all data from dataset '%s'", dataset));

    return true;
  }

  public static void purgeTableVersions(
      Context context,
      String dataset,
      String tableName,
      String tableId,
      PurgeVersions purgeVersions,
      boolean deleteIndex,
      boolean deleteAliases)
      throws BasicAccumuloException {

    List<TableVersionInfo> versions =
        TableVersionInfo.allVersionsOfTable(context, dataset, tableName);

    boolean found = false;
    boolean exact = false;
    for (TableVersionInfo t : versions) {
      exact = false;
      // Null just means that a tableId wasn't passed in, so we are really just matching the first
      // tableId that we find.
      if (tableId == null || t.table_id.equals(tableId)) {
        exact = true;
        found = true;
        if (tableId == null) { // just so the next is not an exact match
          tableId = t.table_id;
        }
      }
      if (found) {
        if (purgeVersions == PurgeVersions.All
            || (exact == true && purgeVersions == PurgeVersions.Exact)
            || (exact != true)) {
          purgeTable(context, t, false, deleteIndex, deleteAliases);
        }
        if (purgeVersions == PurgeVersions.Exact) {
          break;
        }
      }
    }
    if (!found) {
      logger.error(
          String.format(
              "Failed to find table id %s for dataset %s and table %s.",
              tableId, dataset, tableName));
    }
  }

  public static void purgeTable(
      Context context,
      TableVersionInfo table,
      boolean purgeDatasetMetadata,
      boolean deleteIndex,
      boolean deleteAliases)
      throws BasicAccumuloException {

    logger.warn(
        String.format(
            "Deleting all data from table '%s' from dataset '%s'",
            table.table_name, table.dataset_name));

    // Delete from the Accumulo column counts table
    displayDeleteTable(
        table.dataset_name, table.table_name, context.getConfig().accumuloColumnCountsTable);
    new ColumnCountObject()
        .bulkPurgeTable(context, table.dataset_name, table.table_name, table.table_id);
    new ColumnCountPaginationObject()
        .bulkPurgeTable(context, table.dataset_name, table.table_name, table.table_id);

    // Delete from the Accumulo datawave rows table
    displayDeleteTable(
        table.dataset_name, table.table_name, context.getConfig().accumuloDatawaveRowsTable);
    new DatawaveRowObject()
        .bulkPurgeTable(context, table.dataset_name, table.table_name, table.table_id);
    new DatawaveRowShardIndexObject()
        .bulkPurgeTable(context, table.dataset_name, table.table_name, table.table_id);

    // Delete from the index table
    if (deleteIndex) {
      displayDeleteTable(
          table.dataset_name, table.table_name, context.getConfig().accumuloIndexTable);
      new ColumnCountIndexObject()
          .bulkPurgeTable(context, table.dataset_name, table.table_name, table.table_id);
    } else {
      logger.warn("Skipping deletion of index");
    }

    // Delete from the Alias table
    if (deleteAliases) {
      displayDeleteTable(
          table.dataset_name, table.table_name, context.getConfig().accumuloElementAliasesTable);
      new ElementAlias().bulkPurgeTable(context, table.dataset_name, table.table_name);
    } else {
      logger.warn("Skipping deletion of aliases");
    }

    // Delete from the samples table
    displayDeleteTable(
        table.dataset_name, table.table_name, context.getConfig().accumuloSamplesTable);
    new ColumnSampleObject()
        .bulkPurgeTable(context, table.dataset_name, table.table_name, table.table_id);

    // We bulkDelete the metadata last so that if there is an error we can still get
    // the info we need to bulkDelete the columns.
    displayDeleteTable(
        table.dataset_name, table.table_name, context.getConfig().accumuloMetadataTable);

    for (VersionedMetadataObject o : table.metadata) {
      if (purgeDatasetMetadata) {
        o.bulkPurgeDataset(context, new MetadataVersionObject(o.version_id), o.dataset_name);
      } else {
        o.bulkPurgeTable(context, o);
      }
    }

    logger.warn(
        String.format(
            "Successfully deleted all data from table '%s' from dataset '%s'",
            table.table_name, table.dataset_name));
  }

  private static void displayDeleteTable(String dataset, String table, String accumuloTable) {
    logger.warn(
        String.format(
            "Deleting dataset '%s' and table '%s' from Accumulo table '%s'",
            dataset, table, accumuloTable));
  }

  private static void displayDeleteDataset(String dataset, String accumuloTable) {
    logger.warn(
        String.format("Deleting dataset '%s' from Accumulo table '%s'", dataset, accumuloTable));
  }

  public static void softDeleteDatasetsOrTables(Context context, Deletions deletions)
      throws Exception {
    MetadataVersionObject version = MetadataVersionObject.newRandomMetadataVersion();
    CommitMetadata.commitMetadata(context, version, deletions, false);
  }

  public static void purge(String[] args, DeleteDataOptions config)
      throws BasicAccumuloException, IOException {
    PurgeVersions purgeVersions = PurgeVersions.Exact;
    if (config.deleteAllTableVersions) {
      purgeVersions = PurgeVersions.All;
      logger.warn("Configure set to purge all versions");
    } else if (config.deletePreviousTableVersions) {
      purgeVersions = PurgeVersions.Previous;
      logger.warn("Configure set to purge the previous version");
    }

    String dataset = config.parameters.get(0);
    String table = config.parameters.size() == 2 ? config.parameters.get(1) : null;
    Context context = new Context(args);

    MetadataVersionObject version = context.getCurrentMetadataVersion();

    if (table != null) {
      logger.warn(
          String.format("Purging all versions of the table '%s' for dataset '%s'", table, dataset));
      purgeTableVersions(
          context,
          dataset,
          table,
          config.tableId,
          purgeVersions,
          config.deleteIndex,
          config.deleteAliases);
    } else {
      if (purgeVersions == PurgeVersions.All) {
        logger.warn(String.format("Purging all versions of the dataset '%s'", dataset));
        purgeDatasetAllVersions(context, dataset, config.deleteIndex, config.deleteAliases);
      } else if (purgeVersions == PurgeVersions.Previous) {
        logger.warn(String.format("Purging the previous version of the dataset '%s'", dataset));
        purgeDatasetAllButCurrentVersions(
            context, dataset, config.deleteIndex, config.deleteAliases);
      } else {
        logger.warn(String.format("Purging the current version of the dataset '%s'", dataset));
        purgeDataset(context, version, dataset, config.deleteIndex, config.deleteAliases);
      }
    }
  }

  public static void delete(String[] args, DeleteDataOptions config)
      throws BasicAccumuloException, IOException {
    String dataset = config.parameters.get(0);
    String table = config.parameters.size() == 2 ? config.parameters.get(1) : null;
    Context context = new Context(args);

    if (table != null) {
      System.out.println(String.format("Deleting table '%s' from dataset '%s'", table, dataset));
    } else {
      System.out.println(String.format("Deleting all tables from dataset '%s'", dataset));
    }

    boolean status = true;
    Deletions deletions = new Deletions();
    if (table != null) {
      deletions.addTable(dataset, table);
    } else {
      deletions.datasetDeletions.add(dataset);
    }
    try {
      softDeleteDatasetsOrTables(context, deletions);
    } catch (Exception e) {
      System.err.println(e);
      status = false;
    }

    if (!status) {
      System.err.println("ERROR: Deletion failed");
      System.exit(1);
    }
    System.out.println("Deletion successful");
  }

  public static void main(String[] args) throws IOException, BasicAccumuloException {
    DeleteDataOptions config = new DeleteDataOptions();
    if (!config.parse(args)) {
      System.exit(1);
    }

    if (config.parameters.size() < 1 || config.parameters.size() > 2) {
      if (config.parameters.size() < 1) {
        System.err.println("ERROR: dataset name required");
      } else if (config.parameters.size() > 2) {
        System.err.println("ERROR: more than two parameters specified");
      }

      System.err.println("Usage: <main_class> [options] <dataset> [<table>]");
      config.printHelp();
      System.exit(1);
    }

    if (config.deleteAllTableVersions
        && config.deletePreviousTableVersions
        && config.tableId != null) {
      System.err.println(
          "Only one of --table-id --delete-previous-versions, and delete-all-versions may be specified at one time");
      System.exit(1);
    }

    if ((config.deleteAllTableVersions
            || config.deletePreviousTableVersions
            || config.tableId != null)
        && !config.purge) {
      System.err.println(
          "The options --table-id, --delete-previous-versions, and delete-all-versions are only compatible with --purge.");
      System.exit(1);
    }

    if (config.purge) {
      purge(args, config);
    } else {
      delete(args, config);
    }
  }

  public enum PurgeVersions {
    Exact,
    All,
    Previous
  }
}
