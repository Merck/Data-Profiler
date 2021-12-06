# Overview

This contains both a python library for access the dataprofiler REST api and some command line tools. Within the library is
also the data loading daemon that runs data load / analysis jobs.

The command line tools and the API require you to have the right configuration (via a dataprofiler config file). Running it on
the cluster is one way to do this. `muster set-dev-env` is another way.

## Generating Distribution Archives

From within the `python_client` directory run the following command to generate distribution packages.

    python3 setup.py sdist bdist_wheel

It will create the `dist/` directory and place all distribution file in that directory.

    $ ls -R1 dist
    dataprofiler-api-1.0.tar.gz
    dataprofiler_api-1.0-py3-none-any.whl

## Deleting Data and Looking at Deleted Datasets / Tables

There are two kinds of deletes: soft deletes which simply remove the data from view and purges. The soft deletes
are the defaults. They simply remove datasets and tables from the current version of the metadata. You can do
those like so:

    $ dpversions delete-dataset mydataset

This will delete the entire dataset and all of it's tables.

A single table can be deleted similarly:

    $ dpversions delete-table mydataset some_table

If you want to purge a dataset - that is, permanently delete the data from accumulo - you can add the purge flag, 
like so:

    $ dpversions delete-dataset --purge mydataset

or

    $ dpversions delete-table --purge mydataset some_table

It's important to talk about versions at this point. Each time a table with the same name is loaded it creates a new
version. So if some_table had been loaded 4 times, it would have four versions. Each of those versions would have a
unique table id that is not normally visible. Now, at any given time, only 1 table version is "active" and that is the
version that is returned normally from the profiler API. The purge command above would simply purge all versions of a datasets.

It is also possible to purge all but the most recent version of a dataset.

    $ dpversions delete-table --purge --only-previous-versions some_table mydataset

This will purge the all previous versions, but leave the current version in place (the same
flag works on delete-dataset). This is useful to free up space.

It's also possible that to purge datasets and tables that were soft deleted; the same commands above will work fine
on soft deleted tables and datasets. Additionally, you can see all datasets, including ones that have been deleted
using the following command:

    $ dpversions list-datasets --all-versions

Note that this command takes a very long time to return and can use quite a lot of memory.

## Python API

The python module is called dataprofiler and (at this moment) only has basic coverage of the REST API. Coverage
will increase over time. It also has support for loading and saving data via pyspark, which allows data processing.

Here is a basic example showing retriving the information about a job:

    >>> import dataprofiler
    >>> a = dataprofiler.Api()
    >>> import pprint
    >>> pprint.pprint(a.get_job('ff86be27-769e-4309-a6a1-d4928c11a429'))
    {'columnProperties': DotMap(),
     'creatingUser': 'developer',
     'datasetName': None,
     'datasetProperties': DotMap(),
     'deleteBeforeReload': False,
     'delimiter': None,
     'jobId': 'ff86be27-769e-4309-a6a1-d4928c11a429',
     's3Path': None,
     'status': 'error',
     'statusMessage': 'Error loading data',
     'submissionDateTime': datetime.datetime(2019, 1, 31, 18, 29, 26, 437000),
     'tableProperties': DotMap(),
     'timestamp': 1549469606790,
     'type': 'download',
     'visibilities': None,
     'visibility': ''}

There is not API documentation right now, but dataprofiler. Api is fairly readable. The REST API docs are also viewable
in the profiler (under access control -> Static API Keys -> Manage -> API Docs).

## Authentication

The API uses static API keys stored in a credentials file. The file can be stored in `/etc/dataprofiler.credentials`

or `~/.dataprofiler/credentials` . The format is:

    {"key":"SECRET_KEY","username":"root"}

If you are using one of the systems in the cluster, credentials are already present.

## Loading data via pyspark

