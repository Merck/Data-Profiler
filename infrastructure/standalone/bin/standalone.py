#!/usr/bin/env python3
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

import shutil
import subprocess
import sys
import argparse
import logging
import time
import tempfile

from typing import List, Tuple
from custom_formatter import CustomFormatter
from pathlib import Path

# Location of files
PATH_SCRIPT = Path(__file__).absolute()
PATH_STANDALONE = PATH_SCRIPT.parent.parent
PATH_INFRASTRUCTURE = PATH_SCRIPT.parent.parent.parent
PATH_DATA_PROFILER = PATH_SCRIPT.parent.parent.parent.parent

# Port Mapping Definitions
DEFAULT_PORT = '8080'
DEFAULT_URL = 'http://localhost'

NGINX_INGRESS_APP_NAME = 'ingress-nginx-controller'

CONFIGMAP_FILE = PATH_STANDALONE / 'conf/env/env-vars.yaml'

TMP_DIR_BASE = 'dataprofiler'


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


class TempDir:
    def __init__(self):
        self.name = self._find()

    def _find(self):
        dirs = sorted(Path(tempfile.gettempdir()).glob(
            f'{TMP_DIR_BASE}-*'), reverse=True)
        if not dirs:
            return None

        logger.debug(f'Found temp directory: {dirs[0]}')
        return dirs[0]

    def create(self):
        dir = Path(tempfile.gettempdir()) / \
            f'{TMP_DIR_BASE}-{time.time_ns()}'
        logger.debug(f'Creating temp directory: {dir}')
        self.name = dir
        dir.mkdir()
        return dir

    def _exists(self) -> bool:
        if self._find():
            return True
        return False

    def delete(self):
        if self.name:
            logger.debug(f'Removing temp directory: {self.name}')
            shutil.rmtree(self.name, ignore_errors=True)
        else:
            logger.debug('No temp directory to remove')


class ContainerImage:
    def __init__(self, name: str, tag: str, path: str, options=[], port=-1):
        self.name = name
        self.tag = tag
        self.path = path
        self.options = options
        self.port = port

    def __str__(self):
        return f'name: {self.name}, tag: {self.tag}, path: {self.path}, options: {self.options}, port{self.port}'

    def is_running(self) -> bool:
        cmd = ['kubectl',
               'get',
               'pods',
               '-l',
               f'app={self.name}',
               '-o',
               'jsonpath="{.items[0].status.phase}"']
        proc, _, _ = exec_cmd(cmd)

        if proc.returncode != 0:
            return False

        return True

    def wait_for_pod_ready(self):
        # get the pod name
        cmd = ['kubectl',
               'get',
               'pods',
               '-l',
               f'app={self.name}',
               '-o',
               'jsonpath="{.items[0].metadata.name}"']
        _, stdout, _ = exec_cmd(cmd)
        pod = stdout.strip('"')

        # Wait for the pod to be ready
        cmd = ['kubectl',
               'wait',
               f'pod/{pod}',
               '--for=condition=Ready',
               '--timeout=-1s']
        exec_cmd(cmd)
        logger.debug(f'Pod "{pod}" is ready')

    def build(self) -> bool:
        build_cmd = [
            'docker',
            'build',
        ]

        build_cmd.extend(self.options)

        build_cmd.extend([
            '-t',
            self.tag,
            self.path
        ])

        (proc, _, _) = exec_cmd(build_cmd)

        if proc.returncode == 0:
            logger.info(4*' ' + f'{self.name:.<70}' + 'success')
            return True

        logger.error(4*' ' + f'{self.name:.<70}' + 'failed')
        return False

    def deploy(self) -> bool:
        logger.debug(f'Deploying: {self.name}')
        # Enumerate all config files in a directory
        conf_files = list(
            Path(f'{PATH_STANDALONE}/conf/{self.name}').glob('*.yaml'))

        # Create deployment for each config
        for file in conf_files:
            file_wo_ext = file.with_suffix('').name
            deploy_cmd = [
                'kubectl',
                'create',
                '-f',
                str(file)
            ]
            (proc, _, _) = exec_cmd(deploy_cmd)

            if proc.returncode == 0:
                logger.info(4*' ' + f'{file_wo_ext:.<70}' + 'success')
            else:
                logger.error(4*' ' + f'{file_wo_ext:.<70}' + 'failed')
                return False

        self.wait_for_pod_ready()

        self.create_service()
        return True

    def create_service(self):
        logger.debug(f'Exposing component {self.name}')
        if self.port == -1:
            cmd_suffix = ['--cluster-ip=None']
        else:
            cmd_suffix = [
                '--target-port',
                f'{self.port}',
                '--type',
                'NodePort'
            ]

        expose_cmd = [
            'kubectl',
            'expose',
            'deployment',
            f'{self.name}',
            '--name',
            f'{self.name}'
        ] + cmd_suffix

        (proc, _, _) = exec_cmd(expose_cmd)
        return proc

    def terminate(self) -> bool:
        logger.debug(f'Deleting: {self.name}')
        success = True

        # Enumerate all config files in a directory
        conf_files = list(
            Path(f'{PATH_STANDALONE}/conf/{self.name}').glob('*.yaml'))

        # Delete each config
        for file in conf_files:
            file_wo_ext = file.with_suffix('').name
            delete_deploy_cmd = [
                'kubectl',
                'delete',
                '-f',
                str(file)
            ]
            (proc, _, _) = exec_cmd(delete_deploy_cmd)

            if proc.returncode == 0:
                logger.info(4*' ' + f'{file_wo_ext:.<70}' + 'success')
                if success:
                    success = True
            else:
                logger.error(4*' ' + f'{file_wo_ext:.<70}' + 'failed')
                success = False

        # delete service
        self.delete_service()

        return success

    def delete_service(self):
        # delete service
        delete_svc_cmd = [
            'kubectl',
            'delete',
            'svc',
            f'{self.name}'
        ]
        exec_cmd(delete_svc_cmd)


