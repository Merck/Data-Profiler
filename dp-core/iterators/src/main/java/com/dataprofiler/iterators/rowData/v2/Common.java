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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Comparator;
import org.apache.accumulo.core.client.lexicoder.LongLexicoder;
import org.apache.accumulo.core.data.Value;
import org.apache.hadoop.io.Text;

public class Common {
  static final byte[] ZERO = new byte[] {0};
  static final byte[] TWOFIDDYSIX = new byte[] {(byte) 0xff};
  static final byte[] MIN_DOC_ID = new LongLexicoder().encode(Long.MIN_VALUE);
  static final byte[] MAX_DOC_ID = new LongLexicoder().encode(Long.MAX_VALUE);

  static final Text DOC_ID_COLUMN_FAMILY = new Text("D");

  static final Value EMPTY = new Value(new byte[0]);

  static final Comparator<IndexIterator> MAX_SORT =
      (a, b) -> -1 * a.getTopKey().compareTo(b.getTopKey());

  static final Text DELIMITER = new Text(ZERO);

  static final LongLexicoder LEX_LONG = new LongLexicoder();

  public static Text join(Object... objs) {
    Text t = new Text();
    for (int i = 0; i < objs.length; ++i) {
      Object obj = objs[i];
      if (obj instanceof Text) {
        Text text = (Text) obj;
        t.append(text.getBytes(), 0, text.getLength());
      } else if (obj instanceof byte[]) {
        byte[] bytes = (byte[]) obj;
        t.append(bytes, 0, bytes.length);
      } else {
        byte[] bytes = obj.toString().getBytes(UTF_8);
        t.append(bytes, 0, bytes.length);
      }

      if (i < objs.length - 1) {
        t.append(Common.ZERO, 0, Common.ZERO.length);
      }
    }
    return t;
  }
}
