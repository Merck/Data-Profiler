#
# S3 location for state file: Uncomment 
#
# terraform {
#   backend "s3" {
#     bucket  = "bucket"
#     key     = "terraform/rds.tfstate"
#     region  = "us-east-1"
#     encrypt = true
#   }
# }

provider "aws" {
  region = var.region
}

locals {
  # Security Group of the kube machines
  kube_sg = ""

  # ID of the VPC
  vpc_id = ""
  rds_port = 5432
}

resource "aws_security_group" "rds_sg" {
  name        = "rds-sg"
  description = "rds security group"
  vpc_id      = local.vpc_id

  tags = merge(
  tomap({"Name" = format("rds-sg")})
  , var.tags)

}

resource "aws_security_group_rule" "allow_kube_sg" {
  security_group_id = aws_security_group.rds_sg.id
  type              = "ingress"
  description       = "allow k8s cluster"
  from_port         = local.rds_port
  to_port           = local.rds_port
  protocol          = "all"
  source_security_group_id = local.kube_sg
}

module "rds_quality_db" {
  source = "../../../modules/rds"

  availability_zone         = var.region
  vpc_id                    = local.vpc_id
  vpc_security_group_ids = [aws_security_group.rds_sg.id]
  ec2_subnet                = ""
  db_subnet_group_name      = ""

  create_db_subnet_group    = false
  create_db_parameter_group = false

  rds_port               = local.rds_port
  rds_initial_db_name    = "lastmile"
  rds_name               = "lastmile"
  rds_instance_type      = "db.m6g.large"
  ca_cert_identifier     = "rds-ca-2019"
  rds_username           = "postgres"
  rds_password           = ""

  # aws/rds kms
  kms_key_id             = ""
  tags                   = var.tags

}

