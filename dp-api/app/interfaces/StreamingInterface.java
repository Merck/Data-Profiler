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
package interfaces;

import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import play.http.HttpEntity;
import play.mvc.ResponseHeader;
import play.mvc.Result;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

public interface StreamingInterface {

  static final Integer BUFFER_SIZE = 32768;
  static final ObjectMapper mapper = new ObjectMapper();
  static final Logger logger = Logger.getLogger(StreamingInterface.class);
  static final String tmpDir = "/tmp/";

  default String getTemporaryFilePath() {
    return tmpDir + UUID.randomUUID().toString();
  }

  default BufferedWriter getBufferedWriter(String filePath) throws IOException {
    return new BufferedWriter(new FileWriter(filePath), BUFFER_SIZE);
  }

  default Result streamJsonArray(String filePath) {
    try {
      // this is horrible, but hopefully "streaming to file" isn't a long term solution
      // i tried to not include the comma if it was the last record, but sometimes logic has to be
      // done inside the iterator
      String[] command = {"sed", "-i", "-e", "s/},]$/}]$/g", filePath};
      ProcessBuilder pb = new ProcessBuilder(command).inheritIO();
      Process process = pb.start();
      process.waitFor();

      Source<ByteString, ?> source = FileIO.fromPath(Paths.get(filePath));
      Optional<Long> contentLength = Optional.of(new File(filePath).length());
      return new Result(
          new ResponseHeader(200, Collections.emptyMap()),
          new HttpEntity.Streamed(source, contentLength, Optional.of("application/json")));
    } catch (IOException e) {
      logger.error(e);
      return new Result(500);
    } catch (InterruptedException e) {
      logger.error(e);
      return new Result(500);
    }
  }
}
