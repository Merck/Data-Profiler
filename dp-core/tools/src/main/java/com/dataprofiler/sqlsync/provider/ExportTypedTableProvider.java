package com.dataprofiler.sqlsync.provider;

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

import static java.lang.Boolean.parseBoolean;
import static java.lang.Byte.parseByte;
import static java.lang.Double.parseDouble;
import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.lang.Short.parseShort;
import static java.lang.String.format;
import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static org.apache.spark.sql.types.DataTypes.BinaryType;
import static org.apache.spark.sql.types.DataTypes.BooleanType;
import static org.apache.spark.sql.types.DataTypes.ByteType;
import static org.apache.spark.sql.types.DataTypes.DateType;
import static org.apache.spark.sql.types.DataTypes.DoubleType;
import static org.apache.spark.sql.types.DataTypes.FloatType;
import static org.apache.spark.sql.types.DataTypes.IntegerType;
import static org.apache.spark.sql.types.DataTypes.LongType;
import static org.apache.spark.sql.types.DataTypes.ShortType;
import static org.apache.spark.sql.types.DataTypes.StringType;
import static org.apache.spark.sql.types.DataTypes.TimestampType;

import com.dataprofiler.ColumnMetaData;
import com.dataprofiler.DPSparkContext;
import com.dataprofiler.SparkAccumuloIO;
import com.dataprofiler.sqlsync.SqlSyncException;
import com.dataprofiler.sqlsync.spec.SqlSyncSpec;
import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.objects.DatawaveRowObject;
import com.dataprofiler.util.objects.MetadataVersionObject;
import com.dataprofiler.util.objects.ObjectScannerIterable;
import com.dataprofiler.util.objects.VersionedDataScanSpec;
import com.dataprofiler.util.objects.VersionedDatasetMetadata.MissingMetadataException;
import com.dataprofiler.util.objects.VersionedMetadataObject;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.log4j.Logger;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Decimal;
import org.apache.spark.sql.types.StructField;

public class ExportTypedTableProvider implements Serializable {
  private static final long serialVersionUID = 13792L;
  private static final Logger logger = Logger.getLogger(ExportTypedTableProvider.class);
  private static final String EXPORT_SYMBOL = "\u2190";

  public Dataset<Row> exportRows(
      DPSparkContext context, SparkSession spark, SqlSyncSpec sqlSyncSpec, Authorizations auths)
      throws SqlSyncException, IOException, BasicAccumuloException, MissingMetadataException {
    Instant start = now();
    JavaRDD<DatawaveRowObject> pairRDD = readRows(context, sqlSyncSpec, auths);
    if (pairRDD.take(1).size() < 1) {
      throw new SqlSyncException(
          format(
              "No data was found for Dataset '%s' Table '%s')",
              sqlSyncSpec.getDataset(), sqlSyncSpec.getTable()));
    }
    List<StructField> columns = defineOrderedColumns(context, sqlSyncSpec);
    JavaRDD<Row> rowRDD = mapColumnTypes(pairRDD, columns, sqlSyncSpec);
    Dataset<Row> dataset = spark.createDataFrame(rowRDD, DataTypes.createStructType(columns));
    Instant end = now();
    if (logger.isInfoEnabled()) {
      Duration duration = between(start, end);
      logger.info(format("%s initiated export rows time: %s", EXPORT_SYMBOL, duration));
    }
    return dataset;
  }

  public JavaRDD<DatawaveRowObject> readRows(
      DPSparkContext context, SqlSyncSpec sqlSyncSpec, Authorizations auths)
      throws MissingMetadataException, IOException, BasicAccumuloException {
    // TODO: Note: VersionedDataScanSpec may override the context auths so be sure they correct
    // after this ctor
    VersionedDataScanSpec versionedDataScanSpec = new VersionedDataScanSpec(context, sqlSyncSpec);
    DatawaveRowObject datawaveRowObject = new DatawaveRowObject();
    // TODO: clean up. do we really have to set this or can we trust the context to keep its auths
    // correct?
    context.setAuthorizations(auths);
    if (logger.isDebugEnabled()) {
      logger.debug("reading accumulo rows with auths " + context.getAuthorizations());
    }
    SparkAccumuloIO io = new SparkAccumuloIO(context);
    return io.rddFromAccumulo(datawaveRowObject.find(context, versionedDataScanSpec));
  }

  protected List<StructField> defineOrderedColumns(
      DPSparkContext context, SqlSyncSpec sqlSyncSpec) {
    Map<String, ColumnMetaData> columnMetaDataMap =
        fetchColumnMetaData(context, sqlSyncSpec.getDataset(), sqlSyncSpec.getTable());
    overrideColumnMetaData(columnMetaDataMap, sqlSyncSpec.getColumnTypeOverrides());

    List<ColumnMetaData> columnMetaData = new ArrayList<>(columnMetaDataMap.values());
    columnMetaData.sort(comparing(ColumnMetaData::getColumnNumber));

    return columnMetaData.stream()
        .map(entry -> DataTypes.createStructField(entry.name, entry.dataType, entry.nullable))
        .collect(Collectors.toList());
  }

