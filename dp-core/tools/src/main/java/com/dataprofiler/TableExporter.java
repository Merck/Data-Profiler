package com.dataprofiler;

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
import com.dataprofiler.util.objects.ColumnCountObject;
import com.dataprofiler.util.objects.DataScanSpec;
import com.dataprofiler.util.objects.DataScanSpec.Type;
import com.dataprofiler.util.objects.DatawaveRowObject;
import com.dataprofiler.util.objects.VersionedDataScanSpec;
import com.dataprofiler.util.objects.VersionedDatasetMetadata.MissingMetadataException;
import com.dataprofiler.util.objects.VersionedMetadataObject;
import com.dataprofiler.util.objects.VersionedTableMetadata;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

public class TableExporter {

  private static Dataset<Row> rowRddToExportDataframe(
      SparkSession spark, JavaRDD<Row> rows, StructType schema) {
    return rowRddToExportDataframe(spark, rows, schema, true);
  }

  private static Dataset<Row> rowRddToExportDataframe(
      SparkSession spark, JavaRDD<Row> rows, StructType schema, boolean coalesce) {
    Dataset<Row> df = spark.createDataFrame(rows, schema);

    // Since we are preparing for export, we want a single file. So we coalesce into a single
    // partition.
    // TRJN-1951: removing coalesce seemed to help with query performance of Spark SQL queries
    return coalesce ? df.coalesce(1) : df;
  }

  public static Dataset<Row> exportRows(
      DPSparkContext context, SparkSession spark, SparkAccumuloIO io, DataScanSpec ds)
      throws IOException, BasicAccumuloException, MissingMetadataException {

    return exportRows(context, spark, io, ds, null);
  }

  public static Dataset<Row> exportRows(
      DPSparkContext context,
      SparkSession spark,
      SparkAccumuloIO io,
      DataScanSpec ds,
      Authorizations auths)
      throws IOException, BasicAccumuloException, MissingMetadataException {

    VersionedTableMetadata metadata =
        new VersionedMetadataObject().allMetadataForTable(context, ds.getDataset(), ds.getTable());
    VersionedDataScanSpec versionedSpec = new VersionedDataScanSpec(metadata, ds);
    if (auths != null) {
      context.setAuthorizations(auths);
    }
    JavaRDD<DatawaveRowObject> rowObjects =
        io.rddFromAccumulo(new DatawaveRowObject().find(context, versionedSpec));
    List<DatawaveRowObject> firstRows = rowObjects.take(1);
    if (firstRows.size() < 1) {
      return null;
    }

    List<String> columnNames = metadata.sortedColumnNames();

    if (ds.getColumnOrder() != null) {
      // Make certain the provided ordering has the same columns as the metadata
      HashSet<String> metadataColumns = new HashSet<>(columnNames);
      HashSet<String> providedColumns = new HashSet<>(ds.getColumnOrder());

      // TODO
      // if (metadataColumns.equals(providedColumns)) {
      if (metadataColumns.contains(providedColumns)) {
        columnNames = ds.getColumnOrder();
      }
    }

    // This is just to make this lambda happy by have an effectively final variable
    // List<String> finalColumnNames = new ArrayList<>(columnNames);
    List<String> finalColumnNames = new ArrayList<>(ds.getColumnOrder());

    List<StructField> fields = new ArrayList<>();
    // for (String columnName : columnNames) {
    for (String columnName : ds.getColumnOrder()) {
      StructField field = DataTypes.createStructField(columnName, DataTypes.StringType, true);
      fields.add(field);
    }
    StructType schema = DataTypes.createStructType(fields);

    JavaRDD<Row> rows =
        rowObjects.map(
            rowObject -> {
              ArrayList<String> values = new ArrayList<>();
              for (String columnName : finalColumnNames) {
                values.add(rowObject.getRow().get(columnName));
              }
              return RowFactory.create(values.toArray());
            });

    return rowRddToExportDataframe(spark, rows, schema);
  }

  public static Dataset<Row> exportColumnCounts(
      DPSparkContext context, SparkSession spark, SparkAccumuloIO io, DataScanSpec ds)
      throws IOException, BasicAccumuloException {

    return exportColumnCounts(context, spark, io, ds, null);
  }

  public static Dataset<Row> exportColumnCounts(
      DPSparkContext context,
      SparkSession spark,
      SparkAccumuloIO io,
      DataScanSpec ds,
      Authorizations auths)
      throws IOException, BasicAccumuloException {

    VersionedMetadataObject columnMetadata =
        new VersionedMetadataObject()
            .fetchColumn(
                context,
                context.getCurrentMetadataVersion(),
                ds.getDataset(),
                ds.getTable(),
                ds.getColumn());

    if (auths != null) {
      context.setAuthorizations(auths);
    }

    JavaRDD<ColumnCountObject> columnCounts =
        io.rddFromAccumulo(new ColumnCountObject().fetchColumn(context, columnMetadata));

    List<ColumnCountObject> firstCounts = columnCounts.take(1);

    if (firstCounts.size() < 1) {
      return null;
    }

    List<StructField> fields = new ArrayList<>();
    fields.add(DataTypes.createStructField("value", DataTypes.StringType, true));
    fields.add(DataTypes.createStructField("count", DataTypes.LongType, true));
    StructType schema = DataTypes.createStructType(fields);

    JavaRDD<Row> rows =
        columnCounts.map(
            columnCount -> {
              return RowFactory.create(columnCount.value, columnCount.count);
            });

    return rowRddToExportDataframe(spark, rows, schema);
  }

  public static Dataset<Row> exportDataScanSpec(
      DPSparkContext context, SparkSession spark, SparkAccumuloIO io, DataScanSpec ds)
      throws IOException, BasicAccumuloException, MissingMetadataException {
    return exportDataScanSpec(context, spark, io, ds, null);
  }

  public static Dataset<Row> exportDataScanSpec(
      DPSparkContext context,
      SparkSession spark,
      SparkAccumuloIO io,
      DataScanSpec ds,
      Authorizations auths)
      throws IOException, BasicAccumuloException, MissingMetadataException {
    if (ds.getType() == Type.ROW) {
      return TableExporter.exportRows(context, spark, io, ds, auths);
    } else if (ds.getType() == Type.COLUMN_COUNT) {
      return TableExporter.exportColumnCounts(context, spark, io, ds, auths);
    } else {
      assert (false);
      return null;
    }
  }
}
