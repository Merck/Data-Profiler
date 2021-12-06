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
import com.dataprofiler.util.Config;
import com.dataprofiler.util.Const;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.objects.CommandSpec;
import com.dataprofiler.util.objects.DataScanSpec;
import com.dataprofiler.util.objects.DataScanSpec.Type;
import com.dataprofiler.util.objects.DownloadSpec;
import com.dataprofiler.util.objects.InvalidDataFormat;
import com.dataprofiler.util.objects.Job;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.apache.hadoop.conf.Configured;

public class LoaderTool extends Configured {

  public static void usage() {
    System.out.println("LoaderTool " + "<list|new-upload|new-download|cancel|killall|server>");
  }

  public static void main(String[] args) throws IOException, BasicAccumuloException {
    Context context = new Context(args);
    Config config = context.getConfig();
    HDFSHead head = new HDFSHead();

    if (config.parameters.size() < 1) {
      usage();
      System.exit(1);
    }

    String command = config.parameters.get(0);
    if (command.equals("list")) {
      List<Job> jobs = null;
      jobs = new Job().listJobs(context, -1, -1);
      for (Job job : jobs) {
        System.out.println(job.toFriendlyString());
      }
    } else if (command.equals("new-upload")) {
      if (config.parameters.size() != 5) {
        System.out.println("LoaderTool new-upload jobId datasetName visibilities s3path");
        System.exit(1);
      }
      String jobId = config.parameters.get(1);
      String datasetName = config.parameters.get(2);
      String visibilities = config.parameters.get(3);
      String s3Path = config.parameters.get(4);

      Job job =
          new Job(
              jobId, datasetName, visibilities, s3Path, Job.NEW, System.getProperty("user.name"));

      job.put(context);
    } else if (command.equals("new-download")) {
      if (config.parameters.size() < 5) {
        System.out.println(
            "LoaderTool new-download jobId <row|column> datasetName tableName " + "[columnName]");
        System.exit(1);
      }
      Job job = new Job();
      job.setType(Job.Type.DOWNLOAD);
      DownloadSpec spec = new DownloadSpec();
      DataScanSpec ds = new DataScanSpec();
      spec.getDownloads().add(ds);

      job.setJobId(config.parameters.get(1));
      spec.setJobId(job.getJobId());

      String type = config.parameters.get(2);
      if (type.equals("row")) {
        ds.setType(Type.ROW);
      } else if (type.equals("column")) {
        ds.setType(Type.COLUMN_COUNT);
      }

      ds.setDataset(config.parameters.get(3));
      ds.setTable(config.parameters.get(4));
      if (config.parameters.size() > 5) {
        ds.setColumn(config.parameters.get(5));
      }

      job.put(context);
      spec.put(context);

    } else if (command.equals("cancel")) {
      if (config.parameters.size() != 2) {
        System.out.println("LoaderTool cancel jobId");
        System.exit(1);
      }

      Job job = Job.fetchByJobId(context, config.parameters.get(1));
      job.setStatus(Job.CANCELLED);
      job.put(context);
    } else if (command.equals("killall")) {
      BatchWriter writer =
          context.createBatchWriter(
              config.getAccumuloTableByName(Const.ACCUMULO_DATA_LOAD_JOBS_TABLE_ENV_KEY));

      for (Job job : new Job().listJobs(context, Job.NEW, -1)) {
        job.setStatus(Job.CANCELLED);
        job.put(context, writer);
      }
      try {
        writer.close();
      } catch (MutationsRejectedException e) {
        System.out.println("Error killing jobs: " + e);
        System.exit(1);
      }
    } else if (command.equals("server")) {
      /*
       * For some of the commandline tools we want to be able to execute several commands
       * at once and the startup / connection overhead for this tool is really high. So
       * this 'server' mode just accepts simple commands and returns data.
       */
      // connect here so that all of the connection junk prints out before we send ready
      context.connect();
      System.out.println("ready");
      Scanner input = new Scanner(System.in);
      while (true) {
        String line;
        try {
          line = input.nextLine();
        } catch (NoSuchElementException e) {
          break;
        }

        if (line.startsWith("exit")) {
          break;
        } else if (line.startsWith("update")) {
          String data = line.substring("update".length() + 1);
          Job job = Job.fromJson(data);

          try {
            job.checkRequiredFields();
            job.put(context);
          } catch (Exception e) {
            System.out.println("error\nend");
            continue;
          }
          System.out.println("success\nend");
        } else if (line.startsWith("get_download")) {
          String jobId = line.substring("get_download".length() + 1);
          DownloadSpec ds = DownloadSpec.fetchByJobId(context, jobId);
          if (ds == null) {
            System.out.println("none");
          } else {
            System.out.println(ds.toJson());
          }
          System.out.println("end");
        } else if (line.startsWith("get_command_job")) {
          String jobId = line.substring("get_command_job".length() + 1);
          CommandSpec cs = CommandSpec.fetchByJobId(context, jobId);

          if (cs == null) {
            System.out.println("none");
          } else {
            System.out.println(cs.toJson());
          }
          System.out.println("end");
        } else if (line.startsWith("get")) {
          String jobId = line.substring("get".length() + 1);
          Job job = null;
          try {
            job = Job.fetchByJobId(context, jobId);
          } catch (Exception e) {
            System.out.println("error\nend");
            continue;
          }
          if (job == null) {
            System.out.println("none");
          } else {
            System.out.println(job.toJson());
          }
          System.out.println("end");
        } else if (line.startsWith("list")) {
          int limit;

          try {
            limit = Integer.parseInt(line.substring("list".length() + 1));
          } catch (NumberFormatException e) {
            System.out.println("error\nend");
            continue;
          }
          try {
            for (Job job : new Job().listJobs(context, -1, limit)) {
              System.out.println(job.toJson());
            }
          } catch (InvalidDataFormat e) {
            System.out.println("error\nend");
          }
          System.out.println("end");
        } else if (line.startsWith("head")) {
          String fname = line.substring("head".length() + 1);

          byte[] d;
          try {
            d = head.head(fname, 1024 * 256);
          } catch (IOException e) {
            System.out.println("error\nend");
            continue;
          }
          // This is a workaround, because hadoop overloads standard out
          //with output. Improved logging will be added later.
          System.out.println("\nbegin");
          try {
            System.out.write(d);
          } catch (IOException e) {
            System.out.println("error\nend");
            continue;
          }
          System.out.println("\nend");
        } else if (line.startsWith("activate_viz_expression")) {
          String expression = line.substring("activate_viz_expression".length() + 1);
          ActiveVisibilityManager m = new ActiveVisibilityManager();

          try {
            m.setVisibilitiesFromExpressionAsActive(expression);
          } catch (Exception e) {
            System.out.println("error\nend");
            continue;
          }
          System.out.println("\nend");
        }
      }
    } else {
      usage();
      System.exit(1);
    }
  }
}
