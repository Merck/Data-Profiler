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

import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Const;
import com.dataprofiler.util.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Map.Entry;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;

public class ElementAlias extends PurgableDatasetObject<ElementAlias> {

  private AliasType aliasType;
  private String dataset;
  private String table;
  private String column;
  private String alias;
  private String createdBy;

  public enum AliasType {
    COLUMN,
    TABLE,
    DATASET,
    CONCEPT
  }

  public ElementAlias() {
    super(Const.ACCUMULO_ELEMENT_ALIAS_ENV_KEY);
  }

  public static ObjectScannerIterable<ElementAlias> list(Context context) {
    ElementAlias ea = new ElementAlias();
    return ea.scan(context);
  }

  public ElementAlias(
      String aliasType,
      String dataset,
      String table,
      String column,
      String alias,
      String createdBy) {
    this();
    this.aliasType = AliasType.valueOf(aliasType.toUpperCase().trim());
    this.dataset = dataset;
    this.table = table;
    this.column = column;
    this.alias = alias;
    this.createdBy = createdBy;
  }

  @Override
  public ElementAlias fromEntry(Entry<Key, Value> entry) throws InvalidDataFormat {
    ElementAlias ea = new ElementAlias();
    try {
      JsonNode val = mapper.readValue(entry.getValue().toString(), JsonNode.class);
      String key = entry.getKey().getRow().toString();
      String colFam = entry.getKey().getColumnFamily().toString();
      ea.aliasType = AliasType.valueOf(colFam.toUpperCase().trim());
      ea.alias = val.get("alias").asText();
      ea.createdBy = val.get("createdBy").asText();

      switch (ea.aliasType) {
        case COLUMN:
          String[] colKeySplit = splitString(key, 3);
          ea.dataset = colKeySplit[0];
          ea.table = colKeySplit[1];
          ea.column = colKeySplit[2];
          break;
        case TABLE:
          String[] tableKeySplit = splitString(key, 2);
          ea.dataset = tableKeySplit[0];
          ea.table = tableKeySplit[1];
          break;
        case DATASET:
          String[] datasetKeySplit = splitString(key, 1);
          ea.dataset = datasetKeySplit[0];
          break;
        default:
          throw new InvalidDataFormat("Cannot create object fromEntry for ElementAlias");
      }
    } catch (IOException e) {
      throw new InvalidDataFormat(e.toString());
    }

    ea.updatePropertiesFromEntry(entry);

    return ea;
  }

  @Override
  public Key createAccumuloKey() throws InvalidDataFormat {
    switch (this.aliasType) {
      case COLUMN:
        return new Key(
            joinKeyComponents(this.dataset, this.table, this.column), this.aliasType.toString());
      case TABLE:
        return new Key(joinKeyComponents(this.dataset, this.table), this.aliasType.toString());
      case DATASET:
        return new Key(this.dataset, this.aliasType.toString());
      default:
        throw new InvalidDataFormat("Cannot create accumulo key for ElementAlias");
    }
  }

  @Override
  public Value createAccumuloValue() throws InvalidDataFormat, IOException {
    ObjectNode ret = mapper.createObjectNode();
    ret.put("alias", this.alias);
    ret.put("createdBy", this.createdBy);
    return new Value(mapper.writeValueAsString(ret).getBytes());
  }

  public ObjectScannerIterable<ElementAlias> scanColumns(Context context) {
    return super.scan(context).fetchColumnFamily(AliasType.COLUMN.toString());
  }

  public ObjectScannerIterable<ElementAlias> scanTables(Context context) {
    return super.scan(context).fetchColumnFamily(AliasType.TABLE.toString());
  }

  public ObjectScannerIterable<ElementAlias> scanDatasets(Context context) {
    return super.scan(context).fetchColumnFamily(AliasType.DATASET.toString());
  }

  public static ElementAlias fetchColumnAlias(
      Context context, String dataset, String table, String column) {
    ElementAlias ea = new ElementAlias();
    ea.aliasType = AliasType.COLUMN;
    ea.dataset = dataset;
    ea.table = table;
    ea.column = column;
    return ea.fetch(context, ea.createAccumuloKey());
  }

  public static ElementAlias fetchTableAlias(Context context, String dataset, String table) {
    ElementAlias ea = new ElementAlias();
    ea.aliasType = AliasType.TABLE;
    ea.dataset = dataset;
    ea.table = table;
    return ea.fetch(context, ea.createAccumuloKey());
  }

  public static ElementAlias fetchDatasetAlias(Context context, String dataset) {
    ElementAlias ea = new ElementAlias();
    ea.aliasType = AliasType.DATASET;
    ea.dataset = dataset;
    return ea.fetch(context, ea.createAccumuloKey());
  }

  public static Boolean deleteColumnAlias(
      Context context, String dataset, String table, String column) throws BasicAccumuloException {
    ElementAlias ea = fetchColumnAlias(context, dataset, table, column);
    ea.destroy(context);
    return true;
  }

  public static Boolean deleteTableAlias(Context context, String dataset, String table)
      throws BasicAccumuloException {
    ElementAlias ea = fetchTableAlias(context, dataset, table);
    ea.destroy(context);
    return true;
  }

  public static Boolean deleteDatasetAlias(Context context, String dataset)
      throws BasicAccumuloException {
    ElementAlias ea = fetchDatasetAlias(context, dataset);
    ea.destroy(context);
    return true;
  }

  public AliasType getAliasType() {
    return aliasType;
  }

  public void setAliasType(AliasType aliasType) {
    this.aliasType = aliasType;
  }

  public String getDataset() {
    return dataset;
  }

  public void setDataset(String dataset) {
    this.dataset = dataset;
  }

  public String getTable() {
    return table;
  }

  public void setTable(String table) {
    this.table = table;
  }

  public String getColumn() {
    return column;
  }

  public void setColumn(String column) {
    this.column = column;
  }

  public String getAlias() {
    return alias;
  }

  public void setAlias(String alias) {
    this.alias = alias;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  @Override
  public String toString() {
    return "ElementAlias{"
        + "aliasType="
        + aliasType
        + ", dataset='"
        + dataset
        + '\''
        + ", table='"
        + table
        + '\''
        + ", column='"
        + column
        + '\''
        + ", alias='"
        + alias
        + '\''
        + ", createdBy='"
        + createdBy
        + '\''
        + '}';
  }
}
