package com.dataprofiler.loader.datatypes;

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

import com.dataprofiler.DPSparkContext;
import com.dataprofiler.loader.config.JsonFileParams;
import com.dataprofiler.util.BasicAccumuloException;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.DataFrameReader;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.ArrayType;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import scala.collection.JavaConverters;
import scala.collection.Seq;

public class JsonLoader extends Loader {

  private final String EXPLODE_BLACKLIST_DELIM = ",";

  private final JsonFileParams fileParams;
  private List<String> columnsExcludedFromExplode;

  public JsonLoader(
      DPSparkContext context,
      SparkSession spark,
      String datasetName,
      String tableName,
      String visibility,
      String columnVisibilityExpression,
      String rowVisibilityColumnName,
      JsonFileParams fileParams)
      throws BasicAccumuloException {
    super(
        context,
        spark,
        datasetName,
        tableName,
        visibility,
        columnVisibilityExpression,
        rowVisibilityColumnName);
    this.fileParams = fileParams;
  }

  public boolean load() throws Exception {

    if (!this.inputContainsFiles(fileParams.getInputFilename())) {
      throw new LoaderException(
          String.format("Input '%s' contains no files", fileParams.getInputFilename()));
    }

    DataFrameReader reader = getSpark().read();
    Dataset<Row> dataset =
        reader.option("multiLine", fileParams.getMultiLine()).json(fileParams.getInputFilename());

    dataset.printSchema();

    if (StringUtils.isNotBlank(fileParams.getExplodeBlacklist())) {
      columnsExcludedFromExplode =
          new ArrayList<>(
              ImmutableList.copyOf(
                  fileParams.getExplodeBlacklist().split(EXPLODE_BLACKLIST_DELIM)));
    }

    origTable = flattenJsonDf(dataset, columnsExcludedFromExplode);
    return super.load();
  }

  private static Dataset<Row> flattenJsonDf(Dataset<Row> ds, List<String> excludedColumns) {
    StructField[] fields = ds.schema().fields();

    List<String> fieldNames = new ArrayList<>();
    for (StructField s : fields) {
      fieldNames.add(s.name());
    }

    for (int i = 0; i < fields.length; i++) {
      StructField field = fields[i];
      DataType fieldType = field.dataType();
      String fieldName = field.name();

      if (fieldType instanceof ArrayType) {

        List<String> fieldNamesExcludingArray = new ArrayList<String>();
        for (String fieldNameIndex : fieldNames) {
          if (!fieldName.equals(fieldNameIndex)) {
            fieldNamesExcludingArray.add(fieldNameIndex);
          }
        }

        List<String> fieldNamesAndExplode = new ArrayList<>(fieldNamesExcludingArray);

        if (excludedColumns != null && excludedColumns.stream().anyMatch(fieldName::contains)) {
          String s =
              String.format(
                  "if ( size(%s) == 0, null, to_json(struct(%s))) as %s",
                  fieldName, fieldName, fieldName);
          fieldNamesAndExplode.add(s);

        } else {
          String s = String.format("explode_outer(%s) as %s", fieldName, fieldName);
          fieldNamesAndExplode.add(s);
        }

        String[] exFieldsWithArray = new String[fieldNamesAndExplode.size()];
        Dataset explodedDataset = ds.selectExpr(fieldNamesAndExplode.toArray(exFieldsWithArray));
        explodedDataset.show();
        return flattenJsonDf(explodedDataset, excludedColumns);

      } else if (fieldType instanceof StructType) {

        String[] childFieldNamesStruct = ((StructType) fieldType).fieldNames();
        List<String> childFieldNames = new ArrayList<>();
        for (String childName : childFieldNamesStruct) {
          childFieldNames.add(fieldName + "." + childName);
        }
        List<String> newFieldNames = new ArrayList<>();
        for (String fieldNameIndex : fieldNames) {
          if (!fieldName.equals(fieldNameIndex)) newFieldNames.add(fieldNameIndex);
        }
        newFieldNames.addAll(childFieldNames);
        List<Column> renamedStructCols = new ArrayList<>();
        for (String newFieldNameIndex : newFieldNames) {
          renamedStructCols.add(
              new Column(newFieldNameIndex).as(newFieldNameIndex.replace(".", "_")));
        }

        Seq renamedStructColsSeq =
            JavaConverters.collectionAsScalaIterableConverter(renamedStructCols).asScala().toSeq();
        Dataset dsStruct = ds.select(renamedStructColsSeq);

        System.out.println(
            "STRUCT_TYPE: " + dsStruct.count() + "  columns: " + dsStruct.schema().fields().length);
        dsStruct.show();

        return flattenJsonDf(dsStruct, excludedColumns);

      } else {
        // common struct type
      }
    }

    return ds;
  }
}
