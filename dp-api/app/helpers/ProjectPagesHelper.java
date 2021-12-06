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
package helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ProjectPagesHelper {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String DEFAULT_ENVIRONMENT = "InternalDevelopment";

  private static String readFile(String path) throws IOException {
    byte[] encoded = Files.readAllBytes(Paths.get(path));
    return new String(encoded, Charset.defaultCharset());
  }

  public static ObjectNode projectList(String clusterName) throws IOException {
    String clusterFolder = null;

    if (new File("./projectFiles/" + clusterName).exists()) {
      clusterFolder = clusterName;
    } else {
      clusterFolder = DEFAULT_ENVIRONMENT;
    }

    String projectsPath = null;
    if (new File("./projectFiles").exists()) {
      projectsPath = "./projectFiles/" + clusterFolder;
    } else {
      projectsPath = "./dist/projectFiles/" + clusterFolder;
    }
    File projectsFolder = new File(projectsPath);

    File[] projectDirectoryFiles = projectsFolder.listFiles();
    ObjectNode projectDirectories = mapper.createObjectNode();

    if (projectDirectoryFiles != null) {
      for (File project : projectDirectoryFiles) {
        if (project.isHidden()) {
          continue;
        }

        ObjectNode reportObject = projectDirectories.withArray("reports").addObject();
        reportObject.put("name", project.getName());

        File projectFolder = new File(projectsPath + "/" + project.getName());
        File[] projectQueries = projectFolder.listFiles();

        for (File query : projectQueries) {
          if (query.isDirectory()) {
            ObjectNode queryObject = reportObject.withArray("queries").addObject();
            String rawOverview =
                readFile(projectFolder + "/" + query.getName() + "/overview/data.json");

            String rawMetadata = "{}";
            try {
              rawMetadata =
                  readFile(projectFolder + "/" + query.getName() + "/overview/metadata.json");
            } catch (java.nio.file.NoSuchFileException e) {
              Logger.info("No metadata for project page " + query.getName());
            }

            queryObject.put("name", query.getName());
            queryObject.set("overview", mapper.readTree(rawOverview));
            queryObject.set("metadata", mapper.readTree(rawMetadata));
          }
        }
      }
    }

    return projectDirectories;
  }

  public static JsonNode projectDataFile(
      String clusterName, String projectName, String queryName, String dateType)
      throws IOException {
    String fileName = null;
    String clusterFolder = null;

    if (new File("./projectFiles/" + clusterName).exists()) {
      clusterFolder = clusterName;
    } else {
      clusterFolder = DEFAULT_ENVIRONMENT;
    }

    if (new File("./projectFiles").exists()) {
      fileName =
          "./projectFiles/"
              + clusterFolder
              + "/"
              + projectName
              + "/"
              + queryName
              + "/"
              + dateType
              + "/data.json";
    } else {
      fileName =
          "./dist/projectFiles/"
              + clusterFolder
              + "/"
              + projectName
              + "/"
              + queryName
              + "/"
              + dateType
              + "/data.json";
    }

    if (new File(fileName).exists()) {
      String rawData = readFile(fileName);
      return mapper.readTree(rawData);
    } else {
      return mapper.createObjectNode();
    }
  }
}
