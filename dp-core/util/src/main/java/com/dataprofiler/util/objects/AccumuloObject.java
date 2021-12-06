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

import static java.lang.String.format;

import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Const;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.iterators.ClosableIterator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.beans.Transient;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.apache.accumulo.core.client.admin.TableOperations;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.hadoop.io.Text;
import org.apache.log4j.Logger;

public abstract class AccumuloObject<T extends AccumuloObject> implements Serializable {
  private static final Logger logger = Logger.getLogger(AccumuloObject.class);
  protected static ObjectMapper mapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  /*
   * This is the name of the configuration entry that holds the table name - NOT
   * the table name
   */
  protected transient String configEntryNameForAccumuloTable;
  protected String visibility = "";
  protected long timestamp = 0L;

  protected AccumuloObject() {}

  protected AccumuloObject(String configEntryNameforAccumuloTable) {
    this.configEntryNameForAccumuloTable = configEntryNameforAccumuloTable;
  }

  public static String joinKeyComponents(String... components) {
    return joinKeyComponents(false, components);
  }

  public static String joinKeyComponentsEndDelimited(String... components) {
    return joinKeyComponents(true, components);
  }

  public static String joinKeyComponents(Boolean endDelimited, String... components) {
    String base = Arrays.stream(components).collect(Collectors.joining(Const.DELIMITER));
    if (endDelimited) {
      base = base + Const.DELIMITER;
    }

    return base;
  }

  public static byte[] joinKeyComponents(Boolean endDelimited, byte[]... components) {
    byte[] delimiter = Const.DELIMITER.getBytes(StandardCharsets.UTF_8);

    int length = 0;
    for (byte[] component : components) {
      length += component.length;
    }

    length += delimiter.length * (components.length - 1);

    byte[] combined = new byte[length];

    int currPos = 0;
    for (int i = 0; i < components.length; i++) {
      System.arraycopy(components[i], 0, combined, currPos, components[i].length);
      currPos += components[i].length;

      if (i < components.length - 1) {
        System.arraycopy(delimiter, 0, combined, currPos, delimiter.length);
        currPos += delimiter.length;
      }
    }

    if (endDelimited) {
      System.arraycopy(delimiter, 0, combined, currPos, delimiter.length);
    }

    return combined;
  }

  public static byte[] joinKeyComponents(byte[]... components) {
    return joinKeyComponents(false, components);
  }

  public static byte[] joinKeyComponentsEndDelimited(byte[]... components) {
    return joinKeyComponents(true, components);
  }

  public static Range createExclusiveRange(Boolean endDelimited, String... components) {
    String base = joinKeyComponents(endDelimited, components);

    return new Range(base, base);
  }

  public static Range createExclusiveRange(String... components) {
    return createExclusiveRange(false, components);
  }

  public static Range createExclusiveRangeEndDelimited(String... components) {
    return createExclusiveRange(true, components);
  }

  public static Range createInclusiveRange(Boolean endDelimited, String... components) {
    String base = joinKeyComponents(endDelimited, components);

    return new Range(base, base + Const.HIGH_BYTE);
  }

  public static Range createInclusiveRange(String... components) {
    return createInclusiveRange(false, components);
  }

  public static Range createInclusiveRangeEndDelimited(String... components) {
    return createInclusiveRange(true, components);
  }

  /**
   * Create a new instance of this object from the provided Context and Entry. Can throw the runtime
   * exception of InvalidDataFormat.
   *
   * @param entry An entry from accumulo representing an object of this type
   * @return a newly created object populated with the data from the entry.
   */
  public abstract T fromEntry(Entry<Key, Value> entry);

  /**
   * Update all of the AccumuloObject properties from they key (such as visibilities). Most
   * implementations of fromEntry should call this.
   *
   * @param entry
   */
  public void updatePropertiesFromEntry(Entry<Key, Value> entry) {
    visibility = entry.getKey().getColumnVisibility().toString();
    timestamp = entry.getKey().getTimestamp();
  }

  public T fromKeyAndValue(Key key, Value value) {
    return fromEntry(new SimpleEntry<>(key, value));
  }

  @JsonIgnore
  @Transient
  public String getTable(Context context) {
    return context.getConfig().getAccumuloTableByName(configEntryNameForAccumuloTable);
  }

  public ObjectScannerIterable<T> scan(Context context) {
    return new ObjectScannerIterable<T>(context, getTable(context), (T) this);
  }

  public T fetch(Context context, Key key) {
    ObjectScannerIterable<T> scanner =
        scan(context)
            .addRange(createExclusiveRange(key.getRow().toString()))
            .fetchColumn(key.getColumnFamily().toString(), key.getColumnQualifier().toString());

    return fetch(scanner);
  }

