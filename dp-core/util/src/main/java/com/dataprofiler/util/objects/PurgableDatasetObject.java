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
import org.apache.hadoop.io.Text;
import org.apache.log4j.Logger;

/***
 * This interface is for purging data objects that are not versioned (that is, they are stored
 * separated from specific versions of the data.)
 */
public abstract class PurgableDatasetObject<T extends AccumuloObject> extends AccumuloObject<T> {

  private static final Logger logger = Logger.getLogger(PurgableDatasetObject.class);

  protected PurgableDatasetObject(String configEntryNameforAccumuloTable) {
    super(configEntryNameforAccumuloTable);
  }

  protected static String prevVal(String val) {
    return new String(prevVal(val.getBytes()));
  }

  protected static byte[] prevVal(byte[] val) {
    val[val.length - 1]--;
    return val;
  }

  public void bulkPurgeDataset(Context context, String dataset) throws BasicAccumuloException {
    Text start = new Text(dataset + Const.DELIMITER);
    Text end = new Text(dataset + Const.DELIMITER + Const.HIGH_BYTE);
    bulkPurgeRange(context, start, end);
  }

  public void bulkPurgeTable(Context context, String dataset, String table)
      throws BasicAccumuloException {
    Text start = new Text(joinKeyComponentsEndDelimited(dataset, table));
    Text end = new Text(joinKeyComponentsEndDelimited(dataset, table) + Const.HIGH_BYTE);
    bulkPurgeRange(context, start, end);
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
