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
variable "availability_zone" {
  description = "AWS Availability zone"
  type        = string
  default     = "us-east-1d"
}

variable "vpc_id" {
  description = "ID of the VPC where to create the environment"
  type        = string
  default     = "vpc-b4bd41d3"
}

variable "vpc_security_group_ids" {
  description = "A list of security group IDs to associate with"
  type        = list(string)
  default     = []
}

variable "ec2_subnet" {
  description = "Subnet to start cluster in"
  type        = string
  default     = "default-vpc-b4bd41d3"
}

variable "rds_port" {
  description = "Port for RDS to listen on"
  type        = number
  default     = 5432
}

variable "rds_initial_db_name" {
  description = "Name for the initial database namespace"
  type        = string
  default     = "quality"
}

variable "rds_name" {
  description = "Name for the RDS database"
  type        = string
  default     = "mmd-quality"
}

variable "rds_instance_type" {
  description = "Instance type for the RDS database"
  type        = string
  default     = "db.m6g.large"
}

variable "tags" {
  description = "Tags to apply to the RDS instances"
  type        = map(string)
  default     = {}
}

variable "ca_cert_identifier" {
  description = "CA Cert identifier"
  type        = string
  default     = "rds-ca-2019"
}

variable "rds_password" {
  description = "RDS password"
  type        = string
}

variable "rds_username" {
  description = "RDS username"
  type        = string
  default     = "postgres"
}

variable "kms_key_id" {
  description = "KMS id used for encryption"
  type        = string
}

variable "db_subnet_group_name" {
  description = "rds subnet group name"
  type        = string
}


variable "create_db_subnet_group" {
  description = "create subnet group name"
  type        = bool
}

variable "create_db_parameter_group" {
  description = "create parameter group name"
  type        = bool
}