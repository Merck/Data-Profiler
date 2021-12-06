package com.dataprofiler.loader.config;

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

import java.util.Iterator;

@Deprecated
public class ParquetLoaderConfig extends LoaderConfig {

  public static String paramDesc = "(<file_name> [<schemaFile>])";

  public IHateWeirdBugs fileParams() {
    ParquetLoaderConfig.IHateWeirdBugs b = new ParquetLoaderConfig.IHateWeirdBugs();
    b.config = this;
    return b;
  }

  public class ParquetFileParamsIterator implements Iterator<ParquetFileParams> {
    private final ParquetLoaderConfig config;
    private int currIndex;
    private final int stride;

    public ParquetFileParamsIterator(ParquetLoaderConfig config, int startIndex) {
      this.config = config;

      if (config.separateTableSchema) {
        stride = 2;
      } else {
        stride = 1;
      }
      currIndex = startIndex;
    }

    @Override
    public boolean hasNext() {
      return currIndex + stride - 1 < config.parameters.size();
    }

    @Override
    public ParquetFileParams next() {
      ParquetFileParams p = new ParquetFileParams();
      p.setInputFilename(config.parameters.get(currIndex));

      if (config.separateTableSchema) {
        p.setSchemaFilename(config.parameters.get(currIndex + 1));
      }

      currIndex += stride;

      return p;
    }
  }

  // This exists because jcommander assumes that if you pass in an object
  // that implements iterable then it needs to get the params for the
  // sequence of objects returned by the iterable. So if CsvLoaderConfig
  // is passed into the JCommander constructor it gets the interator, which
  // returns objects that don't have any params. And then it throws up when
  // we try and parse the args. This took _way_ too long to figure out. So
  // I'm going to immortalize my struggle with this class name.
  public class IHateWeirdBugs implements Iterable<ParquetFileParams> {
    private ParquetLoaderConfig config;

    public Iterator<ParquetFileParams> iterator() {
      return new ParquetFileParamsIterator(config, iteratorStartIndex);
    }
  }
}
