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

import static java.lang.String.format;
import static java.util.Objects.isNull;

import java.math.BigDecimal;
import java.text.ChoiceFormat;
import java.util.function.BiFunction;

public class NumberUtil {
  public static final long ZERO = 0L;
  public static final long THOUSAND = 1_000L;
  public static final long MILLION = 1_000_000L;
  public static final long BILLION = 1_000_000_000L;
  public static final long TRILLION = 1_000_000_000_000L;
  public static final long QUADRILLION = 1_000_000_000_000_000L;
  public static final long QUINTILLION = 1_000_000_000_000_000_000L;

  /**
   * @param num
   * @throws IllegalArgumentException if supplied number is null
   * @return
   */
  public static String humanReadableValue(Number num) {
    if (isNull(num)) {
      throw new IllegalArgumentException("supplied number was null");
    }

    double[] numLimits = {ZERO, THOUSAND, MILLION, BILLION, TRILLION, QUADRILLION, QUINTILLION};
    String[] suffixes = {"", "K", "M", "B", "T", "QD", "QN"};
    ChoiceFormat suffixFormatter = new ChoiceFormat(numLimits, suffixes);
    BiFunction<Long, Long, String> formatNumber =
        (Long value, Long floor) -> {
          String truncated = truncateNumber(value, floor);
          String suffix = suffixFormatter.format(value);
          return format("%s%s", truncated, suffix);
        };

    long number = num.longValue();
    if (number == ZERO) {
      return "0";
    } else if (number < THOUSAND) {
      return format("%s", number);
    } else if (number > THOUSAND && number < MILLION) {
      return formatNumber.apply(number, THOUSAND);
    } else if (number > MILLION && number < BILLION) {
      return formatNumber.apply(number, MILLION);
    } else if (number > BILLION && number < TRILLION) {
      return formatNumber.apply(number, BILLION);
    } else if (number > TRILLION && number < QUADRILLION) {
      return formatNumber.apply(number, TRILLION);
    } else if (number > QUADRILLION && number < QUINTILLION) {
      return formatNumber.apply(number, QUADRILLION);
    }
    return format("%s", number);
  }

  public static String truncateNumber(long num, long divisor) {
    double wholeNumber = (double) (num / divisor);
    long truncatedWholeNumber = BigDecimal.valueOf(wholeNumber).setScale(0).longValue();
    long fractionNumber = BigDecimal.valueOf((double) (num % divisor) * 10).setScale(0).longValue();
    return format("%s.%.2s", truncatedWholeNumber, fractionNumber);
  }
}
