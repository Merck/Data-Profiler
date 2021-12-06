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
#
# Kube Master: EC2 Instance
#
resource "aws_instance" "kubernetes_master" {
  count                  = var.num_masters
  ami                    = var.ami
  instance_type          = var.master_ec2_instance_type
  key_name               = var.key_pair_name
  subnet_id              = var.ec2_subnet
  iam_instance_profile   = var.iam_instance_profile
  vpc_security_group_ids = var.vpc_security_group_ids
  user_data              = file("${path.module}/master_user_data.sh")

  tags = merge(
    tomap({"Name" = format("kube-master-%03d", count.index + 1)}),
  var.master_tags)

}


#
# Kube Master: root volume
#
resource "aws_ebs_volume" "kubernetes_master_ebs_root_volume" {
  availability_zone = var.availability_zone
  count             = var.num_masters
  encrypted         = false
  type              = "gp2"
  size              = var.master_root_device_size

  tags = merge(
    tomap({"Name" = format("dataprofiler-kube-master-%03d", count.index + 1)}),
  var.master_volume_tags)
}

resource "aws_volume_attachment" "kubernetes_master_root_vol_attachment" {
  count       = var.num_masters
  device_name = "/dev/sda1"
  volume_id   = aws_ebs_volume.kubernetes_master_ebs_root_volume[count.index].id
  instance_id = aws_instance.kubernetes_master[count.index].id
}

#
# Kube Master: extra volume
#
resource "aws_ebs_volume" "kubernetes_master_ebs_extra_volume" {
  availability_zone = var.availability_zone
  count             = var.num_masters
  encrypted         = true
  type              = "gp2"
  size              = var.master_block_device_size

  tags = merge(
    tomap({"Name" = format("dataprofiler-kube-master-%03d", count.index + 1)}),
  var.master_volume_tags)
}
resource "aws_volume_attachment" "kubernetes_master_extra_vol_attachment" {
  count       = var.num_masters
  device_name = "/dev/xvdf"
  volume_id   = aws_ebs_volume.kubernetes_master_ebs_extra_volume[count.index].id
  instance_id = aws_instance.kubernetes_master[count.index].id
}


#
# Kube Worker: EC2 Instance
#
resource "aws_instance" "kubernetes_worker" {
  count                  = var.num_workers
  ami                    = var.ami
  instance_type          = var.worker_ec2_instance_type
  key_name               = var.key_pair_name
  subnet_id              = var.ec2_subnet
  iam_instance_profile   = var.iam_instance_profile
  vpc_security_group_ids = var.vpc_security_group_ids
  user_data              = file("${path.module}/worker_user_data.sh")


  tags = merge(
    tomap({"Name" = format("kube-worker-%03d", count.index + 1)}),
  var.worker_tags)

}


#
# Kube Worker: root volume
#
resource "aws_ebs_volume" "kubernetes_worker_ebs_root_volume" {
  availability_zone = var.availability_zone
  count             = var.num_workers
  encrypted         = false
  type              = "gp2"
  size              = 128

  tags = merge(
    tomap({"Name" = format("dataprofiler-kube-worker-%03d", count.index + 1)}),
  var.worker_volume_tags)
}

resource "aws_volume_attachment" "kubernetes_worker_root_vol_attachment" {
  count       = var.num_workers
  device_name = "/dev/sda1"
  volume_id   = aws_ebs_volume.kubernetes_worker_ebs_root_volume[count.index].id
  instance_id = aws_instance.kubernetes_worker[count.index].id
}

#
# Kube Worker: extra volume
#
resource "aws_ebs_volume" "kubernetes_worker_ebs_extra_volume" {
  availability_zone = var.availability_zone
  count             = var.num_workers
  encrypted         = true
  type              = "gp2"
  size              = 1000

  tags = merge(
    tomap({"Name" = format("dataprofiler-kube-worker-%03d", count.index + 1)}),
  var.worker_volume_tags)
}
resource "aws_volume_attachment" "kubernetes_worker_extra_vol_attachment" {
  count       = var.num_workers
  device_name = "/dev/xvdf"
  volume_id   = aws_ebs_volume.kubernetes_worker_ebs_extra_volume[count.index].id
  instance_id = aws_instance.kubernetes_worker[count.index].id
}
