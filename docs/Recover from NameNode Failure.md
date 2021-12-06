# Recover from NameNode Failure

If you do not have HDFS Hight Availability (HA) enabled you will have to restore from a checkpoint stored on the Secondary NameNode. If the machine hosting the NameNode was lost, it is easiest to just stand up the NameNode process on an existing machine in the cluster, but if can just as easily stand up a new machine and copy the necessary jars.


## Recovering HDFS

The first step is to recover HDFS: the storage for Accumulo files.

### Stop all HDFS Services

This process will be different depending on the version of HDFS, but the general steps are:

1. Stop the NameNode.

```
su -l hdfs -c "hdfs --daemon stop namenode"
```

2.  Stop the Secondary NameNode.

```
su -l hdfs -c "hdfs --daemon stop secondarynamenode"
```

3. Stop the DataNodes.

```
su -l hdfs -c "hdfs --daemon stop datanode"
```

### Update Required Configuration Files

All of the references to the old NameNode must be updated in the following configuration files.

* core-site.xml
* hdfs-site.xml

### Create a Checkpoint Directory on the new NameNode

Create an empty directory specified in the `dfs.namenode.checkpoint.dir` configuration variable and copy the checkpoint files from the Secondary NameNode to the new NameNode.

```
$ mkdir -p /hadoop/hdfs/namesecondary
$ chown hdfs:hadoop /hadoop/hdfs/namesecondary
```
Ensrue the the checkpoint files are owned by `hdfs`.

```
chown -R hdfs:hadoop /hadoop/hdfs/namesecondary
```

### Start HDFS 

1. Start the NameNode and import the last checkpoint.

```
hdfs namenode -importCheckpoint
```

2. Start the Secondary NameNode.

```
su -l hdfs -c "hdfs --daemon start secondarynamenode"
```

3. Start the DataNodes.

```
su -l hdfs -c "hdfs --daemon start datanode"
```

### Check HDFS

Since the NameNode was lost there is a good chance HDFS will not start up cleanly. The following set of commands can be used to check the file system. I strongly recommend determing what each command does and not just copying and pasting them from this list.

* `hdfs dfsadmin -report`
* `hdfs fsck /`
* `hdfs dfsadmin -safemode leave`


## Recovering YARN

Most likely Yarn will need to be used for `distcp` and other services to move things off of the cluster. To do this the yarn configs will need to be updated. Update all references to the dead nodes in the following files:

* yarn-site.xml
* mapred-site.xml
* slaves


### Stop YARN services

The following commands can be used to stop the YARN services.

```
su -l yarn -c "/usr/hdp/current/hadoop-yarn-client/bin/yarn --daemon stop nodemanager"

su -l yarn -c "/usr/hdp/current/hadoop-yarn-client/bin/yarn --daemon stop timelineserver"

su -l yarn -c "/usr/hdp/current/hadoop-yarn-client/bin/yarn --daemon stop historyserver"

su -l yarn -c "/usr/hdp/current/hadoop-yarn-client/bin/yarn --daemon stop resourcemanager"
```


### Start YARN services

The following commands can be used to start the YARN services.

```
su -l yarn -c "/usr/hdp/current/hadoop-yarn-client/bin/yarn --daemon start nodemanager"

su -l yarn -c "/usr/hdp/current/hadoop-yarn-client/bin/yarn --daemon start timelineserver"

su -l yarn -c "/usr/hdp/current/hadoop-yarn-client/bin/yarn --daemon start historyserver"

su -l yarn -c "/usr/hdp/current/hadoop-yarn-client/bin/yarn --daemon start resourcemanager"
```


## Recovering Accumulo

Since Accumulo's data is stored in HDFS, restoring Accumulo is a matter of restoring the Accumulo services. The following config files need to be updated to reflect any changes in the hostnames of the machines running the services.

* slaves
* masters
* accumulo-site.xml


If the master has changed hosts, the following setting `instance.volumes.replacements` will need to be updated in `accumulo-site.xml`. Use the docs to configure this property correctly. This will cause the next start up to take longer as this will update the references to all of the r-files.


### Starting Accumulo

The Accumulo cluster can be started with the following command.


```
su -l accumulo -c "/usr/hdp/current/accumulo-client/bin/start-all.sh"
```

### Stopping Accumulo


The accumulo cluster can be stopped with the following command.

```
su -l accumulo -c "/usr/hdp/current/accumulo-client/bin/stop-all.sh"
``` 
