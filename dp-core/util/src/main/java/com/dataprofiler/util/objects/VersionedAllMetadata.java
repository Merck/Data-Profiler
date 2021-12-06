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
import com.dataprofiler.util.Context;
import java.io.IOException;
import java.util.HashMap;
import org.apache.accumulo.core.client.BatchWriter;

public class VersionedAllMetadata {
  public MetadataVersionObject version;
  // Metadata keyed by dataset
  public HashMap<String, VersionedDatasetMetadata> metadata = new HashMap<>();

  public VersionedAllMetadata(MetadataVersionObject version) {
    this.version = version;
  }

  public void put(VersionedMetadataObject m) {
    VersionedDatasetMetadata c;
    if (!metadata.containsKey(m.dataset_name)) {
      c = new VersionedDatasetMetadata(version);
      metadata.put(m.dataset_name, c);
    } else {
      c = metadata.get(m.dataset_name);
    }
    c.putMetadata(m);
  }

  private void populate(Context context, MetadataVersionObject version) {
    this.version = version;
    for (VersionedMetadataObject m :
        new VersionedMetadataObject().scan(context, version).setBatch(true)) {
      put(m);
    }
  }

  public void checkMetadataConsistency() throws Exception {
    for (VersionedDatasetMetadata d : metadata.values()) {
      d.checkMetadataConsistency();
    }
  }

  public VersionedAllMetadata(Context context, MetadataVersionObject version) {
    if (version == null) {
      return;
    }
    populate(context, version);
  }

  public VersionedAllMetadata(Context context) {
    version = new MetadataVersionObject().fetchCurrentVersion(context);
    if (version == null) {
      return;
    }
    populate(context, version);
  }

  public void putAll(Context context, BatchWriter writer)
      throws IOException, BasicAccumuloException {
    for (VersionedDatasetMetadata value : metadata.values()) {
      value.putAll(context, writer);
    }
  }

  public boolean isEmpty() {
    return metadata.isEmpty();
  }
}
