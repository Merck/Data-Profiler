### Sign public key via LDAP Authentication

    muster sign-ssh-key

This results in the RSA certificate ~/.ssh/id_rsa-cert.pub saved to the user's home directory.

### Configure SSH client to allow agent forwarding

In ~/.ssh/config:

    Host bastion
            ForwardAgent yes
            HostName xxx.xxx.xxx.xxx
            User <user>
            AddKeysToAgent yes
            UseKeychain yes
            ServerAliveInterval 300
            ServerAliveCountMax 2

### Add Keys to SSH Agent

    # eval `ssh-agent`

    # ssh-add

### Login to cluster

    # ssh -A bastion

  

### Notes

The RSA Certificate binds the user to the admin-role in Vault (secrets.dataprofiler.com)

To view current extensions run the command:

    # ssh-keygen -Lf ~/.ssh/id_rsa-cert.pub

    /Users/<user>/.ssh/id_rsa-cert.pub:
            Type: ssh-rsa-cert-v01@openssh.com user certificate
            Public key: RSA-CERT SHA256:u32NirbVvIFbwz/iTiiozMv+z1t+3MJlefadnx/vw+M
            Signing CA: RSA SHA256:xEsi9J7ZccrWmorwEHqlFH8A3prnYz/QyplIVF/CNr0
            Key ID: "vault-ldap-<user>-bb7d8d8ab6d5bc815bc33fe24e28a8cccbfecf5b7edcc26579f69d9f1fefc3e3"
            Serial: 16305914310896650703
            Valid: from 2020-06-19T08:46:01 to 2020-06-19T16:46:31
            Principals: 
                    <user>
            Critical Options: (none)
            Extensions: 
                    permit-agent-forwarding
                    permit-pty

The certificate eventually expires. The current TTL is set to 8 hours.