# Dependencies
container_java = ContainerImage(
    'java',
    'container-registry.dataprofiler.com/java',
    f'{PATH_INFRASTRUCTURE}/docker/java')

container_playframework = ContainerImage(
    'playframework-base',
    'container-registry.dataprofiler.com/playframework_base',
    f'{PATH_INFRASTRUCTURE}/docker/playframework_base')

container_hadoop = ContainerImage(
    'hadoop',
    'container-registry.dataprofiler.com/hadoop',
    f'{PATH_INFRASTRUCTURE}/docker/hadoop')

container_nodepg = ContainerImage(
    'nodepg',
    'container-registry.dataprofiler.com/nodepg',
    f'{PATH_INFRASTRUCTURE}/docker/nodepg')

container_nodeyarn = ContainerImage(
    'nodeyarn',
    'container-registry.dataprofiler.com/nodeyarn',
    f'{PATH_INFRASTRUCTURE}/docker/nodeyarn')

container_dp_spark_sql_controller = ContainerImage(
    'dp-spark-sql-controller',
    'container-registry.dataprofiler.com/spark-sql-controller',
    f'{PATH_DATA_PROFILER}/spark-sql/spark-sql-controller')


# Data Profiler components
container_dp_accumulo = ContainerImage(
    'dp-accumulo',
    'dp/accumulo',
    f'{PATH_STANDALONE}/conf/dp-accumulo')

container_dp_postgres = ContainerImage(
    'dp-postgres',
    'dp/postgres',
    f'{PATH_STANDALONE}/conf/dp-postgres')

container_dp_api = ContainerImage(
    'dp-api',
    'dp/api',
    f'{PATH_DATA_PROFILER}/dp-api')

container_dp_rou = ContainerImage(
    'dp-rou',
    'dp/rou',
    f'{PATH_DATA_PROFILER}/services/rules-of-use-api')

container_dp_data_loading = ContainerImage(
    'dp-data-loading-daemon',
    'dp/data-loading-daemon',
    f'{PATH_DATA_PROFILER}/services/data-loading-daemon')

container_dp_jobs_api = ContainerImage(
    'dp-jobs-api',
    'dp/jobs-api',
    f'{PATH_DATA_PROFILER}/services/jobs-api')

container_dp_ui = ContainerImage(
    'dp-ui',
    'dp/ui',
    f'{PATH_DATA_PROFILER}/dp-ui')

# Jobs
container_dp_rou_init = ContainerImage(
    'dp-rou-init',
    'dp/rou-init',
    f'{PATH_STANDALONE}/conf/dp-rou-init')


# Images that must be built before Data Profiler specific images can be built
dependencies = {
    container_java.name: container_java,
    container_playframework.name: container_playframework,
    container_hadoop.name: container_hadoop,
    container_nodepg.name: container_nodepg,
    container_nodeyarn.name: container_nodeyarn,
    container_dp_spark_sql_controller.name: container_dp_spark_sql_controller,
}

