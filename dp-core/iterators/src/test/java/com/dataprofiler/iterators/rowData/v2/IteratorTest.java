package com.dataprofiler.iterators.rowData.v2;

/*-
 * 
 * dataprofiler-iterators
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

import static com.dataprofiler.iterators.rowData.v2.Common.LEX_LONG;
import static org.junit.Assert.*;

import com.dataprofiler.test.IntegrationTest;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import com.dataprofiler.querylang.json.Expressions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.iterators.SortedMapIterator;
import org.apache.hadoop.io.Text;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public class IteratorTest {

  Text SHARD_ROW = new Text("shard");
  Text INDEX_CITY_CF = new Text("I\u0000city");
  Text INDEX_NAME_CF = new Text("I\u0000name");
  Text DOC_CF = new Text("D");

  ImmutableSortedMap<Key, Value> data;
  ImmutableMap<String, Long> idIndex;

  public final Function<Entry<Key, Value>, Key> getKeys = e -> e.getKey();

  public List<Entry<Key, Value>> drain(SortedKeyValueIterator<Key, Value> itr) throws IOException {
    return drain(itr, Function.identity());
  }

  public <T> List<T> drain(SortedKeyValueIterator<Key, Value> itr,
      Function<Entry<Key, Value>, T> transform) throws IOException {
    List<T> sink = new ArrayList<>();
    while (itr.hasTop()) {
      Key k = itr.getTopKey();
      Value v = itr.getTopValue();
      sink.add(transform.apply(Maps.immutableEntry(k, v)));
      itr.next();
    }
    return sink;
  }

  @Before
  public void setup() {
    String shard = "shard";
    String indexPrefix = "I\u0000";
    String[] colNames = new String[]{"name", "city"};
    List<String> cities = ImmutableList.of(
        "Baltimore",
        "Washington DC",
        "Seattle",
        "Chicago",
        "Redwood City");
    List<String> names = ImmutableList.of(
        "Bill",
        "Stas",
        "Aaron",
        "Issa",
        "Mike");

    ImmutableSortedMap.Builder<Key, Value> builder = new ImmutableSortedMap.Builder(Comparator.naturalOrder());
    ImmutableMap.Builder<String, Long> idIndexBuilder = new Builder<>();
    {
      long id = 0;
      for (Iterator<String> a = cities.iterator(), b = names.iterator();
          a.hasNext() && b.hasNext(); ++id) {
        final byte[] docId = LEX_LONG.encode(id);
        String city = a.next();
        builder.put(new Key(SHARD_ROW, INDEX_CITY_CF, Common.join(city, docId, "string")),
            new Value());
        builder.put(new Key(SHARD_ROW, DOC_CF, Common.join(docId, "city")),
            new Value(city.getBytes()));
        idIndexBuilder.put(city, id);

        String name = b.next();
        builder.put(new Key(SHARD_ROW, INDEX_NAME_CF, Common.join(name, docId, "string")),
            new Value());
        builder.put(new Key(SHARD_ROW, DOC_CF, Common.join(docId, "name")),
            new Value(name.getBytes()));
        idIndexBuilder.put(name, id);


      }
      data = builder.build();
      idIndex = idIndexBuilder.build();
    }
  }

  @Test
  public void UnionMultipleDocs() throws Throwable {
    String query = "{'$or': ["
        + "{ '$eq': {"
        + "       'column': 'city',"
        + "       'value': 'Redwood City',"
        + "       'type': 'string'"
        + "     }"
        + "},"
        + "{ '$eq': {"
        + "       'column': 'name',"
        + "       'value': 'Bill',"
        + "       'type': 'string'"
        + "     }"
        + "}]}";

    assertNotNull(Expressions.parse(query));

    Map<String, String> config = ImmutableMap.of("query", query);
    RowAggregator queryIterator = new RowAggregator();
    queryIterator.init(new SortedMapIterator(data), config, null);
    queryIterator.seek(new Range("shard"), Collections.emptyList(), false);
    List<Key> keys = drain(queryIterator, getKeys);
    assertEquals(2, keys.size());
    // order dependant, but we know that the "Bill" doc comes before the "Redwood City" doc
    assertEquals(idIndex.get("Bill"), LEX_LONG.decode(keys.get(0).getColumnQualifier().getBytes()));
    assertEquals(idIndex.get("Redwood City"), LEX_LONG.decode(keys.get(1).getColumnQualifier().getBytes()));
  }

  @Test
  public void testTermIterator() throws Throwable {
    TermIterator ti = new TermIterator("city".getBytes(), "Baltimore".getBytes(), "string".getBytes(),
        new SortedMapIterator(data));
    ti.seek(new Range("shard"), new ArrayList<>(), false);
    List<Key> output = drain(ti, getKeys);
    assertEquals(1, output.size());
  }

  @Test
  public void testIntersection() throws Throwable {
    String query = "{'$and': ["
        + "{ '$eq': {"
        + "       'column': 'city',"
        + "       'value': 'Baltimore',"
        + "       'type': 'string'"
        + "     }"
        + "},"
        + "{ '$eq': {"
        + "       'column': 'name',"
        + "       'value': 'Bill',"
        + "       'type': 'string'"
        + "     }"
        + "}]}";

    assertNotNull(Expressions.parse(query));

    Map<String, String> config = ImmutableMap.of("query", query);
    RowAggregator queryIterator = new RowAggregator();
    queryIterator.init(new SortedMapIterator(data), config, null);
    queryIterator.seek(new Range("shard"), Collections.emptyList(), false);
    List<Key> keys = drain(queryIterator, getKeys);
    assertEquals(1, keys.size());
    assertEquals(idIndex.get("Bill"), LEX_LONG.decode(keys.get(0).getColumnQualifier().getBytes()));
    assertEquals(idIndex.get("Baltimore"), LEX_LONG.decode(keys.get(0).getColumnQualifier().getBytes()));
  }

  @Test
  public void testUnionSingleDoc() throws Throwable {
    String query = "{'$or': ["
        + "{ '$eq': {"
        + "       'column': 'city',"
        + "       'value': 'Baltimore',"
        + "       'type': 'string'"
        + "     }"
        + "},"
        + "{ '$eq': {"
        + "       'column': 'name',"
        + "       'value': 'Bill',"
        + "       'type': 'string'"
        + "     }"
        + "}]}";

    assertNotNull(Expressions.parse(query));

    Map<String, String> config = ImmutableMap.of("query", query);
    RowAggregator queryIterator = new RowAggregator();
    queryIterator.init(new SortedMapIterator(data), config, null);
    queryIterator.seek(new Range("shard"), Collections.emptyList(), false);
    List<Key> keys = drain(queryIterator, getKeys);
    assertEquals(1, keys.size());
    assertEquals(idIndex.get("Bill"), LEX_LONG.decode(keys.get(0).getColumnQualifier().getBytes()));
    assertEquals(idIndex.get("Baltimore"), LEX_LONG.decode(keys.get(0).getColumnQualifier().getBytes()));
  }

  @Test
  public void testUnionMultipleDocs() throws Throwable {
    String query = "{'$or': ["
        + "{ '$eq': {"
        + "       'column': 'city',"
        + "       'value': 'Redwood City',"
        + "       'type': 'string'"
        + "     }"
        + "},"
        + "{ '$eq': {"
        + "       'column': 'name',"
        + "       'value': 'Bill',"
        + "       'type': 'string'"
        + "     }"
        + "}]}";

    assertNotNull(Expressions.parse(query));

    Map<String, String> config = ImmutableMap.of("query", query);
    RowAggregator queryIterator = new RowAggregator();
    queryIterator.init(new SortedMapIterator(data), config, null);
    queryIterator.seek(new Range("shard"), Collections.emptyList(), false);
    List<Key> keys = drain(queryIterator, getKeys);
    assertEquals(2, keys.size());

    // We expect this to be the "Bill" doc because it's doc 0
    Key firstDoc = keys.get(0);
    assertEquals(idIndex.get("Bill"), LEX_LONG.decode(firstDoc.getColumnQualifier().getBytes()));

    Key secondDoc = keys.get(1);
    assertEquals(idIndex.get("Redwood City"), LEX_LONG.decode(secondDoc.getColumnQualifier().getBytes()));
  }

  @Test
  public void testNestedOperators() throws Throwable {
    String query = "{"
        + "  '$and': ["
        + "    {"
        + "      '$eq': {"
        + "        'column': 'name',"
        + "        'value': 'Bill',"
        + "        'type': 'string'"
        + "      }"
        + "    },"
        + "    {"
        + "      '$or': ["
        + "        {"
        + "          '$eq': {"
        + "            'column': 'city',"
        + "            'value': 'Redwood City',"
        + "            'type': 'string'"
        + "          }"
        + "        },"
        + "        {"
        + "          '$eq': {"
        + "            'column': 'city',"
        + "            'value': 'Baltimore',"
        + "            'type': 'string'"
        + "          }"
        + "        }"
        + "      ]"
        + "    }"
        + "  ]"
        + "}";

    assertNotNull(Expressions.parse(query));

    Map<String, String> config = ImmutableMap.of("query", query);
    RowAggregator queryIterator = new RowAggregator();
    queryIterator.init(new SortedMapIterator(data), config, null);
    queryIterator.seek(new Range("shard"), Collections.emptyList(), false);
    List<Key> keys = drain(queryIterator, getKeys);
    assertEquals(1, keys.size());

    // We expect this to be the "Bill" doc because he only appears in 'Baltimore'
    Key firstDoc = keys.get(0);
    assertEquals(idIndex.get("Bill"), LEX_LONG.decode(firstDoc.getColumnQualifier().getBytes()));
  }

  @Test
  public void testTransformValue() throws Throwable {
    byte[] bigOne = new String(new char[2048])
        .replace('\0', 'x')
        .getBytes(Charsets.UTF_8);
    Value v = new Value(bigOne, true /*copy*/);
    assertArrayEquals(new Value(v.get(), 0, 1024).get(),
        RowAggregator.transformValue(v).get());

    byte[] littleOne = "little str".getBytes(Charsets.UTF_8);
    assertArrayEquals(littleOne, RowAggregator.transformValue(new Value(littleOne)).get());
  }
}
