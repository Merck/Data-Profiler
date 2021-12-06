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
import requests
import time
import dpmuster.constants as constants
from dpmuster import VaultOps
from requests.auth import HTTPBasicAuth
from requests.packages.urllib3 import exceptions
from json import JSONDecodeError
from urllib.parse import urlencode
import html
import xml.etree.ElementTree as ET

requests.packages.urllib3.disable_warnings(exceptions.InsecureRequestWarning)


def display_final(basic_url, password):
    split = basic_url.split("/")
    build_no = split[-2]
    stack = split[-3]
    blue_url = "https://admin.dataprofiler.com/jenkins/blue/organizations/jenkins/{}/detail/{}/{}/pipeline".format(
        stack, stack, build_no
    )
    text = "🚀 Deploy Started 🚀\n{}\n{}\nJenkins Admin Password: {}".format(
        basic_url, blue_url, password
    )
    lines = text.splitlines()
    width = max(len(s) for s in lines) + 2
    res = ["┌" + "─" * width]
    for s in lines:
        res.append("│ " + (s + " " * width)[:width])
    res.append("└" + "─" * width)
    return "\n".join(res) + "\n"


class Jenkins:
    def __init__(self, pipeline_name, debug=False):
        self.username = "admin"
        self.jenkinsToken = VaultOps().decrypt_and_get_key(
            "non-person-accounts", "jenkinsToken"
        )
        self.adminPassword = VaultOps().decrypt_and_get_key(
            "non-person-accounts", "admin"
        )
        self.jenkinsAuth = HTTPBasicAuth(self.username, self.jenkinsToken)
        self.pipeline_name = pipeline_name
        self.debug = debug

    def reconcile_pipeline(self):
        url = "{}job/{}/config.xml".format(constants.JENKINS_PATH, self.pipeline_name)
        get_request = requests.get(url, verify=False, auth=self.jenkinsAuth)
        config = ET.fromstring(get_request.text)
        pipeline_script = config.find("./definition/script").text

        local_script = None
        fd = open("./dpmuster/jenkins-deploy/deploy.groovy")
        local_script = fd.read()
        fd.close()

        if local_script != pipeline_script:
            print(
                "Deploy Pipeline is behind for {}. Uploading new version".format(
                    self.pipeline_name
                )
            )
            config.find("./definition/script").text = local_script
            headers = {"Content-Type": "text/xml"}
            post = requests.post(
                url,
                verify=False,
                auth=self.jenkinsAuth,
                headers=headers,
                data=ET.tostring(config, encoding="utf8", method="xml"),
            )
            if post.status_code != 200:
                print("Error sending updating deploy pipeline")

    def deploy(self, namespace, revision, components, **kwargs):
        no_cache = "false"
        if kwargs.get("no_cache") is True:
            no_cache = "true"

        qp = urlencode(
            {
                "revision": revision,
                "namespace": namespace,
                "components": components,
                "username": constants.AUTH.username,
                "nocache": no_cache,
            }
        )

        url = "{}job/{}/buildWithParameters?{}".format(
            constants.JENKINS_PATH, self.pipeline_name, qp
        )

        if self.debug:
            print(url)

        post = requests.post(url, verify=False, auth=self.jenkinsAuth)
        if post.status_code == 500:
            raise Exception(
                "Error submitting job. This is typically because Jenkins not deployed this yet. Kick off a deploy from the web ui and try again"
            )
        queue_item_url = "{}api/json".format(post.headers["Location"])

        if self.debug:
            print(
                "Submitted Deploy to {}. Waiting for acceptance".format(queue_item_url)
            )

        build_res = None
        while True:
            try:
                build_res = requests.get(
                    queue_item_url,
                    verify=False,
                    auth=HTTPBasicAuth(self.username, self.jenkinsToken),
                ).json()
            except JSONDecodeError:
                pass
            if build_res and build_res.get("executable", {}).get("url"):
                break
            time.sleep(1)

        print(display_final(build_res["executable"]["url"], self.adminPassword))
        return True
