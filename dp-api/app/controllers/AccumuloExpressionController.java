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
package controllers;

import helpers.AccumuloExpressionHelper;
import org.apache.accumulo.core.util.BadArgumentException;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;

public class AccumuloExpressionController extends Controller {

  private static final String EXCEPTION_PREAMBLE =
      "org.apache.accumulo.core.util.BadArgumentException: ";
  private static final String LINE_ENDINGS = "\\r?\\n";

  @BodyParser.Of(BodyParser.Text.class)
  public Result escapeSingleString(Request req) {
    String input = req.body().asText();
    String ret = AccumuloExpressionHelper.escapeSingleString(input);
    return ok(ret);
  }

  @BodyParser.Of(BodyParser.Text.class)
  public Result validateExpression(Request req) {
    String input = req.body().asText();
    try {
      String ret = AccumuloExpressionHelper.validateExpression(input);
      return ok(ret);
    } catch (BadArgumentException e) {
      return badRequest(e.toString().split(LINE_ENDINGS)[0].replace(EXCEPTION_PREAMBLE, ""));
    }
  }
}
