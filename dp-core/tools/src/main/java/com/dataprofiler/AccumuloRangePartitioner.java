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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.accumulo.core.data.Key;
import org.apache.hadoop.io.Text;
import org.apache.spark.Partitioner;

public class AccumuloRangePartitioner extends Partitioner implements Serializable {

  private static final long serialVersionUID = 1L;
  private final List<byte[]> splits;
  private final ByteComparator comparator;

  public AccumuloRangePartitioner(Collection<Text> listSplits) {
    splits = new ArrayList<>();
    listSplits.forEach(s -> splits.add(s.copyBytes()));

    comparator = new ByteComparator();
  }

  @Override
  public int numPartitions() {
    return splits.size() + 1;
  }

  @Override
  public int getPartition(Object o) {
    Key key = (Key) o;

    // Find the bin for the range and guarantee it is positive
    int index = Collections.binarySearch(splits, key.getRow().getBytes(), comparator);
    index = index < 0 ? (index + 1) * -1 : index;
    return index;
  }
}
