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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.TreeSet;

import org.junit.Test;

public class DatawaveRowObjectTest {
    @Test
    public void testCreatePaginationBounds() {
        TreeSet<DatawaveRowShardIndexObject> shardIndices = new TreeSet<>(DatawaveRowShardIndexObject.comparator());
        for (int i = 0; i < 10; ++i) {
            shardIndices.add(new DatawaveRowShardIndexObject(i, "dataset_id", "table_id", i * 100));
        }

        Collection<DatawaveRowShardIndexObject> choppedBounds =
                DatawaveRowObject.createPaginationBounds(5, 545, new DataScanSpec("dataset_id", "table_id"), shardIndices);
        // we should have 5 ranges
        assertEquals(5, choppedBounds.size());
        // no shard should be less than 5
        choppedBounds.forEach(shart -> assertTrue(shart.getShard() >= 5));
        // the first shard should start on record 545. TermIterator handles
        // making that exclusive, so we don't necessarily care about that
        // here
        assertEquals(545, choppedBounds.iterator().next().getRowIdx());
    }
}
