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
module "ec2_accumulo_master" {
  source = "terraform-aws-modules/ec2-instance/aws"
  version                = "~> 2.21"

  instance_count         = var.num_masters
  use_num_suffix         = true
  name                   = "accumulo-master"
  ami                    = var.ami
  instance_type          = var.master_ec2_instance_type
  vpc_security_group_ids = var.vpc_security_group_ids
  key_name               = var.key_pair_name
  subnet_id              = var.ec2_subnet
  ebs_optimized          = true
  iam_instance_profile   = var.iam_instance_profile
  tags                   = var.ec2_tags_accumulo
  volume_tags            = var.volume_tags_accumulo

  root_block_device = [
    {
      volume_type           = "gp2"
      volume_size           = var.master_root_device_size
      delete_on_termination = true
    }
  ]

  ebs_block_device = [
    {
      device_name           = "/dev/sdf"
      volume_type           = "st1"
      volume_size           = var.master_block_device_size
      delete_on_termination = true
      encrypted             = true
    }
  ]
}

module "ec2_accumulo_worker" {
  source = "terraform-aws-modules/ec2-instance/aws"
  version                = "~> 2.21"

  instance_count         = var.num_workers
  use_num_suffix         = true
  name                   = "accumulo-worker"
  ami                    = var.ami
  instance_type          = var.worker_ec2_instance_type
  vpc_security_group_ids = var.vpc_security_group_ids
  key_name               = var.key_pair_name
  subnet_id              = var.ec2_subnet
  ebs_optimized          = true
  iam_instance_profile   = var.iam_instance_profile
  tags                   = var.ec2_tags_accumulo
  volume_tags            = var.volume_tags_accumulo

  root_block_device = [
    {
      volume_type           = "gp2"
      volume_size           = var.worker_root_device_size
      delete_on_termination = true
    }
  ]

  ebs_block_device = [
    {
      device_name           = "/dev/sdf"
      volume_type           = "st1"
      volume_size           = var.worker_block_device_size
      delete_on_termination = true
      encrypted             = true
    }
  ]
}

module "ec2_zookeeper" {
  source = "terraform-aws-modules/ec2-instance/aws"
  version                = "~> 2.21"

  instance_count         = var.num_zookeepers
  use_num_suffix         = true
  name                   = "zookeeper"
  ami                    = var.ami
  instance_type          = var.zookeeper_ec2_instance_type
  vpc_security_group_ids = var.vpc_security_group_ids
  key_name               = var.key_pair_name
  subnet_id              = var.ec2_subnet
  ebs_optimized          = true
  iam_instance_profile   = var.iam_instance_profile
  tags                   = var.ec2_tags_zookeeper
  volume_tags            = var.volume_tags_zookeeper

  root_block_device = [
    {
      volume_type           = "gp2"
      volume_size           = var.zookeeper_root_device_size
      delete_on_termination = true
    }
  ]
}