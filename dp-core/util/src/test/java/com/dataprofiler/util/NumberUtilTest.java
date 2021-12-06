package com.dataprofiler.util;

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

import org.junit.Before;
import org.junit.Test;

import static com.dataprofiler.util.NumberUtil.humanReadableValue;
import static org.junit.Assert.assertEquals;

public class NumberUtilTest {

  @Before
  public void setup() {}

  @Test(expected = IllegalArgumentException.class)
  public void testFormatNull() {
    String format1 = humanReadableValue(null);
    assertEquals(format1, "0");
  }

  @Test
  public void testFormatZero() {
    String format1 = humanReadableValue(0L);
    assertEquals("0", format1);
  }

  @Test
  public void testFormatSmallNumber() {
    String format1 = humanReadableValue(12L);
    assertEquals("12", format1);
  }

  @Test
  public void testFormatThousandNumber() {
    String format1 = humanReadableValue(1_203L);
    assertEquals("1.20K", format1);
  }

  @Test
  public void testFormatMillionNumber() {
    String format1 = humanReadableValue(5_340_001L);
    assertEquals("5.34M", format1);
  }

  @Test
  public void testFormatBillionNumber() {
    String format1 = humanReadableValue(5_340_001_000L);
    assertEquals("5.34B", format1);
  }

  @Test
  public void testFormatTrillionNumber() {
    String format1 = humanReadableValue(5_340_001_000_000L);
    assertEquals("5.34T", format1);
  }

  @Test
  public void testFormatQuadrillionNumber() {
    String format1 = humanReadableValue(5_340_001_000_000_123L);
    assertEquals("5.34QD", format1);
  }
}
