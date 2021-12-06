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
import com.dataprofiler.util.objects.DataScanSpec;
import com.dataprofiler.util.objects.DownloadSpec;
import com.dataprofiler.util.objects.VersionedDataScanSpec;
import com.dataprofiler.util.objects.VersionedDatasetMetadata.MissingMetadataException;
import com.dataprofiler.util.objects.VersionedMetadataObject;
import com.dataprofiler.util.objects.VersionedTableMetadata;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Test {
  public static void main(String[] argv)
      throws IOException, BasicAccumuloException, MissingMetadataException {
    Context context = new Context();

    String json = new String(Files.readAllBytes(Paths.get(argv[0])));
    DownloadSpec d = null;
    try {
      d = DownloadSpec.fromJson(json);
    } catch (Exception e) {
      System.err.println("Failed to read in Download spec: " + e);
      System.exit(1);
    }

    for (DataScanSpec ds : d.getDownloads()) {
      for (VersionedMetadataObject m :
          new VersionedMetadataObject()
              .scanAllLevelsForDataset(
                  context, context.getCurrentMetadataVersion(), ds.getDataset())) {
        if (m.metadata_level == 1) {
          System.out.println(m);
        }
      }

      VersionedDataScanSpec vs =
          new VersionedDataScanSpec(context, context.getCurrentMetadataVersion(), ds);
      System.out.println("table: " + ds.getTable());

      for (VersionedMetadataObject m :
          new VersionedMetadataObject()
              .scanAllLevelsForTable(
                  context, context.getCurrentMetadataVersion(), ds.getDataset(), vs.getTable())) {
        System.out.println(m);
      }

      VersionedTableMetadata metadata =
          new VersionedMetadataObject()
              .allMetadataForTable(context, ds.getDataset(), ds.getTable());
      System.out.println(metadata.getTable());
      System.out.println(metadata.getVersion());
    }
  }
}
