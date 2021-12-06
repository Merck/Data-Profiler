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
import subprocess
import re


def extract_git_branch():
    return subprocess.run(["git", "symbolic-ref", "HEAD"],
                          stdout=subprocess.PIPE).stdout.decode(
        "utf-8").strip().split("/")[-1]


def extract_git_commit():
    return subprocess.run(["git", "rev-parse", "HEAD"],
                          stdout=subprocess.PIPE).stdout.decode("utf-8").strip()


def extract_git_info():
    branch = extract_git_branch()
    commit = extract_git_commit()
    return "{} ({})".format(branch, commit[:7])


def check_commmit_on_origin(commit_hash):
    contains_results = subprocess.run(["git", "branch", "-r", "--contains", commit_hash],
                                      stdout=subprocess.PIPE).stdout.decode("utf-8").splitlines()

    pattern = re.compile("^origin.*")
    for result in contains_results:
        if pattern.search(result.strip()):
            return True

    raise RuntimeError(
        "Commit {} has not been pushed to origin".format(commit_hash))
