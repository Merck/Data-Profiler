# Data Profiler Backups

Backups and disaster recovery in Data Profiler is accomplished in several ways:

   * HDFS Redundancy - HDFS by default stores multiple copies of each file spread through several systems in the cluster.
   Additionally, the metadata is stored in both the primary namenode and in the secondary namenode. Finally, deletes in HDFS
   are first moved to a "trash" directory before being permanently deleted. These mechanisms together provide strong
   availability and protection against data lose from system failure or accidental deletion.
   
   * S3 Backups - raw data loaded into the data profiler is often additionally stored in S3. For uploads via the API
   or user interface data is always stored in S3 first. For manually loaded data, raw data is periodically copied to
   S3.
   
   * AWS Snapshots - the primary drives of each machine in each data profiler cluster is periodically snapshotted
   using native AWS tools.
   
   * Database backups - in additionl to file system level snapshots, databases within the cluster (such as the
   rules-of-use authorization database) are dumped with database specific tools to disk.
   
# Recovery From Backups

Data recovered can be performed at several levels:

   * AWS Snapshots can be used to restore EBS volumes or create new volumes. This is done via the AWS tools.
   
   * Accidentally deleted data can be moved from the HDFS "trash" back into place.
   
   * Raw data can be copied from S3 and re-ingested. 