package com.dataprofiler.delete;

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

import com.beust.jcommander.Parameter;
import com.dataprofiler.util.Config;

public class DeleteDataOptions extends Config {
  @Parameter(names = "--delete-index", description = "Delete index entries; Default: false")
  public boolean deleteIndex = false;

  @Parameter(names = "--delete-aliases", description = "Delete associated aliases; Default: true")
  public boolean deleteAliases = true;

  @Parameter(
      names = "--purge",
      description = "Permanently delete data from Accumulo; Default: false")
  public boolean purge = false;

  @Parameter(names = "--table-id", description = "Specific table id to delete")
  public String tableId;

  @Parameter(
      names = "--delete-previous-versions",
      description =
          "Delete all table ids before the current one. When deleting a dataset, this will delete all but the current table versions.")
  public boolean deletePreviousTableVersions = false;

  @Parameter(
      names = "--delete-all-versions",
      description =
          "Delete current and all previous table ids. When deleting a dataset, this will delete all table versions.")
  public boolean deleteAllTableVersions = false;

  public DeleteDataOptions() {
    super();
  }

  public DeleteDataOptions(Config config) {
    super(config);
  }
}
