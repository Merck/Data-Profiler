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

public class CsvFileParams {
  private String inputFilename;
  private String delimiter = ",";
  private String escape = "\\";
  private String quote = "\"";
  private String charset = "utf-8";
  private String schemaFilename;
  private boolean hasHeader = true;
  private boolean multiline = true;
  private boolean inferSchema = false;

  public CsvFileParams() {}

  public CsvFileParams(String inputFilename) {
    this.inputFilename = inputFilename;
  }

  @Override
  public String toString() {
    return "CsvFileParams{"
        + "inputFilename='"
        + inputFilename
        + '\''
        + ", delimiter='"
        + delimiter
        + '\''
        + ", escape='"
        + escape
        + '\''
        + ", quote='"
        + quote
        + '\''
        + ", charset='"
        + charset
        + '\''
        + ", schemaFilename='"
        + schemaFilename
        + '\''
        + ", hasHeader="
        + hasHeader
        + ", multiline="
        + multiline
        + '}';
  }

  private String removeExtraQuotes(String str) {
    return str.replaceAll("'", "");
  }

  public String getInputFilename() {
    return inputFilename;
  }

  public void setInputFilename(String inputFilename) {
    this.inputFilename = inputFilename;
  }

  public String getDelimiter() {
    return delimiter;
  }

  public void setDelimiter(String delimiter) {
    this.delimiter = removeExtraQuotes(delimiter);
  }

  public String getEscape() {
    return escape;
  }

  public void setEscape(String escape) {
    this.escape = removeExtraQuotes(escape);
  }

  public String getQuote() {
    return quote;
  }

  public void setQuote(String quote) {
    this.quote = removeExtraQuotes(quote);
  }

  public String getCharset() {
    return charset;
  }

  public void setCharset(String charset) {
    this.charset = charset;
  }

  public String getSchemaFilename() {
    return schemaFilename;
  }

  public void setSchemaFilename(String schemaFilename) {
    this.schemaFilename = schemaFilename;
  }

  public boolean getHasHeader() {
    return hasHeader;
  }

  public void setHasHeader(boolean hasHeader) {
    this.hasHeader = hasHeader;
  }

  public boolean isMultiline() {
    return multiline;
  }

  public void setMultiline(boolean multiline) {
    this.multiline = multiline;
  }

  public boolean getInferSchema() {
    return inferSchema;
  }

  public void setInferSchema(boolean inferSchema) {
    this.inferSchema = inferSchema;
  }
}
