Initial Install

We use the docker image found in `dataprofiler/docker/nginx-ldap-auth`. That should be built and pushed to the container registry before starting the config on the kube cluster.

There are two "authentication profiles" which will each have their own webservice.

- dataprofiler-only-nginx-ldap-auth: You must be a member of the dataprofiler_team distribution list in order to login
- all-nginx-ldap-auth: Any ol'  account which successfully logs on can see this

# Setup

```
# First, setup your secrets

grep "REPLACE" all-nginx-ldap-auth.yaml
grep "REPLACE" dataprofiler-only-nginx-ldap-auth

kubectl -n ingress-nginx delete secret dataprofiler-only-nginx-ldap-auth
kubectl -n ingress-nginx delete secret all-nginx-ldap-auth

kubectl -n ingress-nginx create secret generic dataprofiler-only-nginx-ldap-auth --from-file=config.yaml=dataprofiler-only-nginx-ldap-auth.yaml
kubectl -n ingress-nginx create secret generic all-nginx-ldap-auth --from-file=config.yaml=all-nginx-ldap-auth.yaml

# Ensure you have the image pull secret (copy from production)

kubectl -n production get secret dataprofiler-registry-credentials --export -o yaml | kubectl apply --namespace=ingress-nginx -f -

# Finally...

kubectl apply -f ingress-nginx-ldap.yaml

```