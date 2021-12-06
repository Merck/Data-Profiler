# dataprofiler-secrets

This is a shared secrets manager for all clusters. It's DNS name is secrets.dataprofiler.com. The EC2 instance name is dataprofiler-secrets.

Before starting, install ansible on your local machine `brew install ansible`

Ensure that the IP address for the dataprofiler-secrets inside the EC2 cluster matches the IP in the `secrets_hosts` file.

Create an SSH key and place it under muster/configs/dataprofiler-secrets.pem.enc. You must decrypt and chmod it to 600 before running ansible.


You can run ansible by issuing:

```
$ cd infrastructure/secrets/ansible
$ ansible-playbook -i secrets_hosts secrets_update.yml
```
