#
# S3 location for state file: uncomment to store state file in S3
#
# terraform {
#   backend "s3" {
#     bucket  = "bucket-name"
#     key     = "terraform/tfstate"
#     region  = "us-east-1"
#     encrypt = true
#   }
# }

provider "aws" {
  region = var.region
}

#
# Latest Ubuntu AMI
#
data "aws_ami" "ubuntu" {
  most_recent = true

  owners = ["099720109477"]

  filter {
    name = "root-device-type"
    values = [
      "ebs",
    ]
  }

  filter {
    name = "architecture"
    values = [
      "x86_64",
    ]
  }

  filter {
    name = "name"
    values = [
      "ubuntu/images/hvm-ssd/ubuntu-bionic-18.04-amd64-server-*",
    ]
  }
}


#
# Security groups
#
module "security_group" {
  source           = "../../modules/security-groups"
  vpc_id           = var.vpc_id
  tags             = var.tags
}


#
# Bastion host
#
module "bastion" {
  source                 = "../../modules/ec2-instance"
  ec2_instance_type      = var.bastion_ec2_instance_type
  ec2_subnet             = var.ec2_subnet
  ec2_tags               = merge(var.tags, var.bastion_tags)
  volume_tags            = merge(var.tags, var.bastion_tags)
  ami                    = var.ami != "" ? var.ami : data.aws_ami.ubuntu.id
  vpc_security_group_ids = [module.security_group.frontend_security_group_id]
  host_name              = var.bastion_host_name
  root_device_size       = var.bastion_root_device_size
  key_pair_name          = var.key_pair_name
  iam_instance_profile   = var.iam_instance_profile
}

#
# DNS hosts
#
module "dns" {
  source                 = "../../modules/ec2-instance"
  ec2_instance_type      = var.dns_ec2_instance_type
  ec2_subnet             = var.ec2_subnet
  ec2_tags               = merge(var.tags, var.dns_tags)
  volume_tags            = merge(var.tags, var.dns_tags)
  instance_count         = var.num_nameservers
  ami                    = var.ami != "" ? var.ami : data.aws_ami.ubuntu.id
  vpc_security_group_ids = [module.security_group.backend_security_group_id, module.security_group.dns_security_group_id]
  host_name              = var.dns_host_name
  root_device_size       = var.dns_root_device_size
  key_pair_name          = var.key_pair_name
  iam_instance_profile   = var.iam_instance_profile
}

#
# Kubernetes cluster definition
#
module "kubernetes" {
  source           = "../../modules/kubernetes"
  ami              = var.ami != "" ? var.kube_ami : data.aws_ami.ubuntu.id
  ec2_subnet       = var.ec2_subnet
  vpc_security_group_ids = [module.security_group.kube_ng_security_group_id]
  key_pair_name          = var.kube_key_pair_name
  iam_instance_profile   = var.iam_instance_profile
  availability_zone      = var.availability_zone

  # Master definition
  master_ec2_instance_type = var.kube_master_ec2_instance_type
  num_masters              = var.kube_num_masters
  master_root_device_size  = var.kube_master_root_device_size
  master_block_device_size = var.kube_master_block_device_size
  master_tags              = merge(var.tags, var.kube_master_tags)
  master_volume_tags       = merge(var.tags, var.kube_master_tags)


  # Worker definition
  worker_ec2_instance_type = var.kube_worker_ec2_instance_type
  num_workers              = var.kube_num_workers
  worker_root_device_size  = var.kube_worker_root_device_size
  worker_block_device_size = var.kube_worker_block_device_size
  worker_tags              = merge(var.tags, var.kube_worker_tags)
  worker_volume_tags       = merge(var.tags, var.kube_worker_tags)
}

#
# Load balancers and target groups
#
module "load_balancer" {
  source = "../../modules/load-balancers"
  vpc_id = var.vpc_id

  worker_instance_ids = module.kubernetes.worker_id
  vpc_security_group_ids_ingress_private = [
    module.security_group.lb_ingress_private_security_group_id,
    module.security_group.backend_security_group_id
  ]
  vpc_security_group_ids_ingress_public = [
    module.security_group.lb_ingress_public_security_group_id,
    module.security_group.lb_ingress_private_security_group_id,
    module.security_group.backend_security_group_id
  ]

  certificate_arn = var.load_balancer_certificate_arn
  tags            = var.load_balancer_tags
}

#
# Data processing host
#
module "data_processing" {
  source                 = "../../modules/ec2-instance"
  ec2_instance_type      = var.data_processing_ec2_instance_type
  ec2_subnet             = var.ec2_subnet
  ec2_tags               = merge(var.tags, var.data_processing_tags)
  volume_tags            = merge(var.tags, var.data_processing_tags)
  ami                    = var.ami != "" ? var.ami : data.aws_ami.ubuntu.id
  vpc_security_group_ids = [module.security_group.backend_security_group_id]
  host_name              = var.data_processing_host_name
  root_device_size       = var.data_processing_root_device_size
  key_pair_name          = var.key_pair_name
  iam_instance_profile   = var.iam_instance_profile
}

#
# Spark cluster
#
module "spark_cluster" {
  source                  = "../../modules/spark"
  ec2_instance_type       = var.spark_ec2_instance_type
  ec2_subnet              = var.ec2_subnet
  ec2_tags                = merge(var.tags, var.spark_tags)
  volume_tags             = merge(var.tags, var.spark_tags)
  ami                     = var.ami != "" ? var.ami : data.aws_ami.ubuntu.id
  vpc_security_group_ids  = [module.security_group.backend_security_group_id]
  num_workers             = var.num_spark_workers
  master_root_device_size = var.spark_master_root_device_size
  worker_root_device_size = var.spark_worker_root_device_size
  key_pair_name           = var.key_pair_name
  iam_instance_profile    = var.iam_instance_profile
}

#
# Accumulo  cluster
#
module "accumulo_cluster" {
  source = "../../modules/accumulo"

  ec2_subnet             = var.ec2_subnet
  ec2_tags_accumulo      = merge(var.tags, var.accumulo_tags)
  volume_tags_accumulo   = merge(var.tags, var.accumulo_tags)
  vpc_security_group_ids = [module.security_group.backend_security_group_id]
  ami                    = var.ami != "" ? var.ami : data.aws_ami.ubuntu.id
  key_pair_name          = var.key_pair_name
  iam_instance_profile   = var.iam_instance_profile

  # Masters
  master_ec2_instance_type = var.accumulo_master_ec2_instance_type
  num_masters              = var.num_accumulo_masters
  master_root_device_size  = var.accumulo_master_root_device_size
  master_block_device_size = var.accumulo_master_block_device_size

  # Workers
  worker_ec2_instance_type = var.accumulo_worker_ec2_instance_type
  num_workers              = var.num_accumulo_workers
  worker_root_device_size  = var.accumulo_worker_root_device_size
  worker_block_device_size = var.accumulo_worker_block_device_size

  # Zookeepers
  zookeeper_ec2_instance_type = var.zookeeper_ec2_instance_type
  num_zookeepers              = var.num_zookeepers
  ec2_tags_zookeeper          = merge(var.tags, var.zookeeper_tags)
  volume_tags_zookeeper       = merge(var.tags, var.zookeeper_tags)
  zookeeper_root_device_size  = var.zookeeper_root_block_device
}
