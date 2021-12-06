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
package controllers.accumulo;

import actions.AccumuloUserContext;
import actions.Authenticated;
import com.dataprofiler.util.Const.SortOrder;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.objects.ColumnCountObject;
import com.dataprofiler.util.objects.VersionedMetadataObject;
import com.dataprofiler.util.objects.ColumnCountSampleObject;
import com.typesafe.config.Config;
import helpers.AccumuloHelper;
import helpers.RulesOfUseHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

import static helpers.AccumuloHelper.decodeForwardSlash;

@Authenticated
@AccumuloUserContext
public class AccumuloControllerTableSamples extends Controller {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final Config config;
  private final AccumuloHelper accumulo;

  @Inject
  public AccumuloControllerTableSamples(Config config) throws IOException {
    this.config = config;
    this.accumulo = new AccumuloHelper();
  }

  public Result tableSamples(Request req, String dataset, String table) {
    Context context = (Context) req.attrs().get(RulesOfUseHelper.USER_CONTEXT_TYPED_KEY);
    dataset = decodeForwardSlash(dataset);
    table = decodeForwardSlash(table);

    Map<String, List<ColumnCountObject.ValueCountObject>> result = new HashMap<>();

    VersionedMetadataObject metadata =
        new VersionedMetadataObject()
            .fetchTable(context, context.getCurrentMetadataVersion(), dataset, table);

    for (ColumnCountObject sample :
        new ColumnCountSampleObject().fetchTable(context, metadata, SortOrder.CNT_DESC)) {

      result.compute(
          sample.getColumn(),
          (col, samples) -> {
            if (samples == null) {
              samples = new ArrayList<>();
            }
            samples.add(new ColumnCountObject.ValueCountObject(sample.value, sample.count));

            return samples;
          });
    }

    return ok(Json.toJson(result));
  }
}