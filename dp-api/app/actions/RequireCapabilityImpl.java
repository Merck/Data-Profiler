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
package actions;

import helpers.RulesOfUseHelper;
import helpers.RulesOfUseHelper.UserAttributes;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class RequireCapabilityImpl extends Action<RequireCapability> {
  public CompletionStage<Result> call(Http.Request req) {

    UserAttributes curUserAttrs =
        (UserAttributes) req.attrs().get(RulesOfUseHelper.ROU_ATTRIBUTES_TYPED_KEY);
    String capability = configuration.value();
    Boolean hasCapability = curUserAttrs.hasCapability(capability);
    if (!hasCapability) {
      Result forbidden =
          Results.forbidden(
              Json.toJson(
                  "This action requires "
                      + RulesOfUseHelper.SYSTEM_CAPABILITY_PREFIX
                      + capability
                      + " privileges."));
      return CompletableFuture.completedFuture(forbidden);
    }
    return delegate.call(req);
  }
}
