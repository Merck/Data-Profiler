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

import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Const;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.objects.InvalidDataFormat;
import com.dataprofiler.util.objects.MetadataVersionObject;
import com.dataprofiler.util.objects.ObjectScannerIterable;
import com.dataprofiler.util.objects.PurgableDatasetObject;
import com.dataprofiler.util.objects.VersionedMetadataObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import java.beans.Transient;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.user.RegExFilter;
import org.apache.hadoop.io.Text;

public class ConvertMetadataToVersioned {
  private static class MetadataObject extends PurgableDatasetObject<MetadataObject> {

    // The same object is used to store metadata for dataset, tables, and columns. These constants
    // and the metadata_level field tell you which one this is.
    public static final int DATASET = 0;
    public static final int TABLE = 1;
    public static final int COLUMN = 2;
    private static final String accumuloTable = Const.ACCUMULO_METADATA_TABLE_ENV_KEY;

    @JsonIgnore
    public static TypeReference<MetadataObject> staticTypeReference =
        new TypeReference<MetadataObject>() {};

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

    public String dataset_name;
    public String table_name;
    public String column_name;
    // This is actually the string representation of the Spark datatype - but we can't use
    // that directly here because we don't want a spark dependency at this level.
    public String data_type = Const.DATA_TYPE_STRING;
    public Long load_time;
    public Long update_time;
    public Long num_tables;
    public Long num_columns;
    public Long num_values;
    public Long num_unique_values;
    public Boolean pat_id;
    public int column_num = -1; // Used for ordering of columns to match original data
    public Map<String, String> properties = new HashMap<>();

    public MetadataObject() {
      super(accumuloTable);
      load_time = 0L;
      num_values = 0L;
    }

