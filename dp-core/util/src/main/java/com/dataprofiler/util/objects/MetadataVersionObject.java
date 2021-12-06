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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;

/**
 * MetadataVersionObject represents a version of the metadata for all of our data. It simply holds
 * the current version and a pointer to the previous version of the metadata.
 *
 * <p>At any given time there is a single entry that holds the current version of the metadata
 * stored with a well known key.
 */
public class MetadataVersionObject extends AccumuloObject<MetadataVersionObject> {
  public String id;
  public String previousId;
  public long creationTime;

  private static final String accumuloTable = Const.ACCUMULO_METADATA_TABLE_ENV_KEY;
  private static final TypeReference<MetadataVersionObject> staticTypeReference =
      new TypeReference<MetadataVersionObject>() {};

  public MetadataVersionObject() {
    super(accumuloTable);
  }

  public MetadataVersionObject(String id) {
    this();
    this.id = id;
  }

  public MetadataVersionObject(String id, String previousId) {
    this();
    this.id = id;
    this.previousId = previousId;
  }

  public MetadataVersionObject(String id, MetadataVersionObject previous) {
    this(id, previous.getId());
  }

  public static MetadataVersionObject newRandomMetadataVersion() {
    return new MetadataVersionObject(UUID.randomUUID().toString());
  }

  @Override
  public ObjectScannerIterable<MetadataVersionObject> scan(Context context) {
    return super.scan(context).fetchColumnFamily(Const.COL_FAM_METADATA_VERSION);
  }

  @Override
  public MetadataVersionObject fromEntry(Entry<Key, Value> entry) {
    MetadataVersionObject m = null;
    try {
      m = mapper.readValue(entry.getValue().get(), staticTypeReference);
    } catch (IOException e) {
      throw new InvalidDataFormat(e.toString());
    }

    return m;
  }

  public static Key createVersionKey(String version) {
    return new Key(version, Const.COL_FAM_METADATA_VERSION);
  }

  public Key createCurrentVersionKey() {
    return new Key(Const.CURRENT_METADATA_VERSION_KEY, Const.COL_FAM_METADATA_CURRENT_VERSON);
  }

  /**
   * Set this version as the current default version.
   *
   * @param context
   * @throws BasicAccumuloException
   * @throws IOException
   */
  public void putAsCurrentVersion(Context context) throws BasicAccumuloException, IOException {
    BatchWriter writer = context.createBatchWriter(getTable(context));
    put(context, writer, createCurrentVersionKey(), createAccumuloValue());
    try {
      writer.close();
    } catch (MutationsRejectedException e) {
      throw new BasicAccumuloException(e.toString());
    }
  }

  public void putAsCurrentVersion(Context context, BatchWriter writer)
      throws IOException, BasicAccumuloException {
    Key key = createCurrentVersionKey();
    Value value = createAccumuloValue();
    put(context, writer, key, value);
  }

  public MetadataVersionObject fetchCurrentVersion(Context context) {
    Key key = createCurrentVersionKey();
    return fetch(context, key);
  }

  public MetadataVersionObject fetchVersion(Context context, String version) {
    Key key = createVersionKey(version);
    return fetch(context, key);
  }

  /***
   * Return all of the metadata versions in order newest to oldest.
   * @param context
   * @return list of metadata version newest to oldest.
   */
  public List<MetadataVersionObject> allMetadataVersions(Context context) {
    HashMap<String, MetadataVersionObject> versionsMap = new HashMap<>();

    // Sorting these won't work but we still want to read them all from accumulo at
    // once, so store them in a map and then we can recreate the list efficiently from
    // that.
    for (MetadataVersionObject v : scan(context).setBatch(true)) {
      versionsMap.put(v.id, v);
    }

    MetadataVersionObject current = fetchCurrentVersion(context);

    ArrayList<MetadataVersionObject> versions = new ArrayList<>();

    while (current != null) {
      versions.add(current);
      if (current.previousId == null) {
        current = null;
      } else {
        assert (versionsMap.containsKey(current.previousId));
        if (current.id.equals(current.previousId)) {
          // Some bug on development was causing this to happen, so just ignore.
          System.err.println("ERROR: current metadata equals previous: ignoring");
          current = null;
        } else {
          current = versionsMap.get(current.previousId);
        }
      }
    }

    return versions;
  }

  public static <T> List<T> sortByVersion(
      Map<String, T> versionedData, List<MetadataVersionObject> allVersions) {
    ArrayList<T> out = new ArrayList<>();
    for (MetadataVersionObject version : allVersions) {
      if (versionedData.containsKey(version.id)) {
        out.add(versionedData.get(version.id));
      }
    }
    return out;
  }

  @Override
  public Key createAccumuloKey() throws InvalidDataFormat {
    return new Key(id, Const.COL_FAM_METADATA_VERSION);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getPreviousId() {
    return previousId;
  }

  public void setPreviousId(String previousId) {
    this.previousId = previousId;
  }

  public void setPrevious(MetadataVersionObject previous) {
    if (previous == null) {
      return;
    }
    this.previousId = previous.getId();
  }

  public void updateCreationTime() {
    this.creationTime = System.currentTimeMillis();
  }

  public long getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(long creationTime) {
    this.creationTime = creationTime;
  }

  @Override
  public String toString() {
    return "MetadataVersionObject{"
        + "key="
        + createAccumuloKey().toString()
        + "id='"
        + id
        + '\''
        + ", previousId='"
        + previousId
        + '\''
        + ", creationTime="
        + creationTime
        + '}';
  }
}
