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

import com.beust.jcommander.Parameter;

/**
 * * Special config for programs that process CSV.
 *
 * <p>We need lots of information in order to correctly load a CSV file and that must be passed in
 * per-file. So this handles that by taking in a long string of positional arguments. Is this great?
 * Definitely not. Is it relatively simple and works? Sure.
 */
public class CsvLoaderConfig extends LoaderConfig {

  @Parameter(names = "--delimiter")
  public String delimiter = ",";

  @Parameter(names = "--escape")
  public String escape = "\\";

  @Parameter(names = "--quote")
  public String quote = "\"";

  @Parameter(names = "--charset")
  public String charset = "utf-8";

  @Parameter(names = "--csv-file")
  public String file;

  public CsvFileParams toFileParams() {
    CsvFileParams p = new CsvFileParams();

    p.setDelimiter(delimiter);
    p.setEscape(escape);
    p.setQuote(quote);
    p.setCharset(charset);
    p.setInputFilename(file);

    if (super.separateTableSchema) {
      p.setSchemaFilename(super.schemaPath);
    }

    System.out.println(p);
    return p;
  }
}
