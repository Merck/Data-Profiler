# Installing Data Profiler Monitoring

This helm chart installs the Data Profiler Monitoring (eg graphite, grafana) components needed before deploy time. This is a setup directory for one time components.

## Prereq:
Install;

* kubernetes
* helm 3
* optionally; a namespace

## Switch namespaces
Helm will install to your currently selected namespace. Or the `default` namespace if none were selected;
To switch your current namespace run (replace with the desired namespace)

```
kubectl config set-context --current --namespace=develop
```

## Install
To install the helm chart in your current namespace;

```
./install.sh
```

## List
To list installed helm chart in your current namespace;

```
helm ls
```
(If you do not see the helm chart, check your namespace)

```
kubectl get pv
kubectl get pvc
```

## Uninstall
To uninstall the helm chart in your current namespace;

** WARNING: If there are volumes created in this helm chart, uninstalling the chart may result in data loss!**
```
./uninstall.sh
```