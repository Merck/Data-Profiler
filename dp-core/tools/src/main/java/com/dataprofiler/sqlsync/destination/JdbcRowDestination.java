package com.dataprofiler.sqlsync.destination;

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

import static java.lang.String.format;
import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.dataprofiler.sqlsync.provider.conversion.SparkToPostgresTypeConversionProvider;
import com.dataprofiler.sqlsync.provider.conversion.SparkToRedshiftTypeConversionProvider;
import com.dataprofiler.sqlsync.provider.validation.PostgresValidationRulesProvider;
import com.dataprofiler.sqlsync.provider.validation.ValidatonRulesProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import org.apache.spark.sql.DataFrameWriter;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.collection.Iterator;

public class JdbcRowDestination implements RowDestination {
  private static final Logger logger = LoggerFactory.getLogger(JdbcRowDestination.class);
  private static final String EXPORT_SYMBOL = "\u2192";

  protected SaveMode saveMode = DEFAULT_SAVE_MODE;
  protected String table;
  //  protected String schemaName;
  protected String url;
  protected String user;
  protected String passwd;
  protected int repartition = -1;
  protected int coalesce = -1;
  protected ValidatonRulesProvider validatonRulesProvider;
  protected SparkToPostgresTypeConversionProvider sparkTypeConversionProvider;

  public JdbcRowDestination(String url, String table, String user, String passwd) {
    super();
    this.url = url;
    //    this.schemaName = schemaName;
    this.table = table;
    this.user = user;
    this.passwd = passwd;
    if (!isPostgresUrl(url) && !isRedshiftUrl(url)) {
      throw new IllegalArgumentException(
          format("url: %s is not a redshift or postgres url unable to process", url));
    }
    if (isPostgresUrl(url)) {
      validatonRulesProvider = new PostgresValidationRulesProvider();
      sparkTypeConversionProvider = new SparkToRedshiftTypeConversionProvider();
    }

    if (isRedshiftUrl(url)) {
      validatonRulesProvider = new PostgresValidationRulesProvider();
      sparkTypeConversionProvider = new SparkToRedshiftTypeConversionProvider();
    }
  }

  public void write(Dataset<Row> rows, String dest) {
    //    String[] tuple = dest.split("\\.");
    //    this.schemaName = tuple[0];
    //    this.table = tuple[1];
    this.table = dest;
    write(rows);
  }

  public void write(Dataset<Row> rows) {
    Instant start = now();
    if (isNull(rows) || rows.isEmpty()) {
      Instant end = now();
      Duration duration = between(start, end);
      if (logger.isInfoEnabled()) {
        logger.info(format("%s no rows to write! time: %s", EXPORT_SYMBOL, duration));
      }
      return;
    }
    checkConnectionInfo();
    rows = transformColumnNames(rows);
    printHead(rows);
    if (repartition > 0) {
      rows = rows.repartition(repartition);
    }
    if (coalesce > 0) {
      rows = rows.coalesce(coalesce);
    }

    //    String schemaTable = format("\"%s.%s\"", schemaName, table);
    //    logger.info("schemaTable:" + schemaTable);
    String quoteEscapedTable = format("\"%s\"", table);
    logger.info("table:" + quoteEscapedTable);
    //    String partitionColumnName = "cbsa_code";
    Properties props = new Properties();
    if (nonNull(user) && !user.isEmpty()) {
      // user could be set in the jdbc connection url
      props.put("user", user);
    }
    if (nonNull(passwd) && !passwd.isEmpty()) {
      // password could be set in the jdbc connection url
      props.put("password", passwd);
    }
    props.put("batchsize", "1000");
    //    props.put("numPartitions", "1");
    //    props.put("partitionColumn", "cbsa_code");
    //    props.put("lowerBound", "0");
    //    props.put("upperBound", "999999999");
    //    String schema = "cbsa_code INT, cbsa_title VARCHAR(1024), " +
    //        "csa_code INT, csa_title VARCHAR(1024), " +
    //        "central_outlying_county VARCHAR(32), county_county_equivalent VARCHAR(64), " +
    //        "fips_county_code INT, fips_state_code INT, " +
    //        "metro_division_code INT, metropolitan_division_title VARCHAR(256), " +
    //        "metropolitan_micropolitan_statistical_area VARCHAR(32), state_name VARCHAR(256)";
    String schema = buildTableCreateSchema(rows);
    if (logger.isInfoEnabled()) {
      logger.info("schema:" + schema);
    }

    String driver = jdbcDriver(url);
    if (logger.isInfoEnabled()) {
      logger.info("writing to jdbc destination");
      logger.info(format("driver: %s", driver));
      logger.info(format("url: %s", url));
      logger.info(format("properties: %s", props));
      logger.info(format("table: %s", quoteEscapedTable));
    }

    DataFrameWriter<Row> writer = rows.write();
    if (isRedshiftUrl(url)) {
      writer = configureRedshiftDataframeWriter(writer);
    }
    if (isRedshiftIamUrl(url)) {
      writer = configureRedshiftIAMDataframeWriter(writer);
    }
    writer
        .mode(saveMode)
        .option("dbtable", quoteEscapedTable)
        .option("driver", driver)
        .option("createTableColumnTypes", schema)
        .jdbc(url, quoteEscapedTable, props);

    Instant end = now();
    Duration duration = between(start, end);
    if (logger.isInfoEnabled()) {
      logger.info(format("%s finished writing export time: %s", EXPORT_SYMBOL, duration));
    }
  }

