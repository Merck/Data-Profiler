/*
  Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
 
 	Licensed to the Apache Software Foundation (ASF) under one
 	or more contributor license agreements. See the NOTICE file
 	distributed with this work for additional information
 	regarding copyright ownership. The ASF licenses this file
 	to you under the Apache License, Version 2.0 (the
 	"License"); you may not use this file except in compliance
 	with the License. You may obtain a copy of the License at
 
 	http://www.apache.org/licenses/LICENSE-2.0
 
 
 	Unless required by applicable law or agreed to in writing,
 	software distributed under the License is distributed on an
 	"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 	KIND, either express or implied. See the License for the
 	specific language governing permissions and limitations
 	under the License.
*/

module "rds" {
  source  = "terraform-aws-modules/rds/aws"
  version = "~> 3.4"

  identifier = var.rds_name

  # All available versions: https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_PostgreSQL.html#PostgreSQL.Concepts
  engine               = "postgres"
  engine_version       = "13.3"
  family               = "postgres13" # DB parameter group
  major_engine_version = "13"         # DB option group
  instance_class       = var.rds_instance_type

  allocated_storage     = 100
  max_allocated_storage = 250
  storage_encrypted     = true

  ca_cert_identifier = var.ca_cert_identifier

  name     = var.rds_initial_db_name
  username = var.rds_username
  password = var.rds_password
  port     = 5432

  publicly_accessible   = false
  multi_az               = false
  subnet_ids             = [var.ec2_subnet]
  vpc_security_group_ids = var.vpc_security_group_ids
  db_subnet_group_name   = var.db_subnet_group_name

  maintenance_window              = "mon:00:00-mon:03:00"
  backup_window                   = "22:00-00:00"
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]

  backup_retention_period = 30
  skip_final_snapshot     = true
  deletion_protection     = true
  copy_tags_to_snapshot = true

  kms_key_id              = var.kms_key_id

  performance_insights_enabled          = true
  performance_insights_retention_period = 7
  # create_monitoring_role                = true
  # monitoring_interval                   = 60
  # monitoring_role_name                  = "example-monitoring-role-name"
  # monitoring_role_description           = "Description for monitoring role"

  #parameters = [
  #  {
  #    name  = "autovacuum"
  #    value = 1
  #  },
  #  {
  #    name  = "client_encoding"
  #    value = "utf8"
  #  }
  #]

  create_db_instance        = true
  create_db_subnet_group    = var.create_db_subnet_group
  create_db_parameter_group = var.create_db_parameter_group

  tags = var.tags


}

