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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.UUID;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;

public class ColumnEntityObject extends AccumuloObject<ColumnEntityObject> {

  private String id;
  private String title;
  private ArrayList<String> column_names;
  private String created_by;
  private String grouping_logic;
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final TypeReference columnsTypeRef = new TypeReference<ArrayList<String>>() {};
  private static final ObjectReader columnsJsonReader = mapper.readerFor(columnsTypeRef);

  public ColumnEntityObject() {
    super(Const.ACCUMULO_COLUMN_ENTITIES_ENV_KEY);
  }

  public static ObjectScannerIterable<ColumnEntityObject> list(Context context) {
    ColumnEntityObject ceo = new ColumnEntityObject();
    return ceo.scan(context);
  }

  public ColumnEntityObject(
      String created_by, String title, ArrayList<String> column_names, String grouping_logic) {
    super(Const.ACCUMULO_COLUMN_ENTITIES_ENV_KEY);
    this.created_by = created_by;
    this.title = title;
    this.column_names = column_names;
    this.grouping_logic = grouping_logic;
    this.id = UUID.randomUUID().toString();
  }

  @Override
  public ColumnEntityObject fromEntry(Entry<Key, Value> entry) {
    ColumnEntityObject ceo = new ColumnEntityObject();

    try {
      JsonNode val = mapper.readValue(entry.getValue().toString(), JsonNode.class);
      ceo.id = entry.getKey().getRow().toString();
      ceo.title = val.get("title").asText();
      ceo.created_by = val.get("created_by").asText();
      ceo.grouping_logic = val.get("grouping_logic").asText();
      ceo.column_names = columnsJsonReader.readValue(val.get("column_names"));
      ceo.updatePropertiesFromEntry(entry);
    } catch (IOException e) {
      throw new InvalidDataFormat(e.toString());
    }

    return ceo;
  }

  @Override
  public Key createAccumuloKey() throws InvalidDataFormat {
    String key = this.id;
    return new Key(key);
  }

  @Override
  public Value createAccumuloValue() throws InvalidDataFormat, IOException {
    ObjectNode ret = mapper.createObjectNode();
    ret.put("title", this.title);
    ret.put("created_by", this.created_by);
    ret.put("grouping_logic", this.grouping_logic);
    JsonNode columnNames = mapper.convertValue(this.column_names, JsonNode.class);
    ret.set("column_names", columnNames);
    return new Value(mapper.writeValueAsString(ret).getBytes());
  }

  public static ColumnEntityObject fetchById(Context context, String id) {
    ColumnEntityObject ceo = new ColumnEntityObject();
    ceo.id = id;
    return ceo.fetch(context, ceo.createAccumuloKey());
  }

  public static Boolean deleteById(Context context, String id) throws BasicAccumuloException {
    ColumnEntityObject ceo = fetchById(context, id);
    ceo.destroy(context);
    return true;
  }

  public String getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getCreatedBy() {
    return created_by;
  }

  public void setCreatedBy(String created_by) {
    this.created_by = created_by;
  }

  public ArrayList<String> getColumnNames() {
    return column_names;
  }

  public void setColumnNames(ArrayList<String> columnNames) {
    this.column_names = columnNames;
  }

  public String getGroupingLogic() {
    return grouping_logic;
  }

  public void setGroupingLogic(String grouping_logic) {
    this.grouping_logic = grouping_logic;
  }

  @Override
  public String toString() {
    return "ColumnEntityObject{"
        + "id='"
        + id
        + '\''
        + ", title='"
        + title
        + '\''
        + ", created_by='"
        + created_by
        + '\''
        + ", grouping_logic='"
        + grouping_logic
        + '\''
        + ", column_names='"
        + column_names.toString()
        + '\''
        + '}';
  }
}
