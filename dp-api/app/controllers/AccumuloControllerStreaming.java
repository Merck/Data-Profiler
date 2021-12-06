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

import actions.AccumuloUserContext;
import actions.Authenticated;
import com.fasterxml.jackson.databind.JsonNode;
import com.dataprofiler.util.Const;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.iterators.ClosableIterator;
import com.dataprofiler.util.objects.*;
import com.dataprofiler.util.objects.DataScanSpec.Type;
import helpers.RulesOfUseHelper;
import interfaces.StreamingInterface;
import objects.ColumnCountResult;
import objects.SearchResult;
import org.apache.accumulo.core.data.Key;
import org.apache.log4j.Logger;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;

import java.io.BufferedWriter;
import java.io.IOException;


/**
 *
 * To the poor soul who has to look at this code. You may ask yourself...why is this called a Streaming controller?
 * It's because it was intended to be a streaming controller, and that's what we're trying to "simulate"
 *
 * Four score ago, an ashamed developer set out to develop a streaming controller that would stream accumulo results
 * straight to the http client. It would use akka's reactive streams, and it would require no disk or ram backpressure
 * to accomplish. That sounds awesome right? That ashamed developer was woefully unprepared for what was about to happen.
 * There were countless libraries to upgrade, conventions to change, all to get to the position where it was time to
 * put in the reactive stream. Priorites and deadlines came and went, but the reactive stream refused to stream, nor
 * be reactive. Only to turn out that there were numerous bugs/issues logged for the "experimental / totally non-production"
 * version of reactive streams. Therefore, in an attempt to set a "future raiseable standard", we have this hunk of junk that
 * writes a file, "streams" it, and then deletes the file. So whoever gets to refactor this when reactive streams is ready
 * for production, enjoy your new rockstar status.
 *
 * Until then, have fun "increasing your disk space"!
 *
 * Signed, ashamed developer
 *
 **/


@Authenticated
@AccumuloUserContext
public class AccumuloControllerStreaming extends Controller implements StreamingInterface {

  private static final Logger logger = Logger.getLogger(AccumuloControllerStreaming.class);

  @BodyParser.Of(BodyParser.TolerantText.class)
  public Result rowsStream(Request req) throws IOException, VersionedDatasetMetadata.MissingMetadataException {
    String json = req.body().asText();
    DataScanSpec spec = DataScanSpec.fromJson(json);
    String errorString = spec.checkRequiredFields();
    if (errorString != null) {
      return badRequest(errorString);
    }

    Integer intLimit = spec.getLimit();
    Long limit = null;
    Long counter = null;
    if (intLimit != null && intLimit > 0) {
      limit = new Long(intLimit);
      counter = new Long(0);
    }

    Context context = (Context) req.attrs().get(RulesOfUseHelper.USER_CONTEXT_TYPED_KEY);
    VersionedDataScanSpec versionedSpec = new VersionedDataScanSpec(context, spec);

    String filepath = getTemporaryFilePath();
    BufferedWriter bw = getBufferedWriter(filepath);

    try(ClosableIterator<DatawaveRowObject> iter = new DatawaveRowObject().find(context, versionedSpec).closeableIterator()) {
      bw.write("[");
      while (iter.hasNext()) {
        bw.write(mapper.writeValueAsString(iter.next().getRow()) + ",");

        // handles limit
        if (counter != null) {
          counter++;
          if (counter >= limit){
            break;
          }
        }
      }
      bw.write("]");
      bw.close();
    }catch (Exception e) {
      logger.error(e);
      return new Result(500);
    }
    return streamJsonArray(filepath);
  }
}
