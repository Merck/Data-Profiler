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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class HDFSHead {
  Configuration conf;

  public HDFSHead() {
    conf = new Configuration();
    conf.set("fs.hdfs.impl", org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());
    conf.set("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());
  }

  // This will return
  public byte[] head(String fname, int len) throws IOException {
    Path p = new Path("hdfs://primary/" + fname);

    FileSystem fs = FileSystem.get(p.toUri(), conf);
    FSDataInputStream is = fs.open(p);

    byte[] header = new byte[2];
    is.read(header);

    is.seek(0);

    byte[] out = new byte[len];
    int read;
    if (header[0] == 0x1f && header[1] == (byte) 0x8b) {
      InputStream gzipStream = new GZIPInputStream(is, len);
      int offset = 0;
      read = gzipStream.read(out, offset, len);
    } else {
      read = is.read(out);
    }

    return Arrays.copyOf(out, read);
  }

  public static void main(String[] args) {
    HDFSHead h = new HDFSHead();
    try {
      System.out.write(h.head(args[0], 1024 * 128));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
