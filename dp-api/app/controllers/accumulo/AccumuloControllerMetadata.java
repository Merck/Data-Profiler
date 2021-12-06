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
import com.dataprofiler.util.Context;
import com.dataprofiler.util.objects.MetadataVersionObject;
import com.dataprofiler.util.objects.VersionedMetadataObject;
import com.dataprofiler.util.objects.VersionedDatasetMetadata;
import com.dataprofiler.util.objects.VersionedAllMetadata;
import com.typesafe.config.Config;
import helpers.AccumuloHelper;
import helpers.RulesOfUseHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import static helpers.AccumuloHelper.decodeForwardSlash;

@Authenticated
@AccumuloUserContext
public class AccumuloControllerMetadata extends Controller {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final Config config;
  private final AccumuloHelper accumulo;

  @Inject
  public AccumuloControllerMetadata(Config config) throws IOException {
    this.config = config;
    this.accumulo = new AccumuloHelper();
  }

  private static class MetadataVersionResult {
    public MetadataVersionObject current;
    public List<MetadataVersionObject> all = new ArrayList<>();
  }

  public Result metadata(Request req, String dataset) {
    Context context = (Context) req.attrs().get(RulesOfUseHelper.USER_CONTEXT_TYPED_KEY);
    dataset = decodeForwardSlash(dataset);
    
    VersionedDatasetMetadata metadata =
        new VersionedMetadataObject().allMetadataForDataset(context, dataset);
    return ok(Json.toJson(metadata));
  }

  public Result metadataVersions(Request req) {
    Context context = (Context) req.attrs().get(RulesOfUseHelper.USER_CONTEXT_TYPED_KEY);
    
    MetadataVersionResult result = new MetadataVersionResult();
    
    for (MetadataVersionObject v : new MetadataVersionObject().scan(context)) {
      result.all.add(v);
    }
    
    result.current = context.getCurrentMetadataVersion();
    
    return ok(Json.toJson(result));
  }
    
  public Result metadataVersion(Request req, String versionId) {
    Context context = (Context) req.attrs().get(RulesOfUseHelper.USER_CONTEXT_TYPED_KEY);
    
    MetadataVersionObject version = new MetadataVersionObject(versionId);
    
    VersionedAllMetadata all = new VersionedMetadataObject().allMetadata(context, version);
    
    return ok(Json.toJson(all));
  }
}