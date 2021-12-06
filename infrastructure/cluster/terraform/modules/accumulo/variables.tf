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
variable "ec2_tags_accumulo" {
  description = "Tags to apply to accumulo resources created by EC2 module"
  type        = map(string)
}

variable "ec2_tags_zookeeper" {
  description = "Tags to apply to zookeeper resources created by EC2 module"
  type        = map(string)
}

variable "volume_tags_accumulo" {
  description = "A mapping of tags to assign to the devices created by the instance at launch time"
  type        = map(string)
}

variable "volume_tags_zookeeper" {
  description = "A mapping of tags to assign to the devices created by the instance at launch time"
  type        = map(string)
}

variable "ec2_subnet" {
  description = "Subnet to start cluster in"
  type        = string
}

variable "master_ec2_instance_type" {
  description = "The type to instance to start for the masters"
  type        = string
}

variable "worker_ec2_instance_type" {
  description = "The type to instance to start for the workers"
  type        = string
}

variable "zookeeper_ec2_instance_type" {
  description = "The type to instance to start for zookeeper"
  type        = string
}

variable "ami" {
  description = "ID of the AMI to use for the instance"
  type        = string
}

variable "key_pair_name" {
  description = "The name of the key pair to access the cluster"
  type        = string
}

variable "num_masters" {
  description = "The number of masters in the cluster"
  type        = number
}

variable "num_workers" {
  description = "The number of workers in the cluster"
  type        = number
}

variable "num_zookeepers" {
  description = "The number of zookeepers in the cluster"
  type        = number
}

variable "master_root_device_size" {
  description = "The size of the master's root device in GB"
  type        = number
}

variable "master_block_device_size" {
  description = "The size of the master's block device in GB"
  type        = number
}

variable "worker_root_device_size" {
  description = "The size of the worker's root device in GB"
  type        = number
}

variable "zookeeper_root_device_size" {
  description = "The size of the zookeepers's root device in GB"
  type        = number
}

variable "worker_block_device_size" {
  description = "The size of the workers's block device in GB"
  type        = number
}

variable "vpc_security_group_ids" {
  description = "A list of security group IDs to associate with"
  type        = list(string)
}

variable "iam_instance_profile" {
  description = "The IAM Instance Profile to launch the instance with"
  type        = string
}
