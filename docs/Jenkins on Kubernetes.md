# Jenkins on Kubernetes

https://admin.dataprofiler.com/jenkins/configureClouds/

In case the config for the kubernetes cloud ever gets deleted

Name: kubernetes
Kubernetes URL: Blank
Kubernetes server certificate key: Blank
Disable https certificate check: Checked/True
Kubernetes Namespace: admin
WebSocket: Unchecked
Direct Connection: Unchecked
Jenkins URL: http://dp-jenkins.admin.svc.cluster.local/jenkins

# Upgrading
- Update the jenkins image name in kube/helm-charts/one-off/dp-jenkins/values.yaml
- On a node with kubectl, go to kube/helm-charts/one-off/dp-jenkins
- Ensure your kubectl is in the admin namespace `kubectl config set-context --current --namespace=admin`
- Issue `/opt/helm/bin/helm uninstall dp-jenkins`
- Issue `/opt/helm/bin/helm install dp-jenkins .`

# Notes

- All jenkins plugins are downloaded over HTTP. I sometimes have to download the raw .hpi from the mirror off vpn, and then upload it manually. 
- All the "real stuff" runs on kubernetes pods that are ephemerally, there is a "master" node that runs enqueuing the test-suite, and doing nightly backups

# Credentials

Just some commentary on where all the credentials come from

- *cluster-jenkins-git*: You have to make private key / public key pair, and then configure the bitbucket/stash repository to have read access with that key. Allows the jenkins job to checkout the code.
- *kubernetes-admin*: This is the .kube/conf config 
- *container-registry-docker-login*: username docker, password docker. allows access to our container registry
- *jenkins-vault-approle*: An app role from our hashicorp vault instance
- *converged-accumulo-cluster-ssh-agent*: The ssh key to get into the converged cluster 
