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
variable "region" {
  description = "AWS Region"
  type        = string
  default     = "us-east-1"
}

variable "availability_zone" {
  description = "AWS Availability zone"
  type        = string
  default     = "us-east-1d"
}

#
# AWS variables
#

variable "tags" {
  description = "Tags to apply to resources created by EC2 module"
  type        = map(string)
  default = {
    "dataprofiler" = "true"
  }
}

variable "vpc_id" {
  description = "ID of the VPC where to create the environment"
  type        = string
  default     = "vpc-b4bd41d3"
}

variable "ec2_subnet" {
  description = "Subnet to start cluster in"
  type        = string
  default     = "subnet-0ee14d6dc3f5512ec"
}

variable "key_pair_name" {
  description = "The name of the key pair to access the cluster"
  type        = string
  default     = "dataprofiler-cluster"
}

variable "iam_instance_profile" {
  description = "The IAM Instance Profile to launch the instance with"
  type        = string
  default     = "dataprofiler-service-role"
}

variable "ami" {
  description = "The AMI for the instances"
  type        = string
  default     = "ami-085925f297f89fce1"
}

#
# Load Balancer variables
#
variable "load_balancer_certificate_arn" {
  description = "ARN for the SSL certificate"
  type        = string
  default     = "arn:aws:iam::780502391271:server-certificate/dataprofiler-wildcard-exp-04-2022"
}

variable "load_balancer_tags" {
  description = "Tags to apply to resources in the load balancers"
  type        = map(string)
  default = {
    "dataprofilerr" = "true"
  }
}


#
# Bastion variables
#

variable "bastion_host_name" {
  description = "Host name for the bastion host"
  type        = string
  default     = "bastion"
}

variable "bastion_ec2_instance_type" {
  description = "EC2 instance type for the bastion host"
  type        = string
  default     = "c5.4xlarge"
}

variable "bastion_root_device_size" {
  description = "The size of the bastion hosts' root device in GB"
  type        = number
  default     = 1000
}

variable "bastion_tags" {
  description = "Tags to apply to resources in the bastion role"
  type        = map(string)
  default = {
    "dataprofiler_role" = "bastion"
  }
}

#
# DNS variables
#
variable "dns_host_name" {
  description = "Host name for the DNS host"
  type        = string
  default     = "dns"
}

variable "dns_ec2_instance_type" {
  description = "EC2 instance type for the primary DNS host"
  type        = string
  default     = "m5.xlarge"
}

variable "dns_root_device_size" {
  description = "The size of the primary hosts' root device in GB"
  type        = number
  default     = 50
}

variable "num_nameservers" {
  description = "The number of DNS instances"
  type        = number
  default     = 2
}

variable "use_num_suffix" {
  description = "Always append numerical suffix to instance name, even if instance_count is 1"
  type        = bool
  default     = true
}

variable "dns_tags" {
  description = "Tags to apply to resources in the dns role"
  type        = map(string)
  default = {
    "dataprofiler_role" = "dns"
  }
}

#
# Kube variables
#

variable "kube_master_ec2_instance_type" {
  description = "EC2 instance type for the kube master hosts"
  type        = string
  default     = "m5.2xlarge"
}

variable "kube_worker_ec2_instance_type" {
  description = "EC2 instance type for the kube worker hosts"
  type        = string
  default     = "r5.4xlarge"
}

variable "kube_ami" {
  description = "The AMI for the kube instances"
  type        = string
  default     = ""
}

variable "kube_key_pair_name" {
  description = "The name of the key pair to access the kube nodes"
  type        = string
  default     = "kube"
}

variable "kube_num_masters" {
  description = "The number of masters in the kube cluster"
  type        = number
  default     = 3
}

variable "kube_num_workers" {
  description = "The number of workers in the kube cluster"
  type        = number
  default     = 10
}

variable "kube_master_root_device_size" {
  description = "The size of the kube master's root device in GB"
  type        = number
  default     = 128
}

variable "kube_worker_root_device_size" {
  description = "The size of the kube worker's root device in GB"
  type        = number
  default     = 128
}

variable "kube_master_block_device_size" {
  description = "The size of the master's block device in GB"
  type        = number
  default     = 100

}

