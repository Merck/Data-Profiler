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

from pathlib import Path
import sys
import shutil
import xml.etree.ElementTree as ET
import subprocess
import argparse
import datetime
import platform
import getpass
import shlex
import logging


class SubcommandHelpFormatter(argparse.RawDescriptionHelpFormatter):
    # TODO fix this method to remove the "Description" from the subparser
    def _format_text(self, text):
        parts = super(argparse.RawDescriptionHelpFormatter,
                      self)._format_text(text)
        if text != 'description':
            return parts

    def _format_action(self, action):
        parts = super(argparse.RawDescriptionHelpFormatter,
                      self)._format_action(action)
        if action.nargs == argparse.PARSER:
            parts = "\n".join(parts.split("\n")[1:])
        return parts


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

PYTHON_PROJECTS = [
    'python_client'
]

# The absolute path for the project
PROJECT_DIR = Path(__file__).absolute().parent

# The location of the pyton projects
PYTHON_PROJECTS = [
    PROJECT_DIR / 'python_client'
]

# The output directory for this script
LIB_DIR = PROJECT_DIR / 'lib'

# Output directories for jars
TOOL_JAR_DIR = LIB_DIR / 'tools'
ITERATOR_JAR_DIR = LIB_DIR / 'iterators'
LASTMILE_JAR_DIR = LIB_DIR / 'lastmile'

# List of jar directories
LIB_JAR_DIRS = [
    TOOL_JAR_DIR,
    ITERATOR_JAR_DIR,
    LASTMILE_JAR_DIR
]

# Output directories for python projects
PYTHON_PACKAGE_DIR = LIB_DIR / 'python_packages'

# List of project depending on python projects
PYTHON_OUTPUT_DIRS = [
    PROJECT_DIR / 'services/data-loading-daemon',
    PROJECT_DIR / 'tekton-jobs/download',
    PROJECT_DIR / 'tekton-jobs/sqlsync',
    PROJECT_DIR / 'tekton-jobs/dataset-performance',
    PROJECT_DIR / 'tekton-jobs/dataset-delta',
    PROJECT_DIR / 'tekton-jobs/dataset-quality'
]

# List of projects depending on java projects
JAR_OUTPUT_DIRS = [
    PROJECT_DIR / 'dp-api',
    PROJECT_DIR / 'infrastructure/standalone/conf/dp-accumulo'
]

JAR_OUTPUT_DIRS.extend(PYTHON_OUTPUT_DIRS)

MVN_BUILD_CMD = 'mvn clean install'

MVN_BUILD_API = '-B -DskipTests -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn'

MVN_BUILD_LOCAL = '-P local -DskipTests'


def build_project(project_dir, build_opts=''):
    cmd = shlex.split(MVN_BUILD_CMD)

    if build_opts:
        cmd.extend(shlex.split(build_opts))

    logging.debug(f'Building project with command: {"  ".join(cmd)}')
    subprocess.run(cmd, cwd=project_dir, check=True)


def get_output(cmd):
    return subprocess.run(cmd, shell=True, stdout=subprocess.PIPE).stdout.decode('utf-8').strip()


def get_git_info():
    branch = get_output('git rev-parse --abbrev-ref HEAD')
    hash = get_output('git log --pretty=format:" % H % gD" -n 1')
    if get_output('git diff --shortstat 2> /dev/null | tail -n1') == '':
        dirty = ''
    else:
        dirty = '*'

    return f'{branch} ({hash}){dirty}'


def copy_output(project_dir, output_dir):
    pom_fname = project_dir + '/pom.xml'
    artifact_name, version, src_jar_fname = pom2jar(project_dir, pom_fname)

    target_jar_name = artifact_name + '-current.jar'
    target_jar_fname = output_dir / target_jar_name
    shutil.copy(src_jar_fname, target_jar_fname)

    with open(output_dir / 'versions.txt', 'a') as fd:
        fd.write(f'{target_jar_name} ({artifact_name}) - {version}/{datetime.datetime.now().isoformat()} - {getpass.getuser()}@{platform.node()} - {get_git_info()}\n')


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

    plugins = [x.text for x in root.findall(
        'pom:build/pom:plugins/pom:plugin/pom:artifactId', ns)]

    if 'maven-assembly-plugin' in plugins:
        jar_fname = f'{project}/target/{artifact_name}-{version}-jar-with-dependencies.jar'
    else:
        jar_fname = f'{project}/target/{artifact_name}-{version}.jar'

    return artifact_name, version, jar_fname


def expand_projects(jar_dir, projects):
    return [(x, jar_dir) for x in projects]


def list_files_in_dir(dirname: Path):
    return [f for f in dirname.iterdir() if f.is_file()]


