package com.dataprofiler.util.objects;

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

import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.MiniAccumuloContext;
import com.dataprofiler.util.objects.CustomAnnotation.AnnotationType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class CustomAnnotationCsvLoader {
  private static final Logger logger = LoggerFactory.getLogger(CustomAnnotationCsvLoader.class);

  private MiniAccumuloContext context;

  public CustomAnnotationCsvLoader(MiniAccumuloContext context) {
    super();
    this.context = context;
  }

  public Map<String, List<CustomAnnotation>> loadTestFiles(Map<String, String> datasets) {
    Map<String, List<CustomAnnotation>> map = new HashMap<>();
    for (Map.Entry<String, String> entry : datasets.entrySet()) {
      String dataset = entry.getKey();
      String datasetPath = entry.getValue();
      Path path = Paths.get(datasetPath);
      try {
        List<CustomAnnotation> insertions = loadCsv(path);
        map.putIfAbsent(dataset, insertions);
      } catch (IOException | BasicAccumuloException e) {
        logger.warn(
            "there was an issue inserting an annotation data row. attempting to move on...");
        logger.warn(e.getMessage());
        throw new RuntimeException(e);
      }
    }

    return map;
  }

  public List<CustomAnnotation> loadCsv(Path path) throws IOException, BasicAccumuloException {
    CSVParser parser =
        CSVFormat.RFC4180.withFirstRecordAsHeader().parse(Files.newBufferedReader(path));
    List<CustomAnnotation> annotations = new ArrayList<>();
    int count = 0;
    for (CSVRecord csvRecord : parser) {
      count++;
      if (logger.isTraceEnabled()) {
        logger.trace(format("[%s] parsed record %s:", count, csvRecord));
      }
      AnnotationType annotationType = AnnotationType.valueOf(csvRecord.get(0).toUpperCase().trim());
      String dataset = csvRecord.get(1);
      String table = csvRecord.get(2);
      String column = csvRecord.get(3);
      String note = csvRecord.get(4);
      String createdBy = csvRecord.get(5);
      long createdOn = Long.parseLong(csvRecord.get(6));
      CustomAnnotation inserted =
          insertAnnotation(annotationType, dataset, table, column, note, createdBy, createdOn);
      annotations.add(inserted);
    }
    return annotations;
  }

  private CustomAnnotation insertAnnotation(
      AnnotationType annotationType,
      String dataset,
      String table,
      String column,
      String note,
      String createdBy,
      long createdOn)
      throws IOException, BasicAccumuloException {
    CustomAnnotation annotation = CustomAnnotationMock.customAnnotationWithMockedVisibility();
    annotation.setAnnotationType(annotationType);
    annotation.setDataset(dataset);
    annotation.setTable(table);
    annotation.setColumn(column);
    annotation.setNote(note);
    annotation.setCreatedBy(createdBy);
    annotation.setCreatedOn(createdOn);
    annotation.put(context);
    return annotation;
  }
}
