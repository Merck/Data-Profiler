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

import com.dataprofiler.util.BasicAccumuloException;
import com.typesafe.config.Config;
import helpers.DataprofilerConfigAdaptor;
import helpers.RulesOfUseHelper;
import helpers.RulesOfUseHelper.UserAttributes;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

public class AccumuloUserContextImpl extends Action<AccumuloUserContext> {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private com.dataprofiler.util.Config config;
  private static com.dataprofiler.util.Context context;

  @Inject
  AccumuloUserContextImpl(Config config) {
    this.config = DataprofilerConfigAdaptor.fromPlayConfigruation(config);
    logger.debug("AccumuloUserContextImpl ctor ");
  }

  @Override
  public CompletionStage<Result> call(Http.Request req) {
    com.dataprofiler.util.Context rootContext;
    com.dataprofiler.util.Context userContext;
    try {
      rootContext = getContext();
      userContext = getContextForUser(req);
      logger.debug(
        "setting rootContext: " +
        rootContext +
        " auths: " +
        rootContext.getAuthorizations()
      );
      logger.debug(
        "setting userContext: " +
        userContext +
        " auths: " +
        userContext.getAuthorizations()
      );
    } catch (BasicAccumuloException e) {
      Result internalError = Results.internalServerError(
        Json.toJson("Failed to get Accumulo " + "context for user")
      );
      return CompletableFuture.completedFuture(internalError);
    }

    logger.debug("setting new contexts");
    return delegate.call(
      req
        .addAttr(RulesOfUseHelper.ROOT_CONTEXT_TYPED_KEY, rootContext)
        .addAttr(RulesOfUseHelper.USER_CONTEXT_TYPED_KEY, userContext)
    );
  }

  public com.dataprofiler.util.Context getContextForUser(Http.Request req)
    throws BasicAccumuloException {
    getContext();

    RulesOfUseHelper.UserAttributes ua = (UserAttributes) req
      .attrs()
      .get(RulesOfUseHelper.ROU_ATTRIBUTES_TYPED_KEY);
    logger.debug(
      "creating new context for user: " +
      ua.getUsername() +
      " with auths: " +
      ua.currentAsAuthorizations()
    );
    return new com.dataprofiler.util.Context(
      context,
      ua.currentAsAuthorizations()
    );
  }

  public com.dataprofiler.util.Context getContext()
    throws BasicAccumuloException {
    if (context == null) {
      logger.debug("creating new context from config");
      context = new com.dataprofiler.util.Context(this.config);
      context.refreshAuthorizations();
    }
    return context;
  }
}
