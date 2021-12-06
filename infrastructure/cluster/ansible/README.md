# Ansible

## Inventory

The inventory is dynamically generated from EC2 instance tags. To grab the instance tags, the AWS keys must be set with environment variables. To display the inventory the `ansible-inventory` command can be used:

    ansible-inventory -i inventory/<inventory_file>  --graph

## Playbooks

The playbooks can be run by issuing the following command:

    ansible-playbook  -i inventory/<inventory_file> <playbook>

If the playbook needs to decrypt variables in vault, the following command can be used:

    ansible-playbook  --vault-id @prompt -i inventory/<inventory> <playbook>
