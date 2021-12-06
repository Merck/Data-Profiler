/**
*  Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
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
**/
package handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Environment;
import play.api.OptionalSourceMapper;
import play.api.UsefulException;
import play.http.JsonHttpErrorHandler;
import play.libs.Json;
import play.mvc.Http.RequestHeader;

@Singleton
public class DataProfilerJsonErrorHandler extends JsonHttpErrorHandler {

  private static final Logger logger = LoggerFactory.getLogger(DataProfilerJsonErrorHandler.class);

  @Inject
  public DataProfilerJsonErrorHandler(
  Environment environment,
  OptionalSourceMapper sourceMapper) {
    super(environment, sourceMapper);
    logger.debug("Custom error handler constructed");
  }

  protected JsonNode prodServerError(RequestHeader request, UsefulException exception) {
    ObjectNode exceptionJson = Json.newObject();
    exceptionJson.put("title", exception.title);
    exceptionJson.put("description", exception.description);
//    exceptionJson.set("stacktrace", formatDevServerErrorException(exception.cause));

    ObjectNode result = Json.newObject();
    result.put("id", exception.id);
    result.put("requestId", request.asScala().id());
    result.set("exception", exceptionJson);

    logger.debug("Prod server error");

    return error(result);
  }

  private JsonNode error(JsonNode content) {
    ObjectNode result = Json.newObject();
    result.set("error", content);
    return result;
  }
}