  protected JavaRDD<Row> mapColumnTypes(
      JavaRDD<DatawaveRowObject> pairRDD, List<StructField> columns, SqlSyncSpec sqlSyncSpec) {
    String dateFormat = sqlSyncSpec.getDateFormat();
    DateFormat dateFormatter;
    if (dateFormat != null) {
      dateFormatter = new SimpleDateFormat(dateFormat);
    } else {
      dateFormatter = DateFormat.getDateInstance();
    }

    String timestampFormat = sqlSyncSpec.getTimestampFormat();
    DateFormat timestampFormatter;
    if (timestampFormat != null) {
      timestampFormatter = new SimpleDateFormat(timestampFormat);
    } else {
      timestampFormatter = DateFormat.getTimeInstance();
    }
    return pairRDD.map(
        row -> {
          // This is done in a loop to ensure the order is preserved
          List<Object> fields = new ArrayList<>();
          for (StructField col : columns) {
            DataType type = col.dataType();
            String value = row.getRow().get(col.name());
            long nullCount = 0L;
            if (value == null || value.isEmpty()) {
              if (logger.isDebugEnabled()) {
                logger.debug(format("empty value: %s using null", type));
              }
              nullCount++;
              fields.add(null);
            } else if (type.equals(StringType)) {
              fields.add(value);
            } else if (type.equals(IntegerType)) {
              fields.add(parseInt(value));
            } else if (type.equals(LongType)) {
              fields.add(parseLong(value));
            } else if (type.equals(FloatType)) {
              fields.add(parseFloat(value));
            } else if (type.equals(DoubleType)) {
              fields.add(parseDouble(value));
            } else if (isDecimalType(type)) {
              fields.add(Decimal.apply(new BigDecimal(value)));
            } else if (type.equals(ByteType)) {
              fields.add(parseByte(value));
            } else if (type.equals(ShortType)) {
              fields.add(parseShort(value));
            } else if (type.equals(BinaryType)) {
              fields.add(value.getBytes());
            } else if (type.equals(BooleanType)) {
              fields.add(parseBoolean(value.toLowerCase()));
            } else if (type.equals(TimestampType)) {
              fields.add(new Timestamp(timestampFormatter.parse(value).getTime()));
            } else if (type.equals(DateType)) {
              fields.add(new Date(dateFormatter.parse(value).getTime()));
            } else {
              logger.warn(format("unknown type: %s using default value: %s", type, value));
              fields.add(value);
            }

            if (logger.isTraceEnabled()) {
              logger.trace(
                  format(
                      "column: %s type: %s nullable: %s (nulls found: %s)",
                      col.name(), col.dataType(), col.nullable(), nullCount));
            }
          }

          return RowFactory.create(fields.toArray());
        });
  }

  protected boolean isDecimalType(DataType type) {
    if (isNull(type)) {
      return false;
    }

    if (type.equals(DataTypes.createDecimalType())) {
      if (logger.isDebugEnabled()) {
        logger.debug("found generic decimal type");
      }
      return true;
    }
    int[] precisions = new int[] {4, 8, 12, 16, 24, 32};
    int scale = 0;
    for (int i = 1; i < precisions.length - 1; i++) {
      int precision = precisions[i];
      if (type.equals(DataTypes.createDecimalType(precision, scale))) {
        if (logger.isDebugEnabled()) {
          logger.debug(format("found decimal type precision %s scale %s", precision, scale));
        }
        return true;
      }
    }

    return false;
  }

  protected Map<String, ColumnMetaData> fetchColumnMetaData(
      DPSparkContext context, String dataset, String table) {
    Map<String, ColumnMetaData> columns = new HashMap<>();
    MetadataVersionObject currentVersion = new MetadataVersionObject().fetchCurrentVersion(context);
    ObjectScannerIterable<VersionedMetadataObject> itr =
        new VersionedMetadataObject()
            .scanAllLevelsForTable(context, currentVersion, dataset, table);
    for (VersionedMetadataObject o : itr) {
      if (o.metadata_level != VersionedMetadataObject.COLUMN) {
        continue;
      }

      ColumnMetaData col = new ColumnMetaData(o.column_name, o.data_type, true, o.column_num);
      columns.put(col.name, col);
    }

    for (ColumnMetaData c : columns.values()) {
      logger.warn(c);
    }

    return columns;
  }

  protected void overrideColumnMetaData(
      Map<String, ColumnMetaData> columnMetaData, List<ColumnMetaData> overrides) {
    if (isNull(overrides)) {
      return;
    }
    for (ColumnMetaData c : overrides) {
      if (columnMetaData.containsKey(c.name)) {
        columnMetaData.get(c.name).update(c);
      } else {
        columnMetaData.put(c.name, c);
      }
    }
  }
}
