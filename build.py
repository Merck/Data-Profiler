#! /usr/bin/env python3
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

# This parses the pom files to get the version numbers that we need and then spits out the names of the
# jar files that we need

import os
import sys
import shutil
import xml.etree.ElementTree as ET
import subprocess
import argparse
import datetime
import platform
import getpass
import fnmatch

DEP_PROJECTS = [
    'dp-core'
]

TOOL_PROJECTS = [
    ('dp-core/tools', 'dp-core')
]

ITERATOR_PROJECTS = [
    ('dp-core/iterators', 'dp-core')
]

LASTMILE_PROJECTS = [
    ('dp-core/lastmile', 'dp-core')
]

PYTHON_PROJRECTS = [
    'python_client'
]

ROOT_JAR_DIR = 'lib/'
TOOL_JAR_DIR = ROOT_JAR_DIR + 'tools'
ITERATOR_JAR_DIR = ROOT_JAR_DIR + 'iterators'
LASTMILE_JAR_DIR = ROOT_JAR_DIR + 'lastmile'
PYTHON_PACKAGE_DIR = ROOT_JAR_DIR + 'python_packages'

JAR_OUTPUT_DIRS = [
    "dp-api",
    "services/data-loading-daemon",
    "tekton-jobs/download",
    "tekton-jobs/sqlsync",
    "tekton-jobs/dataset-performance",
    "tekton-jobs/dataset-delta",
    "tekton-jobs/dataset-quality"
]

def clean_output():
    shutil.rmtree(ROOT_JAR_DIR, ignore_errors=True)

def build_project(project_dir):
    subprocess.run(['mvn', 'clean', 'install', '-B',
    '-DskipTests', '-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn'],
    cwd=project_dir, check=True)

def get_output(cmd):
    return subprocess.run(cmd, shell=True, stdout=subprocess.PIPE).stdout.decode('utf-8').strip()

def get_git_info():
    branch = get_output('git rev-parse --abbrev-ref HEAD')
    hash = get_output("git log --pretty=format:'%H %gD' -n 1")
    if get_output('git diff --shortstat 2> /dev/null | tail -n1') == "":
        dirty = ""
    else:
        dirty = "*"

    return "%s (%s)%s" % (branch, hash, dirty)

def copy_output(project_dir, output_dir):
    pom_fname = project_dir + '/pom.xml'
    artiface_name, version, src_jar_fname = pom2jar(project_dir, pom_fname)

    target_jar_name = artiface_name + '-current.jar'
    target_jar_fname = output_dir + '/' + target_jar_name
    shutil.copy(src_jar_fname, target_jar_fname)

    with open(output_dir + '/versions.txt', 'a') as fd:
        fd.write('%s (%s) - %s/%s - %s@%s - %s\n' % (target_jar_name, artiface_name, version, datetime.datetime.now().isoformat(), getpass.getuser(), platform.node(), get_git_info()))

def copy_directory(src_dir, dest_dir):
    shutil.copytree(src_dir, dest_dir)

def pom2jar(project, pom_fname):
    ns = {'pom': 'http://maven.apache.org/POM/4.0.0'}
    tree = ET.parse(pom_fname)
    root = tree.getroot()
    artifact_name = root.find('pom:artifactId', ns).text
    version = root.find('pom:version', ns)
    if version is not None:
        version = version.text
    else:
        version = root.find('pom:parent/pom:version', ns).text

    plugins = [x.text for x in root.findall('pom:build/pom:plugins/pom:plugin/pom:artifactId', ns)]

    if 'maven-assembly-plugin' in plugins:
        jar_fname = "%s/target/%s-%s-jar-with-dependencies.jar" % (project, artifact_name, version)
    else:
        jar_fname = "%s/target/%s-%s.jar" % (project, artifact_name, version)

    return artifact_name, version, jar_fname

def expand_projects(jar_dir, projects):
    return [(x, jar_dir) for x in projects]

def list_files_in_dir(dirname):
    return [os.path.join(dirname, f) for f in os.listdir(dirname) if os.path.isfile(os.path.join(dirname, f))]

def build_python_project(project_dir):
    subprocess.run(['./setup.py', 'clean', '-a'], cwd=project_dir)
    subprocess.run(['./setup.py', 'bdist_wheel'], cwd=project_dir)
    return list_files_in_dir(os.path.join(project_dir, 'dist'))[0]


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='DataProfiler uber build tool')
    parser.add_argument('--just-copy', default=False, action='store_true')
    parser.add_argument('--just-python', default=False, action='store_true')
    parser.add_argument('--api-copy', default=False, action='store_true')
    args = parser.parse_args()

    clean_output()
    os.mkdir(ROOT_JAR_DIR)
    os.mkdir(TOOL_JAR_DIR)
    os.mkdir(ITERATOR_JAR_DIR)
    os.mkdir(LASTMILE_JAR_DIR)
    os.mkdir(PYTHON_PACKAGE_DIR)

    already_built_dirs = set()

    if not (args.just_copy or args.just_python):
        for d in DEP_PROJECTS:
            if d not in already_built_dirs:
                build_project(d)
                already_built_dirs.add(d)

    commands = expand_projects(TOOL_JAR_DIR, TOOL_PROJECTS) + expand_projects(ITERATOR_JAR_DIR, ITERATOR_PROJECTS) + expand_projects(LASTMILE_JAR_DIR, LASTMILE_PROJECTS)

    for project, output_dir in commands:
        if isinstance(project, tuple):
            project_dir, build_dir = project
        else:
            project_dir = project
            build_dir = project

        if not (args.just_copy or args.just_python):
            if build_dir not in already_built_dirs:
                build_project(build_dir)
                already_built_dirs.add(build_dir)
        copy_output(project_dir, output_dir)

    wheel = None
    for project_dir in PYTHON_PROJRECTS:
        wheel = build_python_project(project_dir)
        shutil.copy(wheel, PYTHON_PACKAGE_DIR)

    if (args.api_copy == True):
        matches = []
        for root, dirnames, filenames in os.walk(ROOT_JAR_DIR):
            for inputFilename in fnmatch.filter(filenames, 'dataprofiler*.jar'):
                matches.append(os.path.join(root, inputFilename))
        for inputFilename in matches:
            for output_path in JAR_OUTPUT_DIRS:
                new_path = os.path.dirname(os.path.dirname(ROOT_JAR_DIR)) + output_path + "/data_profiler_core_jars/" + \
                    inputFilename.split("/")[-1]
                shutil.copyfile(inputFilename, new_path)
        shutil.copy(wheel, 'services/data-loading-daemon/python_packages')
        shutil.copy(wheel, 'tekton-jobs/download/python_packages')
        shutil.copy(wheel, 'tekton-jobs/sqlsync/python_packages')
        shutil.copy(wheel, 'tekton-jobs/dataset-performance/python_packages')
        shutil.copy(wheel, 'tekton-jobs/dataset-delta/python_packages')
        shutil.copy(wheel, 'tekton-jobs/dataset-quality/python_packages')

    sys.exit(0)
