# ALL

# start/stop all services
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=start"
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=stop"

# ACCUMULO

# start/stop all accumulo processes
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=start" -t accumulo
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=stop" -t accumulo

# start/stop all accumulo master processes
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=start" -t accumulo_master_services
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=stop" -t accumulo_master_services

# start/stop accumulo masters
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=start" -t accumulo_masters
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=stop" -t accumulo_masters

# start/stop all accumulo primary master processes
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=start" -t accumulo_primary_master
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=stop" -t accumulo_primary_master

# start/stop all accumulo secondary master processes
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=start" -t accumulo_secondary_master
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=stop" -t accumulo_secondary_master

# start/stop all accumulo monitor processes
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=start" -t accumulo_monitor
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=stop" -t accumulo_monitor

# start/stop all accumulo gc processes
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=start" -t accumulo_gc
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=stop" -t accumulo_gc

# start/stop all accumulo tracer processes
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=start" -t accumulo_tracer
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=stop" -t accumulo_tracer


# HDFS

# start/stop all hdfs processes
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=start" -t hdfs
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=stop" -t hdfs

# start/stop all hdfs namenode processes
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=start" -t hdfs_master_services
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=stop" -t hdfs_master_services

# start/stop hdfs namenodes
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=start" -t hdfs_namenodes
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=stop" -t hdfs_namenodes

# start/stop hdfs primary namenode process
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=start" -t hdfs_primary_namenode
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=stop" -t hdfs_primary_namenode

# start/stop hdfs secondary namenode process
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=start" -t hdfs_secondary_namenode
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=stop" -t hdfs_secondary_namenode

# start/stop hdfs journalnode processes
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=start" -t hdfs_journalnodes
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=stop" -t hdfs_journalnodes

# start/stop hdfs zkfc processes
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=start" -t hdfs_zkfc
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=stop" -t hdfs_zkfc

# start/stop hdfs datanodes processes
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=start" -t hdfs_datanodes
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=stop" -t hdfs_datanodes


# HADOOP JOB HISTORY SERVER

# start/stop historyserver process
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=start" -t historyserver
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=stop" -t historyserver


# ZOOKEEPER

# start/stop zookeeper
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=start" -t zookeepers
ansible-playbook -i inventories/preview-aws_ec2.yml service.yml -e "action=stop" -t zookeepers
