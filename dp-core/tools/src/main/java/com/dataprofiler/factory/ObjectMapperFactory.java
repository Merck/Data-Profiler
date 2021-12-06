package com.dataprofiler.factory;

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

import com.dataprofiler.querylang.expr.Expression;
import com.dataprofiler.querylang.json.jackson.ExpressionDeserializer;
import com.dataprofiler.querylang.json.jackson.ExpressionSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectMapperFactory {
  private static final Logger logger = LoggerFactory.getLogger(ObjectMapperFactory.class);

  public static ObjectMapper initObjectMapper() {
    if (logger.isDebugEnabled()) {
      logger.debug("initObjectMapper");
    }
    ObjectMapper mapper = new ObjectMapper();
    SimpleModule expressionSerDeModule = new SimpleModule();
    expressionSerDeModule.addSerializer(Expression.class, new ExpressionSerializer());
    expressionSerDeModule.addDeserializer(Expression.class, new ExpressionDeserializer());
    mapper.registerModule(expressionSerDeModule);
    return mapper;
  }

  public static ObjectMapper initPrettyPrintObjectMapper() {
    ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    return mapper;
  }

  public static ObjectMapper initDateAwareObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    return mapper;
  }
}