def build_python():
    logging.debug(f'Removing directory: {PYTHON_PACKAGE_DIR}')
    shutil.rmtree(PYTHON_PACKAGE_DIR, ignore_errors=True)

    logging.debug(f'Creating directory: {PYTHON_PACKAGE_DIR}')
    PYTHON_PACKAGE_DIR.mkdir(exist_ok=True, parents=True)
    for project_dir in PYTHON_PROJECTS:
        subprocess.run(['./setup.py', 'clean', '-a'], cwd=project_dir)
        subprocess.run(['./setup.py', 'bdist_wheel'], cwd=project_dir)
        wheel = list_files_in_dir(project_dir / 'dist')[0]
        logging.debug(f'Copying: {wheel} to {PYTHON_PACKAGE_DIR}')
        shutil.copy(wheel, PYTHON_PACKAGE_DIR)


def copy_python():
    wheels = list(PYTHON_PACKAGE_DIR.glob('*.whl'))

    for wheel in wheels:
        for output_dir in PYTHON_OUTPUT_DIRS:
            output_path = output_dir / 'python_packages'
            logging.debug(f'Copying: {wheel} to {output_path}')
            output_path.mkdir(exist_ok=True)
            shutil.copy(wheel, output_path)


def build_api(build_opts: str):
    # Remove lib directory
    for dir in LIB_JAR_DIRS:
        logging.debug(f'Removing directory: {dir}')
        shutil.rmtree(dir, ignore_errors=True)

    # Create lib directory
    for dir in LIB_JAR_DIRS:
        logging.debug(f'Creating directory: {dir}')
        dir.mkdir(parents=True)

    already_built_dirs = set()

    # Build the dependent Java project
    for proj in DEP_PROJECTS:
        if proj not in already_built_dirs:
            logging.debug(f'Building project: {proj}')
            build_project(proj, build_opts)
            already_built_dirs.add(proj)

    commands = expand_projects(TOOL_JAR_DIR, TOOL_PROJECTS) \
        + expand_projects(ITERATOR_JAR_DIR, ITERATOR_PROJECTS) \
        + expand_projects(LASTMILE_JAR_DIR, LASTMILE_PROJECTS)

    # Build any other java projects
    for project, output_dir in commands:
        if isinstance(project, tuple):
            project_dir, build_dir = project
        else:
            project_dir = project
            build_dir = project

        if build_dir not in already_built_dirs:
            logging.debug(f'Building project: {build_dir}')
            build_project(build_dir, build_opts)
            already_built_dirs.add(build_dir)
        copy_output(project_dir, output_dir)


def copy_api():
    jars = list(LIB_DIR.glob('**/dataprofiler*.jar'))

    for jar in jars:
        for output_dir in JAR_OUTPUT_DIRS:
            output_path = output_dir / 'data_profiler_core_jars'
            output_path.mkdir(parents=True, exist_ok=True)

            output_filename = output_path / jar.name
            logging.debug(f'Copying: {jar} to {output_filename}')
            shutil.copyfile(jar, output_filename)


def build_all(buildCmd):
    build_api(buildCmd)
    copy_api()
    build_python()
    copy_python()


def api(args):
    build_all(MVN_BUILD_API)


def local(args):
    build_all(MVN_BUILD_LOCAL)


def copy(args):
    copy_api()
    copy_python()


def python(args):
    build_python()
    copy_python()


def main():
    parser = argparse.ArgumentParser(
        description='DataProfiler uber build tool',
        usage=f'build.py [OPTION] COMMAND',
        add_help=True,
        formatter_class=SubcommandHelpFormatter)

    parser._optionals.title = 'Options'
    parser.add_argument(
        '--debug',
        default=False,
        action='store_true',
        help='Display debug messages')

    subparsers = parser.add_subparsers(
        title='Commands',
        description='description',
        metavar='metavar',
        dest='command'
    )

    parser_api = subparsers.add_parser(
        'api',
        help='Build for a remote or production environment')
    parser_api.set_defaults(func=api)

    parser_local = subparsers.add_parser(
        'local',
        help='build for the local or standalone environment')
    parser_local.set_defaults(func=local)

    # Build python libraries
    parser_python = subparsers.add_parser(
        'python',
        help='only build the python libraries')
    parser_python.set_defaults(func=python)

    parser_copy = subparsers.add_parser(
        'copy',
        help='don\'t build only copy')
    parser_copy.set_defaults(func=copy)

    args = parser.parse_args()
    if args.command is None:
        parser.print_help(sys.stderr)
        sys.exit(1)

    if args.debug:
        logging.basicConfig(level=logging.DEBUG)

    args.func(args)


if __name__ == '__main__':
    main()