    public MetadataObject(String datasetName, String tableName, String columnName) {
      this();

      dataset_name = datasetName;
      table_name = tableName;
      column_name = columnName;
      num_columns = 1L;
      num_unique_values = 0L;
      pat_id = PATIENT_ID_SET.contains(columnName.toLowerCase());
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
    public ObjectScannerIterable<MetadataObject> scan(Context context) {
      return super.scan(context).fetchColumnFamily(Const.COL_FAM_UNVERSIONED_METADATA);
    }

    public ObjectScannerIterable<MetadataObject> findAllMetadata(Context context) {
      return super.scan(context)
          .addRange(new Range(Const.ACCUMULO_META_NS, Const.ACCUMULO_META_NS + Const.HIGH_BYTE));
    }

    /**
     * Find all of the enhanced metadata records (at all levels) for this dataset. Includes dataset,
     * table, and column records.
     *
     * @param context
     * @param datasetName name of the dataset
     * @return Accumulo scanner
     */
    public ObjectScannerIterable<MetadataObject> findAllLevelsForDataset(
        Context context, String datasetName) {
      // This is a weird case that doesn't have a convenience method
      Text start = new Text(datasetName);
      Text end = new Text(datasetName + Const.DELIMITER + Const.HIGH_BYTE);
      Range range = new Range(start, end);
      return scan(context).addRange(range);
    }

    public ObjectScannerIterable<MetadataObject> findAllLevelsForTable(
        Context context, String datasetName, String tableName) {
      return scan(context).addRange(createExclusiveRange(datasetName, tableName));
    }

    /**
     * Find a specific table within a dataset
     *
     * @param context
     * @param datasetName
     * @param tableName
     * @return
     */
    public ObjectScannerIterable<MetadataObject> findTable(
        Context context, String datasetName, String tableName) {
      return scan(context)
          .addRange(createInclusiveRange(datasetName, tableName))
          .fetchColumn(Const.COL_FAM_UNVERSIONED_METADATA, "");
    }

    /**
     * Find a specific column within a dataset
     *
     * @param context
     * @param datasetName
     * @param tableName
     * @param columnName
     * @return
     */
    public ObjectScannerIterable<MetadataObject> findColumn(
        Context context, String datasetName, String tableName, String columnName) {
      return scan(context)
          .addRange(createInclusiveRange(datasetName, tableName))
          .fetchColumn(Const.COL_FAM_UNVERSIONED_METADATA, columnName);
    }

    public ObjectScannerIterable<MetadataObject> searchColumnNames(
        Context context, String term, Boolean startsWith) {
      ObjectScannerIterable<MetadataObject> iterable = scan(context);

      IteratorSetting iter = new IteratorSetting(100, "filterColQ", RegExFilter.class);
      String colqRegex = String.format("(?i)%s%s.*", startsWith ? "" : ".*", term);
      RegExFilter.setRegexs(iter, null, null, colqRegex, null, false);
      iterable.addScanIterator(iter);

      return iterable;
    }

    public ObjectScannerIterable<MetadataObject> searchTableNames(
        Context context, String term, Boolean startsWith) {
      ObjectScannerIterable<MetadataObject> iterable = scan(context);

      IteratorSetting iter = new IteratorSetting(100, "filterColQ", RegExFilter.class);
      String rowRegex =
          String.format("(?i).*%s%s%s.*", Const.DELIMITER, startsWith ? "" : ".*", term);
      RegExFilter.setRegexs(iter, rowRegex, null, "^$", null, false);
      iterable.addScanIterator(iter);

      return iterable;
    }

    /**
     * The key varies depending on which level of metadata this is (dataset, table, or column).
     *
     * <p>All forms have Col Fam set to Const.COL_FAM_UNVERSIONED_METADATA. The Col Qual and Row ID
     * are what varies.
     *
     * <p>For Dataset it's:
     *
     * <p>RowID: dataset
     *
     * <p>Col Qual: "" For Table it's:
     *
     * <p>RowId: dataset DELIM table
     *
     * <p>Col Qual: "" For Column it's:
     *
     * <p>RowID: dataset DELIM table
     *
     * <p>Col Qual: column name
     *
     * @return
     * @throws InvalidDataFormat
     */
    @Override
    public Key createAccumuloKey() throws InvalidDataFormat {
      String colFam = Const.COL_FAM_UNVERSIONED_METADATA;
      String rowId;
      String colQual;
      if (metadata_level == DATASET) {
        rowId = dataset_name;
        colQual = "";
      } else if (metadata_level == TABLE) {
        rowId = joinKeyComponents(dataset_name, table_name);
        colQual = "";
      } else {
        rowId = joinKeyComponents(dataset_name, table_name);
        colQual = column_name;
      }

      return new Key(rowId, colFam, colQual, visibility);
    }

    @Override
    public MetadataObject fromEntry(Entry<Key, Value> entry) {
      MetadataObject em = null;
      try {
        em = mapper.readValue(entry.getValue().get(), staticTypeReference);
      } catch (IOException e) {
        throw new InvalidDataFormat(e.toString());
      }

      // The metadata_level property is new and is not in all entries. So if it's not been
      // set when we deserialized, figure it out based on the key.
      if (em.metadata_level == -1) {
        Key key = entry.getKey();

        if (key.getColumnQualifier().toString().equals(em.column_name)) {
          em.metadata_level = COLUMN;
        } else if (key.getRow().toString().equals(em.dataset_name)) {
          em.metadata_level = DATASET;
        } else {
          em.metadata_level = TABLE;
        }
      }
      em.updatePropertiesFromEntry(entry);

      return em;
    }

    @Override
    public void bulkPurgeDataset(Context context, String dataset) throws BasicAccumuloException {
      // The prevVal method is used here because `deleteRows` is between (start, end]
      // Since the start is exclusive, we have to get the previous lexicographical value
      Text start = new Text(prevVal(dataset));
      Text end = new Text(dataset + Const.DELIMITER + Const.HIGH_BYTE);
      bulkPurgeRange(context, start, end);
    }

    @Override
    public void bulkPurgeTable(Context context, String dataset, String table)
        throws BasicAccumuloException {
      // The prevVal method is used here because `deleteRows` is between (start, end]
      // Since the start is exclusive, we have to get the previous lexicographical value
      Text start = new Text(prevVal(joinKeyComponents(dataset, table)));
      Text end = new Text(joinKeyComponentsEndDelimited(dataset, table));
      bulkPurgeRange(context, start, end);
    }

    public MetadataObject.DatasetMetadata allMetadataForDataset(
        Context context, String datasetName) {
      return new MetadataObject.DatasetMetadata(
          new MetadataObject().findAllLevelsForDataset(context, datasetName));
    }

    public MetadataObject.DatasetMetadata allMetadataForTable(
        Context context, String datasetName, String tableName) {
      return new MetadataObject.DatasetMetadata(
          new MetadataObject().findAllLevelsForTable(context, datasetName, tableName));
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

    @Override
    public String toString() {
      return "MetadataObject{"
          + "metadata_level="
          + metadata_level
          + ", dataset_name='"
          + dataset_name
          + '\''
          + ", table_name='"
          + table_name
          + '\''
          + ", column_name='"
          + column_name
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
          + '}';
    }

    public class DatasetMetadata {
      /**
       * Metadata for the entire dataset - there should only ever be one since this represents a
       * single dataset.
       */
      public MetadataObject datasetMetadata;
      /** All of the table level metadata - this is keyed by table name. */
      public Map<String, MetadataObject> tableMetadata = new HashMap<>();
      /** All of the column level metadata - this is keyed by table:column */
      public Map<String, MetadataObject> columnMetadata = new HashMap<>();

      public DatasetMetadata(ObjectScannerIterable<MetadataObject> iteratable) {
        iteratable.setBatch(true);
        for (MetadataObject m : iteratable) {
          switch (m.metadata_level) {
            case MetadataObject.DATASET:
              datasetMetadata = m;
              break;
            case MetadataObject.TABLE:
              tableMetadata.put(m.table_name, m);
              break;
            case MetadataObject.COLUMN:
              columnMetadata.put(createColumnKey(m.table_name, m.column_name), m);
              break;
            default:
              assert (false);
          }
        }
      }

      public String createColumnKey(String table, String column) {
        return String.format("%s:%s", table, column);
      }

      public MetadataObject getColumnMetadata(String table, String column) {
        return columnMetadata.get(createColumnKey(table, column));
      }

      public void put(Context context, boolean dataset, boolean tables, boolean columns)
          throws BasicAccumuloException, IOException {
        BatchWriter writer = context.createBatchWriter(datasetMetadata.getTable(context));

        if (dataset) {
          datasetMetadata.put(context, writer);
        }

        if (tables) {
          for (MetadataObject o : tableMetadata.values()) {
            o.put(context, writer);
          }
        }

        if (columns) {
          for (MetadataObject o : columnMetadata.values()) {
            o.put(context, writer);
          }
        }

        try {
          writer.close();
        } catch (MutationsRejectedException e) {
          throw new BasicAccumuloException(e.toString());
        }
      }

      public void putAll(Context context) throws IOException, BasicAccumuloException {
        put(context, true, true, true);
      }

      public void putDatasetAndTables(Context context) throws IOException, BasicAccumuloException {
        put(context, true, true, false);
      }
    }
  }

  public static void main(String[] args)
      throws IOException, BasicAccumuloException, MutationsRejectedException {
    MetadataVersionObject version = MetadataVersionObject.newRandomMetadataVersion();
    Context context = new Context(args);

    BatchWriter writer = context.createBatchWriter(new VersionedMetadataObject().getTable(context));

    for (MetadataObject m : new MetadataObject().scan(context)) {
      VersionedMetadataObject vm = new VersionedMetadataObject();
      vm.version_id = version.getId();
      vm.metadata_level = m.metadata_level;
      vm.dataset_name = m.dataset_name;
      vm.dataset_display_name = m.dataset_name;

      vm.table_name = m.table_name;
      vm.table_id = m.table_name;
      vm.table_display_name = m.table_name;

      vm.column_name = m.column_name;
      vm.column_display_name = m.column_name;
      vm.column_num = m.column_num;

      vm.data_type = m.data_type;
      vm.load_time = m.load_time;
      vm.update_time = m.update_time;
      if (m.num_tables != null) {
        vm.num_tables = m.num_tables;
      }
      vm.num_columns = m.num_columns;
      if (m.num_values != null) {
        vm.num_values = m.num_values;
      }

      if (m.num_unique_values != null) {
        vm.num_unique_values = m.num_unique_values;
      }

      if (m.pat_id != null) {
        vm.pat_id = m.pat_id;
      }
      vm.setProperties(m.properties);
      vm.setVisibility(m.getVisibility());

      vm.put(context, writer);
    }

    version.put(context, writer);
    version.putAsCurrentVersion(context, writer);

    writer.flush();
    writer.close();
  }
}
