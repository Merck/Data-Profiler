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
import com.dataprofiler.util.iterators.ClosableIterator;
import com.dataprofiler.util.objects.MetadataVersionObject;
import com.dataprofiler.util.objects.VersionedMetadataObject;
import com.dataprofiler.util.objects.DataScanSpec;
import com.dataprofiler.util.objects.VersionedDataScanSpec;
import com.dataprofiler.util.objects.DatawaveRowObject;
import com.dataprofiler.util.objects.response.Rows;
import com.typesafe.config.Config;
import helpers.AccumuloHelper;
import helpers.RulesOfUseHelper;
import helpers.URLEncodingHelper;
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

import static helpers.AccumuloHelper.decodeForwardSlash;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

// Graphql
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.DataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.ExecutionResult;
import graphql.ExecutionInput;
import java.net.URL;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

@Authenticated
@AccumuloUserContext
public class AccumuloControllerV2 extends Controller {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final Config config;
  private final AccumuloHelper accumulo;
  private final String graphqlSdl;

  @Inject
  public AccumuloControllerV2(Config config) throws IOException {
    this.config = config;
    this.accumulo = new AccumuloHelper();
    URL graphqlurl = Resources.getResource("table_metadata_schema.graphqls"); // located in api/conf
    this.graphqlSdl = Resources.toString(graphqlurl, Charsets.UTF_8);
  }

  private GraphQLSchema buildSchema(String sdl, Context context, MetadataVersionObject version) {
    TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);
    RuntimeWiring runtimeWiring = buildWiring(context, version);
    SchemaGenerator schemaGenerator = new SchemaGenerator();
    return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
  }
    
  private RuntimeWiring buildWiring(Context context, MetadataVersionObject version) {
    // if wanting more ways to query metadata, make sure to add a dataFetcher (see 106, 114)
    return RuntimeWiring.newRuntimeWiring()
      .type(
        newTypeWiring("Query")
          .dataFetcher("metadataByDatasetName", datasetMetadataFetcherById(context, version))
          .dataFetcher("metadata", datasetMetadataFetcherAll(context, version)))
          .build();
  }
    
  private DataFetcher<List<VersionedMetadataObject>> datasetMetadataFetcherAll(
    Context context, MetadataVersionObject version) {
    // enables getting all dataset metadata and returns to a data fetching environment for parsing
    // out per schema def
    return dataFetchingEnvironment -> {
      return stream(
        new VersionedMetadataObject().scanDatasets(context, version).spliterator(), false)
          .collect(toList());
    };
  }
    
  private DataFetcher<VersionedMetadataObject> datasetMetadataFetcherById(
    Context context, MetadataVersionObject version) {
    // enables finding a specific dataset metadata by data_set name value (id) and returns a data
    // fetching environment
    return dataFetchingEnvironment -> {
      String datasetName =
        decodeForwardSlash(URLEncodingHelper.decode(dataFetchingEnvironment.getArgument("id")));
      if (logger.isTraceEnabled()) {
        logger.trace(format("fetching metadata for dataset: %s", datasetName));
      }
      return stream(
        new VersionedMetadataObject().scanDatasets(context, version).spliterator(), false)
          .filter(e -> e.dataset_name.equals(datasetName))
          .findFirst()
          .orElse(null);
    };
  }
    
  @BodyParser.Of(play.mvc.BodyParser.Json.class)
  public Result graphql(Request req) {  // V2
    Context context = (Context) req.attrs().get(RulesOfUseHelper.USER_CONTEXT_TYPED_KEY);
    MetadataVersionObject version = context.getCurrentMetadataVersion();
    
    GraphQLSchema graphQLSchema =
      buildSchema(
          this.graphqlSdl, context, version); // Can't skirt this due to needing user context
    GraphQL bgraphql = GraphQL.newGraphQL(graphQLSchema).build();
    String query =
      req.body()
        .asJson()
        .get("query")
        .asText(); // get the target query from the body and pass as text
    ExecutionInput execInput = ExecutionInput.newExecutionInput().query(query).build();
    ExecutionResult result = bgraphql.execute(execInput);
    return ok(Json.toJson(result.getData()));
  }

  @BodyParser.Of(BodyParser.TolerantText.class)
  public Result datawaverows(Request req) throws Exception {
    String json = req.body().asText();
    
    DataScanSpec spec = DataScanSpec.fromJson(json);
    
    String errorString = spec.checkRequiredFields();
  
    if (errorString != null) {
      return badRequest(errorString);
    }
    
    Context context = (Context) req.attrs().get(RulesOfUseHelper.USER_CONTEXT_TYPED_KEY);
    VersionedDataScanSpec versionedSpec = new VersionedDataScanSpec(context, spec);
    
    Integer limit = versionedSpec.getLimit();
    if (versionedSpec.getPageSize() != null) {
      limit = versionedSpec.getPageSize();
    }
    
    Rows rows = new Rows();
    
    rows.setSortedColumns(versionedSpec.getMetadata().sortedColumnNames());
    
    try (ClosableIterator<DatawaveRowObject> iter =
      new DatawaveRowObject().find(context, versionedSpec).closeableIterator()) {
      while (iter.hasNext()) {
        DatawaveRowObject rowObj = iter.next();
    
        rows.addRecord(rowObj.getRow());
   
        if (rows.getCount().equals(limit)) {
          String endLocation = format("%d_%d", rowObj.getShard(), rowObj.getRowIdx());
          rows.setEndLocation(endLocation);
          break;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      return internalServerError();
    }
    
    return ok(Json.toJson(rows));
  }
}