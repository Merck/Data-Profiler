package org.apache.spark.sql.sources.accumulo.v1

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

import com.dataprofiler.{DPSparkContext, SparkAccumuloIO, TableExporter}
import com.dataprofiler.querylang.json.Expressions
import com.dataprofiler.util.objects.DataScanSpec
import com.dataprofiler.util.Context
import org.apache.accumulo.core.client.ZooKeeperInstance
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.apache.accumulo.core.security.Authorizations
import org.apache.log4j.Logger
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Row, SQLContext, SparkSession}
import org.apache.spark.sql.sources.{And, BaseRelation, EqualTo, Filter, GreaterThan, GreaterThanOrEqual, LessThan, LessThanOrEqual, Or, PrunedFilteredScan, PrunedScan, TableScan}
import org.apache.spark.sql.types.{StringType, StructType}

import scala.util.parsing.json.JSON
import scala.collection.JavaConversions._

class AccumuloDataSourceRelation(override val sqlContext: SQLContext,
                                  parameters: Map[String, String],
                                  userSchema: StructType)
  extends BaseRelation with TableScan with PrunedScan with PrunedFilteredScan with Serializable  {

  val logger: Logger = Logger.getLogger(this.getClass)

  final val ScanSpecProp: String = "dataScanSpec"
  final val ScanAuthProp: String = "auths"

  override def schema: StructType = {
    if (userSchema != null) {
      userSchema
    } else {
      retrieveSchemaFromMetadata(getSchemaInfo())
    }
  }

  override def buildScan(): RDD[Row] = {
    val dpSparkContext = new DPSparkContext(sqlContext.sparkContext)
    val spark = SparkSession.builder().sparkContext(sqlContext.sparkContext).getOrCreate()
    val sparkAccumuloIO = new SparkAccumuloIO(dpSparkContext)
    val dataScanSpec = DataScanSpec.fromJson(parameters(ScanSpecProp))

    TableExporter
      .exportDataScanSpec(dpSparkContext, spark, sparkAccumuloIO, dataScanSpec)
      .rdd
  }

  override def buildScan(requiredColumns: Array[String], filters: Array[Filter]): RDD[Row] = {
    val spark = SparkSession.builder().sparkContext(sqlContext.sparkContext).getOrCreate()
    val scanAuthorizations: Authorizations =
      new Authorizations(parameters(ScanAuthProp).split(',').toList.map(_.getBytes()))

    val dpSparkContext = new DPSparkContext(spark.sparkContext, scanAuthorizations)

    val context = new Context(Array(
        "--zookeepers", spark.conf.get("spark.zookeepers"),
        "--accumulo-instance", spark.conf.get("spark.instance")))

    dpSparkContext.setConfig(context.getConfig)
    dpSparkContext.setAuthorizations(scanAuthorizations)

    val sparkAccumuloIO = new SparkAccumuloIO(dpSparkContext)
    val dataScanSpec = DataScanSpec.fromJson(parameters(ScanSpecProp))

    // Setup pruned columns
    dataScanSpec.setColumnOrder(requiredColumns.toList)

    // Setup filters
    if (filters.nonEmpty) {
      dataScanSpec.setV2Query(Expressions.parse(buildV2QueryFromSqlFilters(filters)))
    }

    println(s"dpSparkContext: ${dpSparkContext.toString}")
    println(s"scanAuthorizations: ${scanAuthorizations.getAuthorizations}")
    scanAuthorizations.getAuthorizations.foreach(f => {
      println("AUTH: " + new String(f))
    })

    TableExporter
      .exportDataScanSpec(dpSparkContext, spark, sparkAccumuloIO, dataScanSpec, scanAuthorizations)
      .rdd
  }

  override def buildScan(requiredColumns: Array[String]): RDD[Row] =
    buildScan(requiredColumns, Array.empty[Filter])

  def buildV2QueryFromSqlFilters(filters: Array[Filter]): String = {
    var v2Expr: String = ""
    filters.foreach {
      case And(left, right) =>
        logger.info(s"And($left,$right)")
        v2Expr = v2Expr + s"{'$$and': [" + buildV2QueryFromSqlFilters(Array(left)) + "," +
          buildV2QueryFromSqlFilters(Array(right)) + "]}"

      case Or(left, right) =>
        logger.info(s"Or($left,$right)")
        v2Expr = v2Expr + s"{'$$or': [" + buildV2QueryFromSqlFilters(Array(left)) + "," +
          buildV2QueryFromSqlFilters(Array(right)) + "]}"

      case EqualTo(attr, value) =>
        logger.info(s"EqualTo($attr,$value)")
        return s"{'$$eq': {'type': 'string', 'column': '$attr', 'value': '$value'}}"

      case LessThan(attr, value) =>
        logger.info(s"LessThan($attr,$value)")
        return s"{'$$lt': {'type': 'string', 'column': '$attr', 'value': '$value'}}"

      case LessThanOrEqual(attr, value) =>
        logger.info(s"LessThanEqualTo($attr,$value)")
        return s"{'$$lte': {'type': 'string', 'column': '$attr', 'value': '$value'}}"

      case GreaterThan(attr, value) =>
        logger.info(s"GreaterThan($attr,$value)")
        return s"{'$$gt': {'type': 'string', 'column': '$attr', 'value': '$value'}}"

      case GreaterThanOrEqual(attr, value) =>
        logger.info(s"GreaterThanEqualTo($attr,$value)")
        return s"{'$$gte': {'type': 'string', 'column': '$attr', 'value': '$value'}}"

      case flt => logger.info(s"Unsupported filter: $flt")
    }
    v2Expr
  }

  def getSchemaInfo(): Seq[(String, String)] = {
    val spark = SparkSession.builder().sparkContext(sqlContext.sparkContext).getOrCreate()
    val context = new Context(Array(
      "--zookeepers", spark.conf.get("spark.zookeepers"),
      "--accumulo-instance", spark.conf.get("spark.instance")))
    println(s"Config Schema Info: ${context.getConfig.toString}")
    context.connect()

    val auths = parameters(ScanAuthProp)
    val spec = DataScanSpec.fromJson(parameters(ScanSpecProp))
    val inst = new ZooKeeperInstance(context.getConfig.accumuloInstance, context.getConfig.zookeepers)
    val conn = inst.getConnector(context.getConfig.accumuloUser, new PasswordToken(context.getConfig.accumuloPassword))
    logger.info(s"authorizations: ${context.getAuthorizations.toString}")

    val scanAuthorizations = new Authorizations(auths.split(",").toList.map(_.getBytes()))
    context.setAuthorizations(scanAuthorizations)

    val scanner = conn.createScanner(context.getConfig.accumuloMetadataTable, scanAuthorizations)
    scanner.setRange(org.apache.accumulo.core.data.Range.exact("CURRENT_VERSION"))

    val currentMetadataVersion: String = scanner.iterator().next().getValue.toString
    val jsonMap = JSON.parseFull(currentMetadataVersion) match {
      case Some(map: Map[String, Any]) => map
      case None =>
        throw new IllegalStateException("failed to parse current metadata version")
    }
    val currentVersion = jsonMap("id").toString
    val metaRange = currentVersion+"\0"+spec.getDataset+"\0"+spec.getTable
    logger.info(s"meta range: $metaRange")

    scanner.setRange(org.apache.accumulo.core.data.Range.exact(metaRange))

    var cols: Seq[(String, String)] = Seq()
    val itr = scanner.iterator()
    while (itr.hasNext) {
      val kv = itr.next()
      logger.info(s"kv: ${kv.getKey} -> ${kv.getValue.toString}")
      val name = kv.getKey.getColumnQualifier.toString.split("\\x00").lift(1)
      val jsonValueMap = JSON.parseFull(kv.getValue.toString) match {
        case Some(map: Map[String, Any]) => map
        case None => throw new IllegalArgumentException("parsing value json failed")
      }
      val dataType = jsonValueMap("data_type").toString
      if (name.isDefined) {
        logger.info(s"name: ${name.get}")
        cols = cols ++ Seq(name.get -> dataType)
      }
    }
    cols
  }

  def retrieveSchemaFromMetadata(columns: Seq[(String, String)]): StructType = {
    var schema = new StructType
    for (c <- columns) {
      schema = c._2 match {
//        case "boolean" => schema.add(c._1, BooleanType, nullable = true)
//        case "double" => schema.add(c._1, DoubleType, nullable = true)
//        case "int" => schema.add(c._1, IntegerType, nullable = true)
//        case "long" => schema.add(c._1, LongType, nullable = true)
//        case "string" => schema.add(c._1, StringType, nullable = true)
        case _ => schema.add(c._1, StringType, nullable = true)
      }
    }
    logger.info(s"Schema:\n" + schema)
    schema
  }

}
