# Overview

It's possible to sign in to our clusters by having Vault sign your existing
ssh keys to create a client certificate. You can then log in using your existing
key and the certificate without having to distribute your public key to every
server. This only works for servers that have been configured to accept these
client certificates, but all of the servers in the converged cluster have been
so configured.

# Process

### Create Client Certificate

    muster sign-ssh-key
    
This will use your existing ssh key (~/.ssh/id_rsa) to request a client certificate from vault.
You will prompted to authenticate with your AD password. If successful, a client
certificate will be created and saved to ~/.ssh/id_rsa-cert.pub.

Once this is done, you can ssh as normal and ssh will present this certificate as one
of the options to authenticate.

### Configure SSH Agent

If you want to use ssh agent, you can do that as normal. Start ssh agent:

    # eval `ssh-agent`

Add your key to the agent:

    # ssh-add ~/.ssh/id_rsa

Then when you ssh, the private key and the certificate will be presented.

You can also add configurations in ~/.ssh/config as normal:

    Host bastion
            ForwardAgent yes
            HostName xxx.xxx.xxx.xxx
            User <user>
            AddKeysToAgent yes
            UseKeychain yes

And login as normal:

    # ssh -A bastion
