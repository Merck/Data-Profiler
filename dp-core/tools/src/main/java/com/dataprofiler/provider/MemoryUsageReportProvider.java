package com.dataprofiler.provider;

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

import static java.lang.String.format;

public class MemoryUsageReportProvider {

  public static String memoryUsage() {
    double totalKb = Runtime.getRuntime().totalMemory() / 1024;
    double freeKb = Runtime.getRuntime().freeMemory() / 1024;
    double usageKb = totalKb - freeKb;
    double totalMb = totalKb / 1024;
    double freeMb = freeKb / 1024;
    double usageMb = usageKb / 1024;
    return format(
        "Memory used: %,.2f mb, total: %,.2f mb, free: %,.2f mb", usageMb, totalMb, freeMb);
  }
}
