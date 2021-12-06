# Terraform

## Install Terraform

Terraform can be installed with brew.

    brew install terraform

Once Terraform is installed, autocomplete can be enabled by running the following command.

    terraform -install-autocomplete

## Executing Terraform

Terraform code to provision each environment is in the environments directory.

Before starting you must run `terraform init` to install any necessary plugins. Once Terraform is initialized, you can view the plan with `terraform plan`. When you are ready to make changes, execute  `terraform apply`. Be very careful with Terraform, always read the plan before performing the action. And as always, with great power comes great responsibility.

## Ansible

### Install services

The following steps can be taken to install services.

1. The jump host must be configured

        ansible-playbook -i <inventory> jump_host.yml

2. SSH to the jumpbox, checkout the code, and set up AWS credentials

        ssh -A ubuntu@<bastion_ip>

        git clone /home/git/dataprofiler.git

3. Run on the jumpbox:

        ansible-playbook -i <inventory> common.yml

4. Install HDFS, Accumulo, and Zookeeper

        ansible-playbook -i <inventory> zookeeper.yml
        ansible-playbook -i <inventory> hadoop.yml
        ansible-playbook -i <inventory> accumulo.yml

5. Install Spark

        ansible-playbook -i <inventory> spark.yml

6. Create Accumulo Tables and Deploy Iterators

        ansible-playbook  --vault-id @prompt -i <inventory> deploy_accumulo.yml

### Metrics

To install the metrics services the following command can be used

        ansible-playbook -i <inventory> --vault-id @prompt metrics.yml

## Bugs

Installation of Accumulo does not initialize Accumulo correctly. After running the installation playbook you will need to initialize accumulo by hand. On the Accumulo master, run the following as the user `accumulo`:

        "{{ accumulo_home }}/bin/accumulo init --clear-instance-name --instance-name {{ accumulo_instance }} --password {{ accumulo_password }}"

After the cluster has been initialized, the Accumulo playbook will need to be run again to start Accumulo.
