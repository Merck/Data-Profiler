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
module "ec2_instance" {
  source = "terraform-aws-modules/ec2-instance/aws"
  version                = "~> 2.21"
  
  name                   = format("%s", var.host_name)
  ami                    = var.ami
  instance_type          = var.ec2_instance_type
  subnet_id              = var.ec2_subnet
  key_name               = var.key_pair_name
  vpc_security_group_ids = var.vpc_security_group_ids
  iam_instance_profile   = var.iam_instance_profile
  tags                   = var.ec2_tags
  volume_tags            = var.volume_tags
  instance_count         = var.instance_count
  use_num_suffix         = var.use_num_suffix

  root_block_device = [
    {
      volume_size = var.root_device_size
    }
  ]
}