To use the pyspark portion of the API, you must have a Spark correctly loaded in the Python environment.
The easiest way to do this is to run on the data-processing node of the cluster and start a
shell using dataprofiler_loader as follows:

    user@data-processing-internal-development:~$ sudo su - hdfs
    hdfs@data-processing-internal-development:~$ dataprofiler_loader pyspark-shell
    Python 3.5.2 (default, Nov 12 2018, 13:43:14) 
    [GCC 5.4.0 20160609] on linux
    Type "help", "copyright", "credits" or "license" for more information.
    Setting default log level to "WARN".
    To adjust logging level use sc.setLogLevel(newLevel). For SparkR, use setLogLevel(newLevel).
    Welcome to
          ____              __
         / __/__  ___ _____/ /__
        _\ \/ _ \/ _ `/ __/  '_/
       /__ / .__/\_,_/_/ /_/\_\   version 2.3.2.3.1.0.0-78
          /_/
    
    Using Python version 3.5.2 (default, Nov 12 2018 13:43:14)
    SparkSession available as 'spark'.
    >>> from dataprofiler import pyspark_api as p
    >>> t = p.get_table(sc, 'Boston Public Schools', 'Public_Schools')
    Loading config from /etc/dataprofiler.conf
    Could not open config file ~/.dataprofiler/config
    19/07/19 21:27:36 WARN ClientConfiguration: Found no client.conf in default paths. Using default client configuration values.
    DataScanSpec{type=ROW, dataset='Boston Public Schools', table='Public_Schools', column='null', filters={}}
    >>> t.show()                                                                    
    +--------------------+-------+--------------------+-----------+-------+----------+-----+--------+----------+--------+-------------+-------------+------+--------------------+--------------------+--------+------+---+------------------+-------+-------------------+
    |             ADDRESS|BLDG_ID|           BLDG_NAME|       CITY|COMPLEX|CSP_SCH_ID|Label|OBJECTID|OBJECTID_1|      PL|      POINT_X|      POINT_Y|SCH_ID|           SCH_LABEL|            SCH_NAME|SCH_TYPE|SHARED|TLT|                 Y|ZIPCODE|                 ﻿X|
    +--------------------+-------+--------------------+-----------+-------+----------+-----+--------+----------+--------+-------------+-------------+------+--------------------+--------------------+--------+------+---+------------------+-------+-------------------+
    |   195 Leyden Street|      1|          Guild Bldg|East Boston|       |      4061|   52|       1|         1|   Grace|790128.152748|2967094.37762|  4061|               Guild|    Guild Elementary|      ES|      |  3|42.388798681073467|  02128|-71.004120575202592|
    | 343 Saratoga Street|      3|     Kennedy, P Bldg|East Boston|       |      4541|   72|       2|         2|   Grace|783027.745829|2963317.53394|  4541|          PJ Kennedy|Kennedy Patrick Elem|      ES|      |  3|42.378545298399878|  02128|-71.030480292384141|
    |   218 Marion Street|      4|           Otis Bldg|East Boston|       |      4322|  106|       3|         3|   Grace|782112.823908|2962122.05733|  4322|                Otis|     Otis Elementary|      ES|      |  3|42.375278677150646|  02128|-71.033890583919728|
    |   33 Trenton Street|      6|       Odonnell Bldg|East Boston|       |      4543|  103|       4|         4|   Grace|780994.000003|2963139.99999|  4543|           O'Donnell|O'Donnell Elementary|      ES|      |  3| 42.37808861721215|  02128|-71.038010718154226|
    |     86 White Street|      7|East Boston High ...|East Boston|       |      1070|   36|       5|         5|    Joel|781823.000004|2964189.99999|  1070|      East Boston HS|    East Boston High|      HS|      |  2| 42.38095745717991|  02128|-71.034921434255651|
    |   312 Border Street|      8| Umana / Barnes Bldg|East Boston|       |      4323|  124|       6|         6|   Grace|780367.000002|2963209.99999|  4323|       Umana Academy|       Umana Academy|     K-8|      |  3|42.378289993261447|  02128|-71.040329824559478|
    |     135 Gove Street|     10|East Boston Eec Bldg|East Boston|       |      4450|   35|       7|         7|  Marice|782062.000004|2960079.99998|  4450|     East Boston EEC|     East Boston EEC|     ELC|      |  1|42.369676022773504|  02128| -71.03411994921963|
    |  122 Cottage Street|     11|          Mckay Bldg|East Boston|       |      4360|   89|       8|         8|   Grace| 782012.09674|2959933.10018|  4360|           McKay K-8|           McKay K-8|     K-8|      |  3|42.369273675959718|  02128|-71.034307583171298|
    |  165 Webster Street|     12|          Adams Bldg|East Boston|       |      4361|    1|       9|         9|   Grace|781862.000004|2958580.00002|  4361|               Adams|    Adams Elementary|      ES|      |  3|42.365563005099283|  02128|-71.034890311047619|
    |50 Bunker Hill St...|     13|        Harvard-Kent|Charlestown|       |      4280|   55|      10|        10|   Grace|775732.999998|2962579.99999|  4280|        Harvard/Kent|   Harvard/Kent Elem|      ES|      |  3|42.376628517401734|  02129| -71.05749224520838|
    |  240 Medford Street|     14|Charlestown High ...|Charlestown|       |      1050|   21|      11|        11|Jonathan|774759.589533|2963846.89176|  1050|      Charlestown HS|    Charlestown High|      HS|      |  4|42.380118677406166|  02129|-71.061070593019494|
    |    28 Walker Street|     15|        Edwards Bldg|Charlestown|       |      2010|   38|      12|        12|   Grace|773096.999996|2963459.99999|  2010|          Edwards MS|      Edwards Middle|      MS|      |  3|42.379080355474635|  02129|-71.067231244321462|
    |    50 School Street|     16|Warren-Prescott Bldg|Charlestown|       |      4283|  128|      13|        13|   Grace|773855.716508|2963069.66485|  4283| Warren/Prescott K-8| Warren/Prescott K-8|     K-8|      |  3|42.377998676579331|  02129|-71.064430592758541|
    |   16 Charter Street|     17|          Eliot Bldg|     Boston|       |      4381|   39|      14|        14|  Marice|776866.999999|2958750.00002|  4381|           Eliot K-8|           Eliot K-8|     K-8|      |  1| 42.36610274159122|  02113|-71.053369669707493|
    |152 Arlington Street|     21|Abraham Lincoln B...|     Boston|       |      1450|  112|      15|        15|Jonathan|772661.999995|2952280.00001|  1450| Quincy Upper (6-12)| Quincy Upper School|    K-12|      |  4|42.348408293305269|  02116|-71.069050246565709|
    |885 Washington St...|     22|      Boston HS Bldg|     Boston|       |      4650|  111|      16|        16|   Grace|773779.124764|2952129.10459|  4650|  Quincy Lower (K-5)|  Quincy Lower (K-5)|    K-12|      |  3|42.347978670540776|  02111|-71.064920592332612|
    |  150 Newbury Street|     24|     Snowden Hs Bldg|     Boston|       |      1200|  116|      17|        17| Anthony|770246.740985|2953058.86974|  1200|          Snowden HS|Snowden Internati...|      HS|      |  4|42.350578670999262|  02116|-71.077970596390585|
    |    90 Warren Avenue|     26|Mckinley Mackey Bldg|     Boston|       |      1291|   90|      18|        18|   Grace|771460.937914|2950801.81198|  1291|       McKinley Elem| McKinley Elementary| Special|Shared|  3|42.344368669560012|  02116|-71.073520594017609|
    |    90 Warren Avenue|     26|Mckinley Mackey Bldg|     Boston|       |      1294|   93|      19|        19|   Grace|771460.937914|2950801.81198|  1294|McKinley S. End Acad|McKinley So. End ...| Special|Shared|  3|42.344368669560012|  02116|-71.073520594017609|
    | 70 Worcester Street|     27|         Hurley Bldg|     Boston|       |      4260|   66|      20|        20|  Marice|770330.236569|2948751.74884|  4260|          Hurley K-8|          Hurley K-8|     K-8|      |  1|42.338758668670373|  02118|-71.077740595824949|
    +--------------------+-------+--------------------+-----------+-------+----------+-----+--------+----------+--------+-------------+-------------+------+--------------------+--------------------+--------+------+---+------------------+-------+-------------------+
    only showing top 20 rows
    
    >>> t.createOrReplaceTempView("table1")
    >>> t2 = spark.sql("SELECT *from table1 LIMIT 5")
    Hive Session ID = 15ac611e-7c69-4d90-b85c-89def147aed1
    >>> t2 = spark.sql("SELECT ADDRESS from table1")
    >>> t2.show()
    +--------------------+
    |             ADDRESS|
    +--------------------+
    |   195 Leyden Street|
    | 343 Saratoga Street|
    |   218 Marion Street|
    |   33 Trenton Street|
    |     86 White Street|
    |   312 Border Street|
    |     135 Gove Street|
    |  122 Cottage Street|
    |  165 Webster Street|
    |50 Bunker Hill St...|
    |  240 Medford Street|
    |    28 Walker Street|
    |    50 School Street|
    |   16 Charter Street|
    |152 Arlington Street|
    |885 Washington St...|
    |  150 Newbury Street|
    |    90 Warren Avenue|
    |    90 Warren Avenue|
    | 70 Worcester Street|
    +--------------------+
    only showing top 20 rows

Note that this must all be done as the hdfs user (hence the `sudo su - hdfs` command).