variable "kube_worker_block_device_size" {
  description = "The size of the worker's block device in GB"
  type        = number
  default     = 1000
}

variable "kube_master_tags" {
  description = "Tags to apply to the kubernetes master EC2 instances"
  type        = map(string)
  default = {
    "kubernetes.io/cluster/k8s" = "owned"
    "kubespray-role"            = "kube-master,etcd"
    "PlatformType"              = "Linux"
    "dataprofiler_role"         = "kube"
  }
}

variable "kube_worker_tags" {
  description = "Tags to apply to the kubernetes worker EC2 instances"
  type        = map(string)
  default = {
    "kubernetes.io/cluster/k8s" = "owned"
    "kubespray-role"            = "kube-node"
    "PlatformType"              = "Linux"
    "dataprofiler_role"         = "kube"
  }
}


#
# Data-processing variables
#

variable "data_processing_host_name" {
  description = "Host name for the data processing host"
  type        = string
  default     = "data-processing"
}

variable "data_processing_ec2_instance_type" {
  description = "EC2 instance type for the data processing host"
  type        = string
  default     = "c5.4xlarge"
}

variable "data_processing_root_device_size" {
  description = "The size of the data-processing hosts' root device in GB"
  type        = number
  default     = 1000
}

variable "data_processing_tags" {
  description = "Tags to apply to resources in the data-processing role"
  type        = map(string)
  default = {
    "dataprofiler_role" = "data_processing"
  }
}

#
# Spark variables
#

variable "spark_ec2_instance_type" {
  description = "EC2 instance type for spark cluster"
  type        = string
  default     = "r5.4xlarge"
}

variable "num_spark_workers" {
  description = "The number of spark workers in the spark cluster"
  type        = number
  default     = 30
}

variable "spark_master_root_device_size" {
  description = "The size of the master's root device in GB"
  type        = number
  default     = 1000
}

variable "spark_worker_root_device_size" {
  description = "The size of the workers's root device in GB"
  type        = number
  default     = 1000
}

variable "spark_tags" {
  description = "Tags to apply to resources in the spark role"
  type        = map(string)
  default = {
    "dataprofiler_role" = "spark"
  }
}

#
# Accumulo variables
#

variable "accumulo_master_ec2_instance_type" {
  description = "EC2 instance type for the Accumulo masters in"
  type        = string
  default     = "r5.xlarge"
}

variable "accumulo_worker_ec2_instance_type" {
  description = "EC2 instance type for the Accumulo workers in"
  type        = string
  default     = "r5.2xlarge"
}

variable "num_accumulo_masters" {
  description = "The number of Accumulo masters in the cluster in"
  type        = number
  default     = 2
}

variable "num_accumulo_workers" {
  description = "The number of Accumulo workers in the cluster in"
  type        = string
  default     = 30
}

variable "accumulo_master_root_device_size" {
  description = "The size of the master's root device in GB in"
  type        = number
  default     = 100
}

variable "accumulo_master_block_device_size" {
  description = "The size of the master's block device in GB in"
  type        = number
  default     = 500
}

variable "accumulo_worker_root_device_size" {
  description = "The size of the Accumulo worker's root device in GB in"
  type        = number
  default     = 100
}

variable "accumulo_worker_block_device_size" {
  description = "The size of the Accumulo workers's block device in GB in"
  type        = number
  default     = 4000
}

variable "accumulo_tags" {
  description = "Tags to apply to resources in the accumulo role"
  type        = map(string)
  default = {
    "dataprofiler_role" = "accumulo"
  }
}

variable "zookeeper_ec2_instance_type" {
  description = "EC2 instance type for the zookeepers in"
  type        = string
  default     = "c4.2xlarge"
}

variable "num_zookeepers" {
  description = "The number of zookeepers in the cluster in"
  type        = number
  default     = 3
}

variable "zookeeper_root_block_device" {
  description = "The size of the zookeepers's root device in GB in"
  type        = number
  default     = 100
}

variable "zookeeper_tags" {
  description = "Tags to apply to resources in the zookeeper role"
  type        = map(string)
  default = {
    "dataprofiler_role" = "zookeeper"
  }
}