external_apps = {
    container_dp_postgres.name: container_dp_postgres
}

# Images specific for the Data Profiler
buildable_apps = {
    container_dp_accumulo.name: container_dp_accumulo,
    container_dp_api.name: container_dp_api,
    container_dp_rou.name: container_dp_rou,
    container_dp_data_loading.name: container_dp_data_loading,
    container_dp_jobs_api.name: container_dp_jobs_api,
    container_dp_ui.name: container_dp_ui,
}

deployable_apps = {**external_apps, **buildable_apps}

# Jobs required for the Data Profiler
jobs = {
    container_dp_rou_init.name: container_dp_rou_init,
}


logger = logging.getLogger("standalone")

# create console handler with a higher log level
console_handler = logging.StreamHandler()
console_handler.setLevel(logging.DEBUG)
console_handler.setFormatter(CustomFormatter())
logger.addHandler(console_handler)


def buildable_app_names() -> List[str]:
    components = [c for c in buildable_apps.keys()]
    components.append('all')
    return components


def deployable_app_names() -> List[str]:
    components = [c for c in deployable_apps.keys()]
    components.append('all')
    return components


def job_names() -> List[str]:
    components = [c for c in jobs.keys()]
    components.append('all')
    return components


def expose_components(port):
    close_tunnel_connections()

    # port forwards
    create_local_connection_tunnel(
        NGINX_INGRESS_APP_NAME,
        port,
        '80',
        namespace='ingress-nginx',
        type='service')


def create_local_connection_tunnel(app, host_port, container_port, namespace='default', type='deployment'):
    max_tries = 10
    tunnel_cmd = [
        'kubectl',
        'port-forward',
        '-n',
        f'{namespace}',
        f'{type}/{app}',
        f'{host_port}:{container_port}',
        '--address=0.0.0.0',
    ]

    dir = TempDir()
    if not dir.name:
        dir.create()

    output_dir = dir.name / f'{app}-port-forward'

    exec_daemon_cmd(tunnel_cmd, str(output_dir))
    success = test_connection('localhost', host_port)
    while not success and max_tries > 0:
        max_tries -= 1
        logger.debug(f'Waiting for {app} to report as \'RUNNING\'')
        time.sleep(2)
        exec_daemon_cmd(tunnel_cmd, str(output_dir))
        success = test_connection('localhost', host_port)


def close_tunnel_connections():
    # ssh tunnel connection patterns that must be closed
    patterns = [
        'minikube service --url',
        'kubectl port-forward',
        'ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=n'
    ]
    for p in patterns:
        exec_cmd(['pkill', '-f', p])


def test_connection(host, port):
    test_conn_cmd = [
        'nc', '-v', '-z', host, f'{port}'
    ]
    proc, _, _ = exec_cmd(test_conn_cmd)
    if proc.returncode == 0:
        return True
    else:
        return False


def build_jars() -> bool:
    logger.info('Building libraries')
    app = 'libraries'

    build_cmd = [
        './build.py',
        'local',
    ]

    (proc, _, _) = exec_cmd(build_cmd, cwd=PATH_DATA_PROFILER)
    if proc.returncode == 0:
        logger.info(4*' ' + f'{app:.<70}' + 'success')
        return True

    logger.error(4*' ' + f'{app:.<70}' + 'failed')
    return False


def build_deps() -> bool:
    logger.info('Building Dependencies')
    success = True
    for dep in dependencies.values():
        if not dep.build():
            success = False
    return success


def build_images(app, url, port) -> bool:

    # Set the UI URL and PORT
    container_dp_ui.options.extend(['--build-arg',
                                    f'USER_FACING_API_HTTP_PATH={url}:{port}/api',
                                    '--build-arg',
                                    f'USER_FACING_UI_HTTP_PATH={url}:{port}'])

    if app is None or app == 'all':
        success = True
        for app in buildable_apps.values():
            if not app.build():
                success = False
        return success

    else:
        return buildable_apps.get(app).build()


def build_jobs(app) -> bool:

    if app is None or app == 'all':
        success = True
        for app in jobs.values():
            if not app.build():
                success = False
        return success

    else:
        return jobs.get(app).build()


def deploy_configmap() -> bool:
    logger.info('Deploying ConfigMap')
    configmap_cmd = [
        'kubectl',
        'apply',
        '-f',
        str(CONFIGMAP_FILE),
    ]

    configmap = 'configmap'
    (proc, _, _) = exec_cmd(configmap_cmd)
    if proc.returncode == 0:
        logger.info(4*' ' + f'{configmap:.<70}' + 'success')
        return True

    logger.error(4*' ' + f'{configmap:.<70}' + 'failed')
    return False


