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

import static com.dataprofiler.util.Const.COL_FAM_METADATA;
import static com.dataprofiler.util.Const.COL_QUAL_METADATA_COLUMN;
import static com.dataprofiler.util.Const.COL_QUAL_METADATA_DATASET;
import static com.dataprofiler.util.Const.COL_QUAL_METADATA_TABLE;
import static com.dataprofiler.util.Const.DATA_TYPE_STRING;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.regex.Pattern.quote;
import static java.util.stream.Collectors.toList;

import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Const;
import com.dataprofiler.util.Context;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import java.beans.Transient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.user.RegExFilter;
import scala.NotImplementedError;

/**
 * * Versioned Metadata allows storing multiple copies of metadata at different points in time. This
 * allows us to get older versions of the data and avoid deleting data (it's simply removed from the
 * metadata).
 *
 * <p>The basic idea is that we add a UUID representing the version id to all of the metadata
 * objects for a version. The current metadata key is stored in a special key (see
 * MetadataVersionObject). All of the row / column count data uses a UUID in the key as well to
 * allow multiple versions of, say, the same table.
 */
public class VersionedMetadataObject
    extends VersionedPurgableDatasetObject<VersionedMetadataObject> {

  // The same object is used to store metadata for dataset, tables, and columns.
  // These constants and the metadata_level field tell you which one this is.
  public static final int DATASET = 0;
  public static final int TABLE = 1;
  public static final int COLUMN = 2;
  private static final String accumuloTable = Const.ACCUMULO_METADATA_TABLE_ENV_KEY;

  @JsonIgnore
  public static TypeReference<VersionedMetadataObject> staticTypeReference =
      new TypeReference<VersionedMetadataObject>() {};

  private static final String[] PATIENT_ID_VALUES = {
    "patid",
    "pat_id",
    "patientid",
    "patient_id",
    "patientkey",
    "patient_key",
    "animalid",
    "animal_id"
  };
  public static Set<String> PATIENT_ID_SET = new HashSet<>(Arrays.asList(PATIENT_ID_VALUES));
  public int metadata_level = -1;

  // This is to make it easier to manage deleted data
  private final String columnFamily = COL_FAM_METADATA;

  // This is a UUID for this version of the metadata
  public String version_id;

  /*
   * The dataset and table level will have 2 or 3 identifiers. The id is a UUID that is unique to
   * that current version of the table. These are generated at each data load. The name is a name
   * that is stable externally - so the name from the external data (usually based on a file name or
   * the column headers in the data). This is useful because it's how we track, for example, that a
   * table being loaded is the same as an existing table. The final name is display_name, which is
   * just a friendly name that can be changed and used in the UI.
   */
  public String dataset_name;
  public String dataset_display_name;

  public String table_id;
  public String table_name;
  public String table_display_name;

  public String column_name;
  public String column_display_name;

  // This is actually the string representation of the Spark datatype - but we
  // can't use
  // that directly here because we don't want a spark dependency at this level.
  public String data_type = DATA_TYPE_STRING;
  public long load_time = Long.MAX_VALUE;
  public long update_time;
  public long num_tables;
  public long num_columns;
  public long num_values;
  public long num_unique_values;
  public boolean pat_id;
  public int column_num = -1; // Used for ordering of columns to match original data
  public Map<String, String> properties = new HashMap<>();

  public VersionedMetadataObject() {
    super(accumuloTable);
    load_time = 0L;
    num_values = 0L;
  }

  public VersionedMetadataObject(
      MetadataVersionObject version,
      String datasetName,
      String tableId,
      String tableName,
      String columnName) {
    this();

    version_id = version.getId();
    dataset_name = datasetName;
    dataset_display_name = datasetName;
    table_id = tableId;
    table_name = tableName;
    table_display_name = tableName;
    column_name = columnName;
    column_display_name = columnName;
    num_columns = 1L;
    num_unique_values = 0L;
    if (columnName != null) {
      pat_id = PATIENT_ID_SET.contains(columnName.toLowerCase());
    }
  }

  @JsonIgnore
  @Transient
  public Boolean isEmpty() {
    return dataset_name.isEmpty()
        && table_name.isEmpty()
        && column_name.isEmpty()
        && num_values == 0
        && load_time == 0L;
  }

  @Override
  public ObjectScannerIterable<VersionedMetadataObject> scan(Context context) {
    return super.scan(context).fetchColumnFamily(columnFamily);
  }

  public ObjectScannerIterable<VersionedMetadataObject> scan(
      Context context, MetadataVersionObject version) {
    return scan(context).addRange(createInclusiveRangeEndDelimited(version.getId()));
  }

  /**
   * Find all of the enhanced metadata records (at all levels) for this dataset. Includes dataset,
   * table, and column records.
   *
   * @param context dataprofiler accumulo context
   * @param version metadata version to scan for
   * @param datasetName name of the dataset
   * @return Accumulo scanner
   */
  public ObjectScannerIterable<VersionedMetadataObject> scanAllLevelsForDataset(
      Context context, MetadataVersionObject version, String datasetName) {
    return scan(context).addRange(createInclusiveRangeEndDelimited(version.getId(), datasetName));
  }

  /**
   * Find metadata within a table. Includes table and column metadata.
   *
   * @param context dataprofiler accumulo context
   * @param version metadata version to scan for
   * @param datasetName name of the dataset
   * @param tableName name of the table
   * @return Accumulo scanner
   */
  public ObjectScannerIterable<VersionedMetadataObject> scanAllLevelsForTable(
      Context context, MetadataVersionObject version, String datasetName, String tableName) {
    return scan(context).addRange(createExclusiveRange(version.getId(), datasetName, tableName));
  }

  public ObjectScannerIterable<VersionedMetadataObject> searchColumnNames(
      Context context, MetadataVersionObject version, String term, Boolean startsWith) {
    return this.searchColumnNames(context, version, Arrays.asList(term), startsWith);
  }

  public ObjectScannerIterable<VersionedMetadataObject> searchColumnNames(
      Context context, MetadataVersionObject version, List<String> terms, Boolean startsWith) {
    ObjectScannerIterable<VersionedMetadataObject> iterable = scan(context, version);
    SearchByNameIteratorBuilder builder = new SearchByNameIteratorBuilder();
    IteratorSetting columnNameRegexIterator =
        builder.setupColumnNameRegexIterator(terms, startsWith);
    return iterable.addScanIterator(columnNameRegexIterator);
  }

  public ObjectScannerIterable<VersionedMetadataObject> searchTableNames(
      Context context, MetadataVersionObject version, String term, Boolean startsWith) {
    return this.searchTableNames(context, version, Arrays.asList(term), startsWith);
  }

  public ObjectScannerIterable<VersionedMetadataObject> searchTableNames(
      Context context, MetadataVersionObject version, List<String> terms, Boolean startsWith) {
    ObjectScannerIterable<VersionedMetadataObject> iterable = scan(context, version);
    SearchByNameIteratorBuilder builder = new SearchByNameIteratorBuilder();
    IteratorSetting tableNameRegexIterator =
        builder.setupTableNameRegexIterator(terms, version, startsWith);
    return iterable
        .addScanIterator(tableNameRegexIterator)
        .fetchColumn(columnFamily, COL_QUAL_METADATA_TABLE);
  }

  public ObjectScannerIterable<VersionedMetadataObject> searchDatasetNames(
      Context context, MetadataVersionObject version, String term, Boolean startsWith) {
    return this.searchDatasetNames(context, version, Arrays.asList(term), startsWith);
  }

  public ObjectScannerIterable<VersionedMetadataObject> searchDatasetNames(
      Context context, MetadataVersionObject version, List<String> terms, Boolean startsWith) {
    ObjectScannerIterable<VersionedMetadataObject> iterable = scan(context, version);
    SearchByNameIteratorBuilder builder = new SearchByNameIteratorBuilder();
    IteratorSetting datasetNameRegexIterator =
        builder.setupDatasetNameRegexIterator(terms, version, startsWith);
    return iterable
        .addScanIterator(datasetNameRegexIterator)
        .fetchColumn(columnFamily, COL_QUAL_METADATA_DATASET);
  }

  public VersionedMetadataObject fetchDataset(
      Context context, MetadataVersionObject version, String datasetName) {
    return fetch(context, createAccumuloKeyForDataset(version.id, datasetName, ""));
  }

  public ObjectScannerIterable<VersionedMetadataObject> scanDatasets(
      Context context, MetadataVersionObject version) {
    return scan(context, version).fetchColumn(columnFamily, COL_QUAL_METADATA_DATASET);
  }

  public ObjectScannerIterable<VersionedMetadataObject> scanDatasetsAndTables(
      Context context, MetadataVersionObject version) {
    ObjectScannerIterable<VersionedMetadataObject> scanner = scan(context, version);

    IteratorSetting iter = new IteratorSetting(100, "filterColQ", RegExFilter.class);
    String colqRegex = format("%s|%s", COL_QUAL_METADATA_DATASET, COL_QUAL_METADATA_TABLE);
    RegExFilter.setRegexs(iter, null, null, colqRegex, null, false);
    scanner.addScanIterator(iter);

    return scanner;
  }

  public VersionedMetadataObject fetchTable(
      Context context, MetadataVersionObject version, String datasetName, String tableName) {
    return fetch(context, createAccumuloKeyForTable(version.id, datasetName, tableName, ""));
  }

  /***
   * Find all versions that contain the given table. Note that this will return a
   * VersionedMetadataObject that matches this table for every version of the metadata that it
   * exists in. An actual table version (as represented by a unique table id) may appear multiple
   * times. The caller would need to de-duplicate that if you want table versions.
   *
   * There is a pretty good chance that you actually want something in TableVersionInfo instead.
   *
   * @param context
   * @param datasetName the name of the dataset
   * @param tableName the name of the table (not an id - the stable name)
   * @return an iterable that will return an iterator over the versions.
   */
  public ObjectScannerIterable<VersionedMetadataObject> scanTableAllVersions(
      Context context, String datasetName, String tableName) {
    ObjectScannerIterable<VersionedMetadataObject> iterable = scan(context);

    // The goal here is to find all versions of the table. The regex therefore ignores the version,
    // just filtering on the dataset and
    // table name. As usual the (?s) is included so that the . operator will also match newlines
    // (since our identifiers can include
    // newlines).
    IteratorSetting iter = new IteratorSetting(100, "filterTableName", RegExFilter.class);
    String rowRegex = format("(?s).*%s%s%s", quote(datasetName), Const.DELIMITER, quote(tableName));
    RegExFilter.setRegexs(iter, rowRegex, null, null, null, false);
    iterable.addScanIterator(iter);
    iterable.fetchColumn(COL_FAM_METADATA, COL_QUAL_METADATA_TABLE);

    return iterable;
  }

  public ObjectScannerIterable<VersionedMetadataObject> scanTables(
      Context context, MetadataVersionObject version, String datasetName) {
    return scanAllLevelsForDataset(context, version, datasetName)
        .fetchColumn(columnFamily, COL_QUAL_METADATA_TABLE);
  }

  /***
   * Scan all versions of this dataset.
   * @param context
   * @param datasetName
   * @return
   */
  public ObjectScannerIterable<VersionedMetadataObject> scanDatasetAllVersions(
      Context context, String datasetName) {
    ObjectScannerIterable<VersionedMetadataObject> iterable = scan(context);
    // The goal here is to find all versions of the dataset. The regex therefore ignores the
    // version,
    // just filtering on the dataset.As usual the (?s) is included so that the . operator will also
    // match newlines (since our identifiers can include newlines).
    IteratorSetting iter = new IteratorSetting(100, "filterDatasetName", RegExFilter.class);
    String rowRegex = format("(?s).*%s%s", quote(datasetName), Const.DELIMITER);
    RegExFilter.setRegexs(iter, rowRegex, null, null, null, false);
    iterable.addScanIterator(iter);
    iterable.fetchColumn(COL_FAM_METADATA, COL_QUAL_METADATA_DATASET);

    return iterable;
  }

  public ObjectScannerIterable<VersionedMetadataObject> scanAllDatasetsAllVersions(
      Context context) {
    ObjectScannerIterable<VersionedMetadataObject> iterable = scan(context);

    iterable.fetchColumn(COL_FAM_METADATA, COL_QUAL_METADATA_DATASET);

    return iterable;
  }

  /** Scan all tables in all of the versions of this dataset. */
  public ObjectScannerIterable<VersionedMetadataObject> scanAllTablesAllVersions(
      Context context, String datasetName) {
    ObjectScannerIterable<VersionedMetadataObject> iterable = scan(context);

    // The goal here is to find all versions of all of the tables in this dataset. The regex
    // therefore ignores the version, just filtering on the dataset and
    // As usual the (?s) is included so that the . operator will also match newlines
    // (since our identifiers can include newlines).
    IteratorSetting iter = new IteratorSetting(100, "filterDatasetName", RegExFilter.class);
    String rowRegex = format("(?s).*%s%s.*", datasetName, Const.DELIMITER);
    RegExFilter.setRegexs(iter, rowRegex, null, null, null, false);
    iterable.addScanIterator(iter);
    iterable.fetchColumn(COL_FAM_METADATA, COL_QUAL_METADATA_TABLE);

    return iterable;
  }

  public ObjectScannerIterable<VersionedMetadataObject> scanAllTables(
      Context context, MetadataVersionObject version) {
    return scan(context, version).fetchColumn(columnFamily, COL_QUAL_METADATA_TABLE);
  }

  /***
   * Find all versions that contain the given table. Note that this will return a
   * VersionedMetadataObject that matches this column for every version of the metadata that it
   * exists in. An actual column version (as represented by a unique column id) may appear multiple
   * times. The caller would need to de-duplicate that if you want column versions.
   *
   * @param context
   * @param datasetName the name of the dataset
   * @param tableName the name of the table (not an id - the stable name)
   * @param columnName the name of the column (not an id - the stable name)
   * @return an iterable that will return an iterator over the versions.
   */
  public ObjectScannerIterable<VersionedMetadataObject> scanColumnAllVersions(
      Context context, String datasetName, String tableName, String columnName) {
    ObjectScannerIterable<VersionedMetadataObject> iterable = scan(context);

    // The goal here is to find all versions of the column. The regex therefore ignores the version,
    // just filtering on the dataset and table name.
    // As usual the (?s) is included so that the . operator will also match newlines
    // (since our identifiers can include newlines).
    IteratorSetting iter = new IteratorSetting(100, "filterColumnName", RegExFilter.class);
    String rowRegex =
        format(
            "(?s).*%s%s%s%s%s",
            quote(datasetName),
            Const.DELIMITER,
            quote(tableName),
            Const.DELIMITER,
            quote(columnName));
    RegExFilter.setRegexs(iter, rowRegex, null, null, null, false);
    iterable.addScanIterator(iter);
    iterable.fetchColumn(COL_FAM_METADATA, COL_QUAL_METADATA_COLUMN);

    return iterable;
  }

  public VersionedMetadataObject fetchColumn(
      Context context,
      MetadataVersionObject version,
      String datasetName,
      String tableName,
      String columnName) {

    return fetch(
        context, createAccumuloKeyForColumn(version.id, datasetName, tableName, columnName, ""));
  }

  public ObjectScannerIterable<VersionedMetadataObject> scanColumns(
      Context context, MetadataVersionObject version, String datasetName, String tableName) {
    ObjectScannerIterable<VersionedMetadataObject> scanner =
        scanAllLevelsForTable(context, version, datasetName, tableName);

    IteratorSetting iter = new IteratorSetting(100, "filterColQ", RegExFilter.class);
    String colqRegex = format("(?s)%s.*", COL_QUAL_METADATA_COLUMN + Const.DELIMITER);
    RegExFilter.setRegexs(iter, null, null, colqRegex, null, false);
    scanner.addScanIterator(iter);

    return scanner;
  }

  /**
   * The key varies depending on which level of metadata this is (dataset, table, or column).
   *
   * <p>All forms have Col Fam set to Const.COL_FAM_ENHANCED_METADATA. The Col Qual and Row ID are
   * what varies.
   *
   * <p>For Dataset it's:
   *
   * <p>RowID: VerID DELIM dataset DELIM
   *
   * <p>Col Qual: D
   *
   * <p>For Table it's:
   *
   * <p>RowId: VerID DELIM dataset DELIM table
   *
   * <p>Col Qual: T
   *
   * <p>For Column it's:
   *
   * <p>RowID: VerID DELIM dataset DELIM table
   *
   * <p>Col Qual: C DELIM column name
   */
  @Override
  public Key createAccumuloKey() throws InvalidDataFormat {
    Key key;
    if (metadata_level == DATASET) {
      key = createAccumuloKeyForDataset(version_id, dataset_name, visibility);
    } else if (metadata_level == TABLE) {
      key = createAccumuloKeyForTable(version_id, dataset_name, table_name, visibility);
    } else {
      key =
          createAccumuloKeyForColumn(version_id, dataset_name, table_name, column_name, visibility);
    }

    return key;
  }

  public Key createAccumuloKeyForDataset(String versionId, String datasetName, String visibility) {

    return new Key(
        // So these are endDelimited out of a misguided attempt to make scanning easier. Live and
        // learn.
        joinKeyComponentsEndDelimited(versionId, datasetName),
        columnFamily,
        COL_QUAL_METADATA_DATASET,
        visibility);
  }

  public Key createAccumuloKeyForTable(
      String versionId, String datasetName, String tableName, String visibility) {
    return new Key(
        joinKeyComponents(versionId, datasetName, tableName),
        columnFamily,
        Const.COL_QUAL_METADATA_TABLE,
        visibility);
  }

  public Key createAccumuloKeyForColumn(
      String versionId,
      String datasetName,
      String tableName,
      String columnName,
      String visibility) {
    return new Key(
        joinKeyComponents(versionId, datasetName, tableName),
        columnFamily,
        joinKeyComponents(COL_QUAL_METADATA_COLUMN, columnName),
        visibility);
  }

  @Override
  public VersionedMetadataObject fromEntry(Entry<Key, Value> entry) {
    VersionedMetadataObject em = null;
    try {
      em = mapper.readValue(entry.getValue().get(), staticTypeReference);
    } catch (IOException e) {
      throw new InvalidDataFormat(e.toString());
    }
    em.updatePropertiesFromEntry(entry);

    return em;
  }

  @Override
  public void bulkPurgeDataset(Context context, MetadataVersionObject version, String dataset)
      throws BasicAccumuloException {

    // This is such a small amount of data it ended up being incredibly slow to do bulk deletion.
    BatchWriter writer = context.createBatchWriter(getTable(context));
    try {
      for (VersionedMetadataObject o :
          scanAllLevelsForDataset(context, version, dataset).setBatch(true)) {
        writer.addMutation(o.createDeleteMutation());
      }

      writer.close();
    } catch (MutationsRejectedException e) {
      throw new BasicAccumuloException(e.toString());
    }
  }

  @Override
  public void bulkPurgeTable(
      Context context, String datasetName, String tableName, String tableId) {
    // we just can't implement this efficiently for metadata and we don't really want to
    throw new NotImplementedError();
  }

  /***
   * Purge all of the metadata associated with a table for a single version. We want to be able to
   * do this for metadata as part of delete - see DeleteData.
   * @param context
   * @param tableMetadata
   * @throws BasicAccumuloException
   */
  public void bulkPurgeTable(Context context, VersionedMetadataObject tableMetadata)
      throws BasicAccumuloException {
    System.out.println("bulk deleting metadata table: " + tableMetadata.version_id);
    // This is such a small amount of data it ended up being incredibly slow to do
    // bulk deletion.
    BatchWriter writer = context.createBatchWriter(getTable(context));
    try {
      for (VersionedMetadataObject o :
          scanAllLevelsForTable(
                  context,
                  new MetadataVersionObject(tableMetadata.version_id),
                  tableMetadata.dataset_name,
                  tableMetadata.table_name)
              .setBatch(true)) {
        writer.addMutation(o.createDeleteMutation());
      }

      writer.close();
    } catch (MutationsRejectedException e) {
      throw new BasicAccumuloException(e.toString());
    }
  }

  public VersionedDatasetMetadata allMetadataForDataset(Context context, String datasetName) {
    return allMetadataForDataset(context, context.getCurrentMetadataVersion(), datasetName);
  }

  public VersionedDatasetMetadata allMetadataForDataset(
      Context context, MetadataVersionObject version, String datasetName) {
    return new VersionedDatasetMetadata(
        version,
        new VersionedMetadataObject().scanAllLevelsForDataset(context, version, datasetName));
  }

  public VersionedTableMetadata allMetadataForTable(
      Context context, String datasetName, String tableName) {
    return allMetadataForTable(
        context, context.getCurrentMetadataVersion(), datasetName, tableName);
  }

  public VersionedTableMetadata allMetadataForTable(
      Context context, MetadataVersionObject version, String datasetName, String tableName) {
    return new VersionedTableMetadata(
        version,
        new VersionedMetadataObject()
            .scanAllLevelsForTable(context, version, datasetName, tableName));
  }

  /***
   * Sort a map of version id -> metatadata into a list of metadata ordered by version given a sorted list of metadata version objects. You can get
   * the ordered list of metadata version from MetadataVersionObject.allMetdataVersions.
   *
   * @param metadata
   * @param versions
   * @return order list of VersionedMetadataObject
   */
  public static List<VersionedMetadataObject> sortByVersion(
      Map<String, VersionedMetadataObject> metadata, List<MetadataVersionObject> versions) {
    ArrayList<VersionedMetadataObject> output = new ArrayList<>();

    for (MetadataVersionObject v : versions) {
      if (metadata.containsKey(v.id)) {
        output.add(metadata.get(v.id));
      }
    }

    return output;
  }

  public VersionedAllMetadata allMetadata(Context context, MetadataVersionObject version) {
    return new VersionedAllMetadata(context, version);
  }

  /**
   * Return a map of dataset DELIM table ids to table names. This is used when we need to scan data
   * but want to only return tables that are present in the current version (like the index
   * scanning). It's a map so that the returned table ids can be transformed back into table names.
   *
   * @param context data profiler context
   * @param version version for which tables should be returned
   * @return a map of table id -> table name for all active tables in the provided version
   */
  public Map<String, String> allTableNamesForVersion(
      Context context, MetadataVersionObject version) {
    return StreamSupport.stream(scanAllTables(context, version).spliterator(), true)
        .collect(
            Collectors.toMap(
                x -> joinKeyComponents(x.getDataset_name(), x.getTable_id()),
                VersionedMetadataObject::getTable_name));
  }

  public int getMetadata_level() {
    return metadata_level;
  }

  public void setMetadata_level(int metadata_level) {
    this.metadata_level = metadata_level;
  }

  public String getDataset_name() {
    return dataset_name;
  }

  public void setDataset_name(String dataset_name) {
    this.dataset_name = dataset_name;
  }

  public String getTable_name() {
    return table_name;
  }

  public void setTable_name(String table_name) {
    this.table_name = table_name;
  }

  public String getTable_id() {
    return table_id;
  }

  public void setTable_id(String table_id) {
    this.table_id = table_id;
  }

  public String getColumn_name() {
    return column_name;
  }

  public void setColumn_name(String column_name) {
    this.column_name = column_name;
  }

  public Long getLoad_time() {
    return load_time;
  }

  public void setLoad_time(Long load_time) {
    this.load_time = load_time;
  }

  public Long getUpdate_time() {
    return update_time;
  }

  public void setUpdate_time(Long update_time) {
    this.update_time = update_time;
  }

  public Long getNum_tables() {
    return num_tables;
  }

  public void setNum_tables(Long num_tables) {
    this.num_tables = num_tables;
  }

  public Long getNum_columns() {
    return num_columns;
  }

  public void setNum_columns(Long num_columns) {
    this.num_columns = num_columns;
  }

  public Long getNum_values() {
    return num_values;
  }

  public void setNum_values(Long num_values) {
    this.num_values = num_values;
  }

  public Long getNum_unique_values() {
    return num_unique_values;
  }

  public void setNum_unique_values(Long num_unique_values) {
    this.num_unique_values = num_unique_values;
  }

  public Boolean getPat_id() {
    return pat_id;
  }

  public void setPat_id(Boolean pat_id) {
    this.pat_id = pat_id;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  /**
   * Merge and set properties. The reason that this takes two maps is so that you can control which
   * set of properties are primary - i.e., if there are overlapping keys the primary set of
   * properties will be used.
   *
   * @param primaryProperties the properties that should win if there are overlapping keys
   * @param propertiesToMerge additional properties to merge in if they key doesn't already exist
   */
  public void setMergedProperties(
      Map<String, String> primaryProperties, Map<String, String> propertiesToMerge) {
    HashMap<String, String> newProps = new HashMap<>(primaryProperties);
    for (Entry<String, String> property : propertiesToMerge.entrySet()) {
      if (newProps.containsKey(property.getKey())) {
        continue;
      }
      newProps.put(property.getKey(), property.getValue());
    }
    this.properties = newProps;
  }

  /**
   * Merge other properties into this metadata only replacing keys that aren't already present.
   *
   * @param propertiesToMerge properties to merge with the existing metadata properties.
   */
  public void setMergedProperties(Map<String, String> propertiesToMerge) {
    setMergedProperties(this.properties, propertiesToMerge);
  }

  @Override
  public String toString() {
    return "VersionedMetadataObject{"
        + "metadata_level="
        + metadata_level
        + ", version_id='"
        + version_id
        + '\''
        + ", dataset_name='"
        + dataset_name
        + '\''
        + ", dataset_display_name='"
        + dataset_display_name
        + '\''
        + ", table_id='"
        + table_id
        + '\''
        + ", table_name='"
        + table_name
        + '\''
        + ", table_display_name='"
        + table_display_name
        + '\''
        + ", column_name='"
        + column_name
        + '\''
        + ", column_display_name='"
        + column_display_name
        + '\''
        + ", data_type='"
        + data_type
        + '\''
        + ", load_time="
        + load_time
        + ", update_time="
        + update_time
        + ", num_tables="
        + num_tables
        + ", num_columns="
        + num_columns
        + ", num_values="
        + num_values
        + ", num_unique_values="
        + num_unique_values
        + ", pat_id="
        + pat_id
        + ", column_num="
        + column_num
        + ", properties="
        + properties
        + ", visibility="
        + getVisibility()
        + ", columnFamily="
        + columnFamily
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    VersionedMetadataObject that = (VersionedMetadataObject) o;
    return metadata_level == that.metadata_level
        && version_id.equals(that.version_id)
        && dataset_name.equals(that.dataset_name)
        && Objects.equals(table_id, that.table_id)
        && Objects.equals(table_name, that.table_name)
        && Objects.equals(column_name, that.column_name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        metadata_level, version_id, dataset_name, table_id, table_name, column_name);
  }

  private static class SearchByNameIteratorBuilder {

    SearchByNameIteratorBuilder() {
      super();
    }

    String unquotedColqRegex(String term, boolean startsWith) {
      return format(
          "(?s)(?i)%s%s.*",
          startsWith ? "" : ".*", joinKeyComponents(COL_QUAL_METADATA_COLUMN, term));
    }

    String quotedColqRegex(String term, boolean startsWith) {
      return unquotedColqRegex(quote(term), startsWith);
    }

    String quotedColqRegex(List<String> terms, boolean startsWith) {
      List<String> quotedTerms = terms.stream().map(Pattern::quote).collect(toList());
      String search = join(".*", quotedTerms);
      return unquotedColqRegex(search, startsWith);
    }

    String unquotedTableRegex(String term, MetadataVersionObject version, boolean startsWith) {
      return format(
          "(?s)%s%s(?i).*%s%s%s.*",
          version.getId(), Const.DELIMITER, Const.DELIMITER, startsWith ? "" : ".*", term);
    }

    String quotedTableRegex(String term, MetadataVersionObject version, boolean startsWith) {
      return unquotedTableRegex(quote(term), version, startsWith);
    }

    String quotedTableRegex(List<String> terms, MetadataVersionObject version, boolean startsWith) {
      List<String> quotedTerms = terms.stream().map(Pattern::quote).collect(toList());
      String search = join(".*", quotedTerms);
      return unquotedTableRegex(search, version, startsWith);
    }

    String unquotedDatasetRegex(String term, MetadataVersionObject version, boolean startsWith) {
      String search = format("%s%s%s", startsWith ? "" : ".*", term, ".*");
      return format(
          "(?s)%s%s(?i)%s%s.*", version.getId(), Const.DELIMITER, search, Const.DELIMITER);
    }

    String quotedDatasetRegex(String term, MetadataVersionObject version, boolean startsWith) {
      return unquotedDatasetRegex(quote(term), version, startsWith);
    }

    String quotedDatasetRegex(
        List<String> terms, MetadataVersionObject version, boolean startsWith) {
      List<String> quotedTerms = terms.stream().map(Pattern::quote).collect(toList());
      String search = join(".*", quotedTerms);
      return unquotedDatasetRegex(search, version, startsWith);
    }

    IteratorSetting quotedRegexToColumnNameRegexIterator(String quotedColqRegex) {
      IteratorSetting iter = new IteratorSetting(100, "filterColQ", RegExFilter.class);
      RegExFilter.setRegexs(iter, null, null, quotedColqRegex, null, false);
      return iter;
    }

    IteratorSetting quotedRegexToTableNameRegexIterator(String quotedRegex) {
      IteratorSetting iter = new IteratorSetting(100, "filterRow", RegExFilter.class);
      RegExFilter.setRegexs(iter, quotedRegex, null, null, null, false);
      return iter;
    }

    IteratorSetting quotedRegexToDatasetNameRegexIterator(String quotedRegex) {
      IteratorSetting iter = new IteratorSetting(100, "filterRow", RegExFilter.class);
      RegExFilter.setRegexs(iter, quotedRegex, null, null, null, false);
      return iter;
    }

    IteratorSetting setupColumnNameRegexIterator(String term, boolean startsWith) {
      return quotedRegexToColumnNameRegexIterator(quotedColqRegex(term, startsWith));
    }

    IteratorSetting setupColumnNameRegexIterator(List<String> terms, boolean startsWith) {
      return quotedRegexToColumnNameRegexIterator(quotedColqRegex(terms, startsWith));
    }

    IteratorSetting setupTableNameRegexIterator(
        String term, MetadataVersionObject version, boolean startsWith) {
      return quotedRegexToTableNameRegexIterator(quotedTableRegex(term, version, startsWith));
    }

    IteratorSetting setupTableNameRegexIterator(
        List<String> terms, MetadataVersionObject version, boolean startsWith) {
      return quotedRegexToTableNameRegexIterator(quotedTableRegex(terms, version, startsWith));
    }

    IteratorSetting setupDatasetNameRegexIterator(
        String term, MetadataVersionObject version, boolean startsWith) {
      return quotedRegexToDatasetNameRegexIterator(quotedDatasetRegex(term, version, startsWith));
    }

    IteratorSetting setupDatasetNameRegexIterator(
        List<String> terms, MetadataVersionObject version, boolean startsWith) {
      return quotedRegexToDatasetNameRegexIterator(quotedDatasetRegex(terms, version, startsWith));
    }
  }
}
