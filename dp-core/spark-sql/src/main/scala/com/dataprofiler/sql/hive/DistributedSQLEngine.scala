package com.dataprofiler.sql.hive

/*-
 * 
 * spark-sql
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

import org.apache.spark.sql._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.dataprofiler.util.Context
import com.dataprofiler.util.objects.DataScanSpec
import com.dataprofiler.dse.controller.model.{LoadDataScanSpecRequest, LoadRequest, RefreshTableRequest}

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

object DistributedSQLEngine {

  val ACCUMULO_DATA_SOURCE_V1 = "org.apache.spark.sql.sources.accumulo.v1"

  implicit val system: ActorSystem = ActorSystem("dse-actor-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  def main(args: Array[String]): Unit = {
    println(s"args: $args")
    args.foreach(it => {
      println(s"part -> $it")
    })

    val context = new Context(args)
    val config = context.getConfig
    println("context.config:\n" + config.toString)

    implicit val spark = SparkSession
      .builder()
      .appName("DistributedSQLEngine")
      .enableHiveSupport()
      .config("spark.zookeepers", config.zookeepers)
      .config("spark.instance", config.accumuloInstance)
      .getOrCreate()

    println("Starting Hive2ThriftServer....")
    println(spark.conf.getAll.mkString("\n"))

    import com.dataprofiler.dse.controller.model.LoadRequestJsonSupport._
    import org.apache.spark.sql.hive.thriftserver.HiveThriftServer2
    HiveThriftServer2.startWithContext(spark.sqlContext)

    val route = {
      path("load") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Distributed SQL Engine Load Endpoint</h1>"))
        } ~
        post {
          entity(as[LoadRequest]) { request =>
            complete {
              val optionMap = convertContextToOptions(context, request.dataset, request.table)
              createTableView(optionMap, s"${request.table}_view")
              s"Table: '${request.table}_view' from Dataset: '${request.dataset}' loaded"
            }
          } ~
          entity(as[LoadDataScanSpecRequest]) { request =>
            complete {
              val spec = DataScanSpec.fromJson(request.dataScanSpec)
              val optionMap = parseOptions(request.dataScanSpec)
              createTableView(optionMap, s"${spec.getTable}_view")
              s"Loaded '${spec.getDataset}.${spec.getTable}' as '${spec.getTable}_view'"
            }
          }
        }
      } ~
      path("refresh") {
        post {
          entity(as[RefreshTableRequest]) { request =>
            complete {
              s"Refreshed table '${request.table}''"
            }
          }
        }
      } ~
      path("catalog") {
        post {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1Catalog Endpoint></h1>"))
        }
      }
    }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 10005)

    println(s"Server online at http://localhost:10005/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done

    while (true) {
      Thread.sleep(30000)
      println("running....")
    }
  }

  def createTableView(options: Map[String, String], viewName: String)(
    implicit spark: SparkSession): Unit = {
    val df = spark
      .read
      .options(options)
      .format(ACCUMULO_DATA_SOURCE_V1)
      .load()
    df.printSchema()
    val dfCached = df.cache()
    dfCached.show(100, false)
    df.registerTempTable(viewName)
  }

  def refreshTable(tableName: String)(implicit spark: SparkSession): Unit = {
    println(s"Refreshing table: $tableName ....")
    spark.sql(s"refresh table $tableName");
  }

  def convertContextToOptions(context: Context, dataset: String, table: String): Map[String, String] = {
    Map (
      "instance" -> context.getConfig.accumuloInstance,
      "zookeepers" -> context.getConfig.zookeepers,
      "user" -> context.getConfig.accumuloUser,
      "password" -> context.getConfig.accumuloPassword,
      "table" -> context.getConfig.accumuloDatawaveRowsTable,
      "auths" -> context.getAuthorizations.getAuthorizationsArray.mkString(","),
      "dataset" -> dataset,
      "datasetTable" -> table
    )
  }

  def parseOptions(dataScanSpec: String): Map[String, String] = {
    Map (
      "auths" -> "",
      "dataScanSpec" -> dataScanSpec
    )
  }

}
