package com.dataprofiler.util.objects;

/*-
 * 
 * dataprofiler-util
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

import com.dataprofiler.util.Const;
import com.dataprofiler.util.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;

/** This represents a */
public class CommandSpec extends AccumuloObject<CommandSpec> {
  private String jobId;
  private String commandName;
  private List<String> commandArguments;

  private static final TypeReference<CommandSpec> staticTypeReference =
      new TypeReference<CommandSpec>() {};

  public CommandSpec() {
    super(Const.ACCUMULO_DATA_LOAD_JOBS_TABLE_ENV_KEY);
  }

  private static final String[] validCommands = {};

  @Override
  public CommandSpec fromEntry(Entry<Key, Value> entry) {
    CommandSpec c = null;
    try {
      c = fromJson(entry.getValue().toString());
    } catch (IOException e) {
      throw new InvalidDataFormat(e.toString());
    }

    Key key = entry.getKey();
    c.jobId = key.getRow().toString();

    c.updatePropertiesFromEntry(entry);

    return c;
  }

  public static CommandSpec fetchByJobId(Context context, String jobId) {
    CommandSpec c = new CommandSpec();
    c.setJobId(jobId);
    return c.fetch(context, c.createAccumuloKey());
  }

  /**
   * Key for DownloadSpec consists of the following:
   *
   * <p>RowID - jobId
   *
   * <p>ColFam - COL_FAM_DOWNLOAD_SPEC
   *
   * <p>ColQual - ""
   *
   * @return Key
   * @throws InvalidDataFormat
   */
  @Override
  public Key createAccumuloKey() throws InvalidDataFormat {
    return new Key(jobId, Const.COL_FAM_COMMAND_SPEC, "", visibility);
  }

  public static CommandSpec fromJson(String json) throws IOException {
    return mapper.readValue(json, staticTypeReference);
  }

  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  public String getCommandName() {
    return commandName;
  }

  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  public List<String> getCommandArguments() {
    return commandArguments;
  }

  public void setCommandArguments(List<String> commandArguments) {
    this.commandArguments = commandArguments;
  }

  public String toJson() throws JsonProcessingException {
    return mapper.writeValueAsString(this);
  }

  @Override
  public String toString() {
    return "CommandSpec{"
        + "jobId='"
        + jobId
        + '\''
        + ", commandName='"
        + commandName
        + '\''
        + ", commandArguments="
        + commandArguments
        + '}';
  }
}