  /**
   * TODO: remove this code if not used
   *
   * @todo remove this code if not used
   * @deprecated
   * @see "https://docs.databricks.com/data/data-sources/aws/amazon-redshift.html"
   * @param writer
   * @return
   */
  @Deprecated
  protected DataFrameWriter<Row> configureRedshiftDataframeWriter(DataFrameWriter writer) {
    if (isNull(writer)) {
      return null;
    }
    String format = "com.databricks.spark.redshift";
    if (logger.isInfoEnabled()) {
      logger.info("detected redshift url, configuring for redshift");
      logger.info(format("format: %s", format));
    }
    return writer.format(format);
  }

  /**
   * TODO: remove me if not used
   *
   * @todo remove me if not used
   * @deprecated
   * @see "https://docs.databricks.com/data/data-sources/aws/amazon-redshift.html"
   * @param writer
   * @return
   */
  @Deprecated
  protected DataFrameWriter<Row> configureRedshiftIAMDataframeWriter(DataFrameWriter writer) {
    if (isNull(writer)) {
      return null;
    }
    String format = "com.databricks.spark.redshift";
    //    InstanceProfileCredentialsProvider provider = new InstanceProfileCredentialsProvider();
    //    AWSSessionCredentials credentials = (AWSSessionCredentials) provider.getCredentials();
    //    String token = credentials.getSessionToken();
    //    String accessKey = credentials.getAWSAccessKeyId();
    //    String secretKey = credentials.getAWSSecretKey();
    // TODO import ARN
    String roleArn = "";
    if (logger.isInfoEnabled()) {
      logger.info("detected redshift IAM url, configuring for IAM");
      logger.info(format("role: %s", roleArn));
      //      logger.info(format("token: %s", token));
      //      logger.info(format("key: %s", accessKey));
      //      logger.info(format("secret: %s", secretKey));
    }
    writer.format(format).option("aws_iam_role", roleArn);
    //        .option("temporary_aws_access_key_id", accessKey)
    //        .option("temporary_aws_secret_access_key", secretKey)
    //        .option("temporary_aws_session_token", token);
    return writer;
  }

  /**
   * database specific jdbc driver
   *
   * @param url
   * @return
   */
  protected String jdbcDriver(String url) {
    String postgresDriver = "org.postgresql.Driver";
    if (isRedshiftUrl(url)) {
      // maybe
      // "com.amazon.redshift.jdbc42.Driver"
      // "com.amazon.redshift.jdbc.Driver"
      return "com.amazon.redshift.jdbc42.Driver";
    } else if (isPostgresUrl(url)) {
      return postgresDriver;
    } else {
      logger.warn(
          format(
              "unknown driver needed for db at url %s, attempting to use driver: %s",
              url, postgresDriver));
      return postgresDriver;
    }
  }

  protected boolean isRedshiftIamUrl(String url) {
    if (isNull(url)) {
      return false;
    }
    return url.startsWith("jdbc:redshift:iam");
  }

  protected boolean isRedshiftUrl(String url) {
    if (isNull(url)) {
      return false;
    }
    return url.startsWith("jdbc:redshift");
  }

  protected boolean isPostgresUrl(String url) {
    if (isNull(url)) {
      return false;
    }
    return url.startsWith("jdbc:postgresql");
  }

  protected String buildTableCreateSchema(Dataset<Row> rows) {
    return this.buildTableCreateSchema(rows, false);
  }

  protected String buildTableCreateSchema(Dataset<Row> rows, boolean shouldQuote) {
    if (isNull(rows) || rows.isEmpty()) {
      return "";
    }

    StructType structType = rows.schema();
    Iterator<StructField> fields = structType.iterator();
    StringBuilder builder = new StringBuilder(PostgresValidationRulesProvider.MAX_COLUMN_LEN * 2);
    int index = 0;
    String escapeQuote = "\"";
    while (fields.hasNext()) {
      if (index > 0) {
        builder.append(", ");
      }
      index++;
      StructField field = fields.next();
      String fieldName = field.name();
      DataType dataType = field.dataType();
      if (shouldQuote) {
        builder = builder.append(escapeQuote);
      }
      builder = builder.append(validatonRulesProvider.normalizeColumnName(fieldName));
      if (shouldQuote) {
        builder = builder.append(escapeQuote);
      }
      builder = builder.append(" ").append(sparkTypeConversionProvider.convertType(dataType));
    }

    return builder.toString();
  }

  protected Dataset<Row> transformColumnNames(Dataset<Row> rows) {
    if (isNull(rows)) {
      return null;
    }
    String[] columnNames = rows.columns();
    for (String columnName : columnNames) {
      String normalizedName = validatonRulesProvider.normalizeColumnName(columnName);
      if (logger.isDebugEnabled()) {
        logger.debug("renaming column: " + columnName + " to " + normalizedName);
      }
      rows = rows.withColumnRenamed(columnName, normalizedName);
    }
    return rows;
  }

  public void checkConnectionInfo() {
    if (isNull(this.url)) {
      throw new IllegalStateException("url is null");
    }
    //    if (Objects.isNull(this.schemaName)) {
    //      throw new IllegalStateException("schemaName is null");
    //    }
    if (isNull(this.table)) {
      throw new IllegalStateException("table is null");
    }
  }

  public void setRepartition(int repartition) {
    this.repartition = repartition;
  }

  public void setCoalesce(int coalesce) {
    this.coalesce = coalesce;
  }
}
