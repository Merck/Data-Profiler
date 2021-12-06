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
import org.apache.hadoop.io.Text;
import org.apache.log4j.Logger;

/***
 * This interface is for objects that allow bulk purging of versioned
 * datasets and tables. It's *really* focused on purging datasets by
 * table id. When you purge a dataset, it takes a single version, but
 * what happens is that it simply looks up all of the tables in that
 * version of the metadata and purges those (which is the only thing
 * that we can really do efficiently).
 *
 */
public abstract class VersionedPurgableDatasetObject<T extends AccumuloObject>
    extends AccumuloObject<T> {

  private static final Logger logger = Logger.getLogger(PurgableDatasetObject.class);

  protected VersionedPurgableDatasetObject(String configEntryNameforAccumuloTable) {
    super(configEntryNameforAccumuloTable);
  }

  public void bulkPurgeDataset(Context context, MetadataVersionObject version, String dataset)
      throws BasicAccumuloException {
    // For versioned data we can't delete an entire dataset at a time using table
    // operations (for most objects) because the keys aren't contiguous for an
    // entire dataset. We have to, instead, delete
    // them table by table. It's still faster to do this with table operations for most cases, so
    // the default implementation has this datset level operation lookup the table level
    // information and call the table level delete in a loop.
    VersionedDatasetMetadata d =
        new VersionedMetadataObject().allMetadataForDataset(context, version, dataset);
    for (VersionedMetadataObject table : d.getTableMetadata().values()) {
      bulkPurgeTable(context, table.dataset_name, table.table_name, table.table_id);
    }
  }

  public abstract void bulkPurgeTable(
      Context context, String datasetName, String tableName, String tableId)
      throws BasicAccumuloException;

  protected static String prevVal(String val) {
    return new String(prevVal(val.getBytes()));
  }

  protected static byte[] prevVal(byte[] val) {
    val[val.length - 1]--;
    return val;
  }

  protected void bulkPurgeRange(Context context, Text start, Text end)
      throws BasicAccumuloException {
    String accumuloTable = getTable(context);

    logger.warn(
        String.format("Deleting range ('%s', '%s'] from table %s", start, end, accumuloTable));
    try {
      context.getConnector().tableOperations().deleteRows(accumuloTable, start, end);
    } catch (Exception e) {
      throw new BasicAccumuloException(e.getMessage());
    }
  }
}