  public T fetch(ObjectScannerIterable<T> scanner) {
    ClosableIterator<T> iterator = scanner.iterator();
    T t = null;
    if (iterator.hasNext()) {
      t = iterator.next();
    }
    try {
      iterator.close();
    } catch (Exception e) {
      throw new RuntimeException(e.toString());
    }

    return t;
  }

  public void put(Context context, BatchWriter writer, Key key, Value value)
      throws BasicAccumuloException {
    Mutation mut = createMutation(key, value);
    try {
      writer.addMutation(mut);
    } catch (MutationsRejectedException e) {
      throw new BasicAccumuloException(e.toString());
    }
  }

  public void put(Context context, BatchWriter writer) throws IOException, BasicAccumuloException {
    put(context, writer, createAccumuloKey(), createAccumuloValue());
  }

  public void put(Context context) throws IOException, BasicAccumuloException {
    BatchWriter writer = context.createBatchWriter(getTable(context));

    put(context, writer);

    try {
      writer.close();
    } catch (MutationsRejectedException e) {
      throw new BasicAccumuloException(e.toString());
    }
  }

  public void destroy(Context context) throws BasicAccumuloException {
    BatchWriter writer = context.createBatchWriter(getTable(context));
    destroy(context, writer);
    try {
      writer.close();
    } catch (MutationsRejectedException e) {
      throw new BasicAccumuloException(e.toString());
    }
  }

  public void destroy(Context context, BatchWriter writer) throws BasicAccumuloException {
    Mutation mut = createDeleteMutation();
    try {
      writer.addMutation(mut);
    } catch (MutationsRejectedException e) {
      throw new BasicAccumuloException(e.toString());
    }
  }

  public void bulkDeleteContainingTable(Context context) throws BasicAccumuloException {
    String currTable = getTable(context);

    try {
      TableOperations tops = context.getConnector().tableOperations();

      // Delete the previous backups
      for (String table : tops.list()) {
        if (table.startsWith(currTable + "_")) {
          tops.delete(table);
        }
      }

      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
      tops.rename(currTable, currTable + "_" + timestamp);
      tops.create(currTable);
      tops.removeConstraint(currTable, 1);
    } catch (Exception e) {
      throw new BasicAccumuloException(e.getMessage());
    }
  }

  public abstract Key createAccumuloKey() throws InvalidDataFormat;

  public Value createAccumuloValue() throws InvalidDataFormat, IOException {
    if (logger.isTraceEnabled()) {
      logger.trace(format("serializing %s", this));
    }
    return new Value(mapper.writeValueAsBytes(this));
  }

  public Mutation createMutation(Key key, Value value) {
    Mutation mut = new Mutation(key.getRow());
    mut.put(
        key.getColumnFamily(), key.getColumnQualifier(), key.getColumnVisibilityParsed(), value);

    return mut;
  }

  public Mutation createMutation() throws InvalidDataFormat, IOException {
    Key key = createAccumuloKey();
    Value value = createAccumuloValue();

    return createMutation(key, value);
  }

  public Mutation createDeleteMutation() {
    Key key = createAccumuloKey();
    Mutation mut = new Mutation(key.getRow());
    mut.putDelete(key.getColumnFamily(), key.getColumnQualifier(), key.getColumnVisibilityParsed());

    return mut;
  }

  public String[] splitString(String s, int expectedLen) {
    String[] parts = s.split(Const.DELIMITER);

    if (parts.length != expectedLen) {
      throw new InvalidDataFormat(
          format(
              "Key with wrong number of fields (expected: %d, got: %d): %s",
              expectedLen, parts.length, s));
    }

    return parts;
  }

  public List<Text> stringArrayToTextList(String[] strings) {
    List<Text> out = new ArrayList<>();
    Stream.of(strings).forEach(str -> out.add(new Text(str)));

    return out;
  }

  public Text[] stringArrayToTextArray(String[] strings) {
    return stringArrayToTextList(strings).toArray(new Text[0]);
  }

  @JsonIgnore
  @Transient
  public String getConfigEntryNameForAccumuloTable() {
    return configEntryNameForAccumuloTable;
  }

  @JsonIgnore
  @Transient
  public void setConfigEntryNameForAccumuloTable(String configEntryNameForAccumuloTable) {
    this.configEntryNameForAccumuloTable = configEntryNameForAccumuloTable;
  }

  public String getVisibility() {
    return visibility;
  }

  public void setVisibility(String visibility) {
    this.visibility = visibility;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }
}
