# Summary
This document describes ways to create and mount volumes in the k8s cluster.

# Glossary
There are four k8s entity types to be aware of if you are thinking about creating and mounting volumes; AWS EBS or otherwise.

_Pod_ - One or more containers (docker images) running in k8s
_PersistentVolumeClaim (pvc)_ - A PersistentVolumeClaim (PVC) is a request for storage by a user
_PersistentVolume (pv)_ - A PersistentVolume (PV) is a piece of storage in the cluster that has been provisioned by an administrator or dynamically provisioned using Storage Classes
_Storage class (sc)_ - A StorageClass provides a way for administrators to describe the “classes” of storage they offer
See;
https://kubernetes.io/docs/concepts/storage/persistent-volumes/
https://kubernetes.io/docs/concepts/storage/storage-classes/

## Dynamic Provisioning

The dynamic approach to creating a volume in k8s is probably ideal. The following is an example;

Define a storage class (done only once on the cluster)
```yaml
kind: StorageClass
apiVersion: storage.k8s.io/v1
metadata:
  name: gp2-retain
  annotations:
    storageclass.kubernetes.io/is-default-class: "false"
provisioner: kubernetes.io/aws-ebs
parameters:
  type: gp2
  zone: us-east-1d
reclaimPolicy: Retain
```

Define a PersistentVolumeClaim (pvc)
```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: develop-grafana-pvc
  namespace: develop
spec:
  storageClassName: gp2-retain
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 64Gi
```
Once this is snippet is `kubectl apply`, an EBS volume will show up in the AWS console. Eventually the correct tags are labeled on the volume (I do not know how)
Notice we request a volume of a certain size. The referenced storage class also requested an EBS volume in the correct AWS Zone and EBS type.
The underlying PersistentVolume is done automatically for us by k8s

_Just to simplify things treat the following as truth…_
**Warning: deleting the pvc or its underlying pv will delete the EBS volume!**
**Warning: deleting a storage class will delete all of its referenced EBS volumes!**
**DELETING a PVC, PV, or SC could result in data loss!**

Reference a PVC in a pod. 

Define the claim in a Deployment.spec.template.spec.containers.volumes section;
```yaml
volumes:
  - name: grafana-pvc
    persistentVolumeClaim:
      claimName: develop-grafana-pvc
```

Reference the claim in the volume mount definition under Deployment.spec.template.spec.containers[0].volumeMounts

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: deployment-1
spec:
  template:
     spec:
      containers:
        - name: grafana
           image: grafana/grafana
           volumeMounts:
             - mountPath: /var/lib/grafana
               name: grafana-pvc
```


Note: When defining volumes using helm, make the volume definitions independent for the deployments or pods.  There is a reclaim policy settings of retain to allow you to delete a claim with out losing data. However, we do a helm install and uninstall on every deployment which makes it difficult to find the correct pv to attach a new claim across upgrades.

## Static Provisioning

The static approach to creating a volume in k8s is probably less ideal for your use case. See the dynamic approach for an easier alternative. The following is an example to use the static approach;

Define a storage class (done only once on the cluster)
```yaml
kind: StorageClass
apiVersion: storage.k8s.io/v1
metadata:
  name: gp2-retain
  annotations:
    storageclass.kubernetes.io/is-default-class: "false"
provisioner: kubernetes.io/aws-ebs
parameters:
  type: gp2
  zone: us-east-1d
reclaimPolicy: Retain
```

Create an EBS volume out of band from k8s tools. You can use either the AWS console or the aws cli or terraform. Verify the EBS volume is created in the correct AWS Zone and the correct Tags are applied.  From there retrieve the EBS volume id. It will be in the form of vol-123434. Once you have this volume id define a persistent volume

```yaml
kind: PersistentVolume
apiVersion: v1
metadata:
  name: sample-static-pv1
spec:
  capacity:
    storage: 12Gi
  accessModes:
    - ReadWriteOnce
  awsElasticBlockStore:
    volumeID: 
    fsType: ext4
  storageClassName: gp2
```

Notice we reference the volume id.

Next define a persistent volume claim, requesting an amount less to or equal to what is offered by the persistent volume

```yaml
kind: PersistentVolumeClaim
apiVersion: v1
metadata:
  name: sample-static-pvc1
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 12Gi
```

At this point the AWS console should show the EBS volume as ready and not available.

Finally, use the pvc in a deployment spec or pod spec

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: test-ebs
spec:
  containers:
  - image: busybox
    name: test-ebs
    volumeMounts:
    - mountPath: /data
      name: test-volume
    args:
    - sleep
    - "1000000"
  volumes:
    - name: test-volume
      persistentVolumeClaim:
       claimName: sample-static-pvc1
```

_Just to simplify things treat the following as truth…_

**DELETING a PVC, PV, or SC could result in data loss!**

