# container-registry

This is a shared container registry for all clusters. It's DNS name is container-registry.dataprofiler.com. The EC2 instance name is container-registry. It was created with `./muster raw-create-instance container-registry --keypair_name container-registry-ec2 --root_volume_size 30 --instance_type r5n.xlarge --ami_name ubuntu`

It has to sit a little outside of our cluster-specific and automated infrastructure, since we're doing very non-standard things such as "using DNS". Therefore, don't use muster to manage things.

Before starting, install ansible on your local machine `brew install ansible`

Ensure that the IP address for the container-registry inside the EC2 cluster matches the IP in the `container_registry_hosts` file.

SSH key is located in vault at `ssh-keys/dataprofiler-container-registry-ec2.pem` .

We use S3 as a storage mechanism. The config (bucket location, prefix) can be found in `ansible/roles/container_registry/templates/docker-compose.yml.j2`

You can run ansible by issuing:

    cd dataprofiler/container-registry/ansible
    ansible-playbook -i container_registry_hosts container_registry_update.yml