def deploy_apps(app) -> bool:
    if app is None or app == 'all':
        success = True
        for app in deployable_apps.values():
            if not app.deploy():
                success = False
        return success
    else:
        return deployable_apps.get(app).deploy()


def execute_jobs(job) -> bool:
    if job is None or job == 'all':
        success = True
        for job in jobs.values():
            if not job.deploy():
                success = False
            return success
    else:
        return jobs.get(job).deploy()


def terminate_apps(app) -> bool:
    if app is None or app == 'all':
        success = True
        for app in deployable_apps.values():
            if not app.terminate():
                success = False

        close_tunnel_connections()
        TempDir().delete()

        return success
    else:
        return deployable_apps.get(app).terminate()


def terminate_jobs(job) -> bool:
    success = True
    if job is None or job == 'all':
        for job in jobs.values():
            if not job.terminate():
                success = False

        return success
    else:
        return jobs.get(job).terminate()


def exec_cmd(cmd, cwd=PATH_STANDALONE, show_output=True) -> Tuple[subprocess.Popen, str, str]:
    logger.debug(f'Executing command: {" ".join(cmd)}')
    try:

        stdout = ''
        stderr = ''
        with subprocess.Popen(
                cmd,
                cwd=cwd,
                text=True,
                close_fds=True,
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE) as process:

            while process.poll() is None:
                for line in process.stdout:
                    stdout += line
                    logger.debug(line.strip())

                for line in process.stderr:
                    stderr += line
                    logger.debug(line.strip())

            rc = process.poll()

            if rc == 0:
                logger.debug(f'Command executed successful: {" ".join(cmd)}')
            else:
                logger.debug(
                    f'Command exited with return code {rc}: {" ".join(cmd)}')

    except subprocess.CalledProcessError as e:
        logger.error(f'Fatal error: {e}')
        sys.exit(1)

    return (process, stdout, stderr)


def exec_daemon_cmd(cmd, file):
    logger.debug(f'Executing daemon command: {" ".join(cmd)}')
    f = open(file + '.out', 'w')
    fe = open(file + '.err', 'w')
    proc = subprocess.Popen(cmd, stdout=f, stderr=fe)
    return proc


def build(args):
    """Build application(s) for the Minikube cluster
    """

    if args.app is None and args.job is None and not args.deps and not args.jars:
        args.deps = True
        args.jars = True
        args.app = 'all'
        args.job = 'all'

    if args.jars:
        if not build_jars():
            logger.error(
                "Failed building libraries. Re-run with --debug flag for more information")
            sys.exit(1)

    if args.deps:
        if not build_deps():
            logger.error(
                "Failed building container dependencies. Re-run with --debug flag for more information")
            sys.exit(1)

    if args.app is not None:
        logger.info('Building Applications')
        if not build_images(args.app, args.url, args.port):
            logger.error(
                "Failed building data profiler images. Re-run with --debug flag for more information")
            sys.exit(1)

    if args.job is not None:
        logger.info('Building Jobs')
        if not build_jobs(args.job):
            logger.error(
                "Failed building data profiler jobs. Re-run with --debug flag for more information")
            sys.exit(1)


def deploy(args):
    """Deploy application(s) to Minikube cluster
    """

    if args.app is None and args.job is None:
        args.app = 'all'
        args.job = 'all'

    if not deploy_configmap():
        logger.error(
            "Failed to deploy ConfigMap. Re-run with --debug flag for more information")
        sys.exit(1)

    if args.app is not None:
        logger.info('Deploying Applications')
        if not deploy_apps(args.app):
            logger.error(
                "Failed to deploy apps. Re-run with --debug flag for more information")
            sys.exit(1)

    if args.job is not None:
        logger.info('Executing Jobs')
        if not execute_jobs(args.job):
            logger.error(
                "Failed to execute jobs. Re-run with --debug flag for more information")
            sys.exit(1)

    # Create tunnel connection
    expose_components(args.port)


