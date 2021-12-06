package com.dataprofiler;

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

import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.objects.APIToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

public class ApiTokenTool {
  public static void usage() {
    System.out.println("ApiTokenTool <verify token|create username|create-root config_path>");
  }

  public static void main(String[] argv) throws IOException, BasicAccumuloException {
    Context context = new Context(argv);

    if (context.getConfig().parameters.size() != 2) {
      usage();
      System.exit(1);
    }

    String command = context.getConfig().parameters.get(0);
    String arg = context.getConfig().parameters.get(1);

    APIToken tok;
    switch (command) {
      case "verify":
        tok = APIToken.fetchByToken(context, arg);
        if (tok == null) {
          System.out.println("Token is not valid");
        } else {
          System.out.println("Valid token for " + tok.getUsername());
        }
        break;
      case "create":
        tok = APIToken.createToken(arg);
        tok.put(context);
        System.out.println("API Token created: " + tok.getToken());
        break;
      case "create-root":
        /*
         * This only creates the token if it's actually needed to be created
         */
        tok = APIToken.createTokenIfNeeded(context, "root");
        tok.put(context);

        try (BufferedWriter fd = Files.newBufferedWriter(Paths.get(arg))) {
          HashMap<String, String> out = new HashMap<>();
          out.put("username", tok.getUsername());
          out.put("key", tok.getToken());
          ObjectMapper mapper = new ObjectMapper();
          fd.write(mapper.writeValueAsString(out));
        }
        break;
      default:
        System.out.println("Command not found: " + command);
        usage();
        System.exit(1);
    }
  }
}
