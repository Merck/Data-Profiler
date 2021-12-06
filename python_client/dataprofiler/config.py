"""
  Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
 
 	Licensed to the Apache Software Foundation (ASF) under one
 	or more contributor license agreements. See the NOTICE file
 	distributed with this work for additional information
 	regarding copyright ownership. The ASF licenses this file
 	to you under the Apache License, Version 2.0 (the
 	"License"); you may not use this file except in compliance
 	with the License. You may obtain a copy of the License at
 
 	http://www.apache.org/licenses/LICENSE-2.0
 
 
 	Unless required by applicable law or agreed to in writing,
 	software distributed under the License is distributed on an
 	"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 	KIND, either express or implied. See the License for the
 	specific language governing permissions and limitations
 	under the License.
"""
# This is the mirror of the config we have on the Java side - it can pull it from
# either the json config files we use or environment variables for when that's
# injected into docker containers.
#

import re
import inspect
import os
import json
import requests

# Yes - this module is about 25% mucking about with different ways of formatting
# variable names. Then another 25% python meta-programming. And the rest is really
# just a list of properties on a class. It's still better than the nonsense we
# have on the Java side.


def to_camel_case(snake_str):
    parts = snake_str.split("_")
    return "".join(x.title() for x in parts)


def to_snake_case(camel_str):
    s1 = re.sub("(.)([A-Z][a-z]+)", r"\1_\2", camel_str)
    return re.sub("([a-z0-9])([A-Z])", r"\1_\2", s1).lower()


def to_kaboob_case(camel_str):
    s1 = re.sub("(.)([A-Z][a-z]+)", r"\1-\2", camel_str)
    return re.sub("([a-z0-9])([A-Z])", r"\1-\2", s1).lower()


class Config:
    STANDARD_FNAMES = ["/etc/dataprofiler.conf", "~/.dataprofiler/config"]

    def __init__(self):
        self.environment = ""
        self.zookeepers = ""
        self.accumuloInstance = ""
        self.accumuloAPITokensTable = ""
        self.accumuloColumnEntitiesTable = ""
        self.accumuloElementAliasesTable = ""
        self.accumuloCustomAnnotationsTable = ""
        self.accumuloUser = ""
        self.accumuloPassword = ""
        self.accumuloScannerThreads = ""
        self.rulesOfUseApiPath = ""
        self.rulesOfUseApiKey = ""
        self.numSamples = ""
        self.threadPoolSize = ""
        self.downloadYarnQueue = ""
        self.runSparkLocally = ""
        self.loadType = ""
        self.loaderOutputDest = ""
        self.runSparkFromPlayFramework = ""
        self.sparkIngestBaseDir = ""
        self.s3Bucket = ""
        self.hadoopNamenode1 = ""
        self.hadoopNamenode2 = ""
        self.hadoopDefaultFs = ""
        self.statsdHost = ""
        self.statsdPort = ""
        self.dataProfilerToolsJar = ""
        self.jobsApiPath = ""
        self.tableMapperApiPath = ""
        self.downloadEventListenerUrl = ""
        self.userFacingApiHttpPath = ""
        self.sqlsyncUrl = ""
        self.sqlsyncUser = ""
        self.sqlsyncPassword = ""
        self.sqlsyncEventListenerUrl = ""
        self.datasetperformanceEventListenerUrl = ""
        self.datasetdeltaEventListenerUrl = ""
        self.datasetqualityEventListenerUrl = ""
        self.matomoApiKey = ""
        self.analyticsApiSiteId = ""
        self.analyticsUiSiteId = ""
        self.cancellerEventListenerUrl = ""
        self.connectionEngineEventListenerUrl = ""

        self.from_standard_files()
        self.from_env()
        self.from_aws_metadata()

    def from_standard_files(self):
        fnames = [os.path.expanduser(p) for p in self.STANDARD_FNAMES]
        for fname in fnames:
            try:
                with open(fname) as fd:
                    self.from_config(json.load(fd))
            except FileNotFoundError:
                pass

    def __get_props(self):
        return [prop for prop, x in inspect.getmembers(self) if not "_" in prop and prop and not inspect.isroutine(x)]

    def from_env(self):
        props = self.__get_props()
        env_vars_pairs = [(prop, to_snake_case(prop).upper())
                          for prop in props]

        for prop, env_var in env_vars_pairs:
            var = os.environ.get(env_var)
            if var is not None:
                setattr(self, prop, var)

    def from_config(self, d: dict):
        for k, v in d.items():
            if hasattr(self, k):
                setattr(self, k, v)

    def from_aws_metadata(self):
        # see if we are on ec2 - got this from https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/identify_ec2_instances.html
        fname = "/sys/hypervisor/uuid"
        if not os.path.isfile(fname):
            return
        if not open(fname).read().startswith("ec2"):
            return
        base_url = ""
        ret = requests.get(base_url + "/latest/meta-data/iam/info")
        if ret.status_code != 200:
            print("aws metadata not available")
            return

        role = ret.json()["InstanceProfileArn"].split("/")[-1]
        ret = requests.get(
            base_url + "/latest/meta-data/iam/security-credentials/" + role)
        ret.raise_for_status()
        d = ret.json()
        self.awsAccessKey = d["AccessKeyId"]
        self.awsSecretKey = d["SecretAccessKey"]
        self.awsSessionToken = d["Token"]

    def to_dict(self):
        out = {}
        for p in self.__get_props():
            out[p] = getattr(self,  p)
        return out

    def to_commandline_args(self):
        out = []
        for p in self.__get_props():
            v = getattr(self, p)
            if v == "":
                continue
            out.append("--" + to_kaboob_case(p))
            out.append(v)

        return out


if __name__ == "__main__":
    c = Config()
    print(c.to_commandline_args())