def terminate(args):
    """Terminate application(s) on the Minikube cluster
    """
    logger.info(f'Terminating')

    if args.app is None and args.job is None:
        args.app = 'all'
        args.job = 'all'

    if args.app is not None:
        logger.info('Terminating Applications')
        if not terminate_apps(args.app):
            logger.error(
                "Failed to terminate apps. Re-run with --debug flag for more information")
    if args.job is not None:
        logger.info('Terminating Jobs')
        if not terminate_jobs(args.job):
            logger.error(
                "Failed to terminate jobs. Re-run with --debug flag for more information")


def restart(args):
    args.port = DEFAULT_PORT
    terminate(args)
    deploy(args)


def status(args=None):
    logger.info('Status')

    app = NGINX_INGRESS_APP_NAME
    dir = TempDir()

    if not dir.name:
        logger.debug('Temp directory does not exist')
        logger.info(4*' ' + f'{app:.<70}unavailable')
    else:
        output_dir = dir.name / f'{app}-port-forward.out'
        with open(output_dir, 'r') as ui_status_file:
            for line in ui_status_file:
                logger.info(line.strip())

    logger.info('Applications')

    for app in deployable_apps.values():
        status = 'up' if app.is_running() else 'down'
        logger.info(4*' ' + f'{app.name:.<70}{status}')

    logger.info('Jobs')

    for job in jobs.values():
        status = 'up' if job.is_running() else 'down'
        logger.info(4*' ' + f'{job.name:.<70}{status}')


def main():

    parser = argparse.ArgumentParser(
        description='Standalone environment for the Data Profiler',
        usage=f'standalne.py [OPTION] COMMAND',
        add_help=True,
        formatter_class=SubcommandHelpFormatter)
    parser._optionals.title = 'Options'

    parser.add_argument(
        "--debug",
        default=False,
        action="store_true")

    subparsers = parser.add_subparsers(
        dest='command',
        title='Command',
        metavar='')

    # build
    parser_build = subparsers.add_parser(
        'build',
        help='Build components')
    parser_build.add_argument(
        '--app',
        type=str,
        default=None,
        help='deployment name',
        choices=buildable_app_names())
    parser_build.add_argument(
        '--deps',
        action='store_true',
        help='build dependencies')
    parser_build.add_argument(
        '--job',
        type=str,
        default=None,
        help='job name',
        choices=job_names())
    parser_build.add_argument(
        '--jars',
        action='store_true',
        help='build jars')
    parser_build.add_argument(
        '--url',
        type=str,
        default=DEFAULT_URL,
        help=f'URL to access UI (Default: {DEFAULT_URL})')
    parser_build.add_argument(
        '--port',
        type=int,
        default=DEFAULT_PORT,
        help=f'External port to access UI (Default: {DEFAULT_PORT})')
    parser_build.set_defaults(func=build)

    # deploy
    parser_deploy = subparsers.add_parser(
        'deploy',
        help='Deploy to minikube')
    parser_deploy.add_argument(
        '--app',
        type=str,
        default=None,
        help='deployment name',
        choices=deployable_app_names())
    parser_deploy.add_argument(
        '--job',
        type=str,
        default=None,
        help='job name',
        choices=job_names())
    parser_deploy.add_argument(
        '--port',
        type=int,
        default=DEFAULT_PORT,
        help=f'External port to access UI (Default: {DEFAULT_PORT})')
    parser_deploy.set_defaults(func=deploy)

    # terminate
    parser_terminate = subparsers.add_parser(
        'terminate',
        help='Terminate the application on minikube')
    parser_terminate.add_argument(
        '--app',
        type=str,
        default=None,
        help='deployment name',
        choices=deployable_app_names())
    parser_terminate.add_argument(
        '--job',
        type=str,
        default=None,
        help='job name',
        choices=job_names())
    parser_terminate.set_defaults(func=terminate)

    # restart
    parser_restart = subparsers.add_parser(
        'restart',
        help='Restart the application in minikube')
    parser_restart.add_argument(
        '--app',
        type=str,
        default='all',
        help='deployment name',
        choices=deployable_app_names())
    parser_restart.add_argument(
        '--job',
        type=str,
        default=None,
        help='job name',
        choices=job_names())
    parser_restart.set_defaults(func=restart)

    # status
    parser_status = subparsers.add_parser(
        'status',
        help='Display the application\'s status')
    parser_status.set_defaults(func=status)

    args = parser.parse_args()
    if args.command is None:
        parser.print_help(sys.stderr)
        sys.exit(1)

    # Default log level is INFO
    logger.setLevel(logging.INFO)

    if args.debug:
        logger.setLevel(logging.DEBUG)

    args.func(args)


if __name__ == '__main__':
    main()
