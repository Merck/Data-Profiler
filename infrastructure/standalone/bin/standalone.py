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

import subprocess
import sys
import argparse
import logging
import time


from custom_formatter import CustomFormatter
from pathlib import Path

CPUS = 4
MEMORY = '8096'


PATH_SCRIPT = Path(__file__).absolute()
PATH_STANDALONE = PATH_SCRIPT.parent.parent
PATH_INFRASTRUCTURE = PATH_SCRIPT.parent.parent.parent
PATH_DATA_PROFILER = PATH_SCRIPT.parent.parent.parent.parent


USERNAME = 'developer'
ROU_HOST = 'dp-rou'

# Port Mapping Definitions
UI_PORT = '8080'
ROU_PORT = '8081'
API_PORT = '9000'
JOBS_API_PORT = '8082'
DATA_LOADING_DAEMON_PORT = '8084'
SPARK_SQL_CONTROLLER_PORT = '7999'
SPARK_UI_PORT = '4040'


CONFIGMAP_FILE = PATH_STANDALONE / 'conf/env/env-vars.yaml'


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


class DockerImage:
    def __init__(self, name: str, tag: str, path: str, options=[], port=-1):
        self.name = name
        self.tag = tag
        self.path = path
        self.options = options
        self.port = port

    def __str__(self):
        return f'name: {self.name}, tag: {self.tag}, path: {self.path}, options: {self.options}, port{self.port}'


# Dependencies
docker_java = DockerImage(
    'java',
    'container-registry.dataprofiler.com/java',
    f'{PATH_INFRASTRUCTURE}/docker/java')

docker_playframework = DockerImage(
    'playframework-base',
    'container-registry.dataprofiler.com/playframework_base',
    f'{PATH_INFRASTRUCTURE}/docker/playframework_base')

docker_hadoop = DockerImage(
    'hadoop',
    'container-registry.dataprofiler.com/hadoop',
    f'{PATH_INFRASTRUCTURE}/docker/hadoop')

docker_nodepg = DockerImage(
    'nodepg',
    'container-registry.dataprofiler.com/nodepg',
    f'{PATH_INFRASTRUCTURE}/docker/nodepg')

docker_nodeyarn = DockerImage(
    'nodeyarn',
    'container-registry.dataprofiler.com/nodeyarn',
    f'{PATH_INFRASTRUCTURE}/docker/nodeyarn')

docker_dp_spark_sql_controller = DockerImage(
    'dp-spark-sql-controller',
    'container-registry.dataprofiler.com/spark-sql-controller',
    f'{PATH_DATA_PROFILER}/spark-sql/spark-sql-controller',
    port=SPARK_SQL_CONTROLLER_PORT)


# Backend images
docker_dp_accumulo = DockerImage(
    'dp-accumulo',
    'dp/accumulo',
    f'{PATH_STANDALONE}/conf/dp-accumulo')

docker_dp_postgres = DockerImage(
    'dp-postgres',
    'dp/postgres',
    f'{PATH_STANDALONE}/conf/dp-postgres')

docker_dp_api = DockerImage(
    'dp-api',
    'dp/api',
    f'{PATH_DATA_PROFILER}/dp-api',
    port=API_PORT)

docker_dp_rou = DockerImage(
    'dp-rou',
    'dp/rou',
    f'{PATH_DATA_PROFILER}/services/rules-of-use-api',
    port=ROU_PORT)

docker_dp_data_loading = DockerImage(
    'dp-data-loading-daemon',
    'dp/data-loading-daemon',
    f'{PATH_DATA_PROFILER}/services/data-loading-daemon',
    port=DATA_LOADING_DAEMON_PORT)

docker_dp_jobs_api = DockerImage(
    'dp-jobs-api',
    'dp/jobs-api',
    f'{PATH_DATA_PROFILER}/services/jobs-api',
    port=JOBS_API_PORT)

docker_dp_ui = DockerImage(
    'dp-ui',
    'dp/ui',
    f'{PATH_DATA_PROFILER}/dp-ui',
    ['--build-arg',
     f'USER_FACING_API_HTTP_PATH=http://localhost:{API_PORT}',
     '--build-arg',
     f'USER_FACING_UI_HTTP_PATH=http://localhost:{UI_PORT}', ])


# Images that must be built before Data Profiler specific images can be built
components_deps = {
    docker_java.name: docker_java,
    docker_playframework.name: docker_playframework,
    docker_hadoop.name: docker_hadoop,
    docker_nodepg.name: docker_nodepg,
    docker_nodeyarn.name: docker_nodeyarn,
    docker_dp_spark_sql_controller.name: docker_dp_spark_sql_controller,
}


# Backend components for the data profiler
components_backend = {
    docker_dp_accumulo.name: docker_dp_accumulo,
    docker_dp_postgres.name: docker_dp_postgres,
    docker_dp_api.name: docker_dp_api,
    docker_dp_rou.name: docker_dp_rou,
    docker_dp_data_loading.name: docker_dp_data_loading,
    docker_dp_jobs_api.name: docker_dp_jobs_api,
}

# Front end components for the data profiler
components_frontend = {
    docker_dp_ui.name: docker_dp_ui
}

components_all = {**components_backend, **components_frontend}


logger = logging.getLogger("standalone")
logger.setLevel(logging.DEBUG)
# logger.setLevel(logging.INFO)

# create console handler with a higher log level
console_handler = logging.StreamHandler()
console_handler.setLevel(logging.DEBUG)
console_handler.setFormatter(CustomFormatter())
logger.addHandler(console_handler)


def component_names():
    components = [c for c in components_all.keys()]
    components.append('all')
    return components


def configure_rou_db():
    logger.info('* Configuring Rules Of Use')

    # Create ROU key
    logger.info('  * Created api key as \'dp-rou-key\'')
    p = exec_cmd([f'{PATH_STANDALONE}/util/create_api_key.sh'])
    if not p.returncode == 0:
        logger.error('Failed to create api key')

    # Activate ROU attributes
    logger.info('  * Activating ROU attributes')
    p = exec_cmd([f'{PATH_STANDALONE}/util/activate_attributes.sh'],
                 cwd=f'{PATH_STANDALONE}/util/')
    if not p.returncode == 0:
        logger.error('Failed to activate attributes')

    # Update user ROU attributes
    logger.info('  * Updating ROU attributes for \'developer\'')
    p = exec_cmd(
        [f'{PATH_STANDALONE}/util/update_user_attributes.sh'], show_output=False)
    if not p.returncode == 0:
        logger.error('Failed to update user attributes for \'developer\'')


def display_status(component, port):
    if not test_connection('localhost', f'{port}'):
        expose_component(component)
    else:
        print(f'{component} available at http://localhost:{port}')


def expose_components():
    close_tunnel_connections()

    # backend
    for c in components_backend:
        expose_component(c)

    # port forwards
    create_local_connection_tunnel('dp-ui', f'{UI_PORT}', 80)
    create_local_connection_tunnel('dp-api', f'{API_PORT}', f'{API_PORT}')
    create_local_connection_tunnel('dp-rou', f'{ROU_PORT}', f'{ROU_PORT}')
    create_local_connection_tunnel(
        'dp-jobs-api', f'{JOBS_API_PORT}', f'{JOBS_API_PORT}')
    create_local_connection_tunnel(
        'dp-data-loading-daemon', f'{DATA_LOADING_DAEMON_PORT}', f'{DATA_LOADING_DAEMON_PORT}')


def expose_component(app, port=None):
    logger.debug(f'Exposing component {app}')
    prefix = 4*' ' + '> '
    if port is None:
        cmd_suffix = ['--cluster-ip=None']
    else:
        cmd_suffix = [
            '--target-port',
            f'{port}',
            '--type',
            'NodePort'
        ]

    expose_cmd = [
        'kubectl',
        'expose',
        'deployment',
        f'{app}',
        '--name',
        f'{app}'
    ] + cmd_suffix

    process = exec_cmd(expose_cmd, prefix=prefix)
    return process


def expose_ui(ui_app='dp-ui'):
    logger.debug(f'Exposing ui')
    service_cmd = [
        'minikube',
        'service',
        '--url',
        ui_app
    ]
    exec_daemon_cmd(service_cmd, '/tmp/dp-ui-port-forward')
    logger.debug(ui_app)


def create_local_connection_tunnel(app, host_port, container_port):
    max_tries = 10
    tunnel_cmd = [
        'kubectl',
        'port-forward',
        f'deployment/{app}',
        f'{host_port}:{container_port}',
        '--address=0.0.0.0',
    ]

    exec_daemon_cmd(tunnel_cmd, f'/tmp/{app}-port-forward')
    success = test_connection('localhost', host_port)
    while not success and max_tries > 0:
        max_tries -= 1
        logger.debug(f'Waiting for {app} to report as \'RUNNING\'')
        time.sleep(2)
        exec_daemon_cmd(tunnel_cmd, f'/tmp/{app}-port-forward')
        success = test_connection('localhost', host_port)


def build_jars():
    logger.info('Building libraries')

    build_cmd = [
        './build.py',
        'local',
    ]

    process = exec_cmd(build_cmd, cwd=PATH_DATA_PROFILER)
    return process


def build_deps():
    logger.info('Building docker dependencies')
    for docker in components_deps.values():
        logger.debug(f'Building docker image: {docker.name}')
        build_docker_image(docker)


def build_images(app='all'):
    if app is None or app == 'all':
        logger.info('Building docker images')
        for image in components_all.values():
            build_docker_image(image)

    else:
        logger.info(f'Building docker image for {app}')
        build_docker_image(components_all.get(app))


def build_docker_image(docker: DockerImage):

    logger.info(f'{docker.tag} image')

    docker_build_cmd = [
        'docker',
        'build',
    ]

    docker_build_cmd.extend(docker.options)

    docker_build_cmd.extend([
        '-t',
        docker.tag,
        docker.path
    ])

    process = exec_cmd(docker_build_cmd)
    # process = exec_cmd(docker_build_cmd, cwd=build_base)
    if not process.returncode == 0:
        logger.error(f'Failed to build {docker.tag} image. exiting')
        exit(1)


def deploy_components():
    logger.info('* Deploying Components')
    for component in components_all:
        deploy_component(component)


def deploy_component(app):
    print(4*' ' + '> deploying ' + app, end='\r')

    # indent prefix for following output
    prefix = 4*' ' + '> '

    # create deployment
    deploy_cmd = [
        'kubectl',
        'create',
        '-f',
        f'{PATH_STANDALONE}/conf/{app}/{app}.yaml'
    ]
    logger.debug(deploy_cmd)
    exec_cmd(deploy_cmd, prefix=prefix)

    if app == 'dp-ui':
        expose_component(app, 80)
    else:
        expose_component(app)

    # wait for component to report as 'RUNNING'
    app_status = check_pod_status(app)
    while not app_status.returncode == 0:
        logger.debug(f'Waiting for {app} to report as \'RUNNING\'')
        time.sleep(2)
        app_status = check_pod_status(app)

    print(' '*100, end='\r')
    logger.info(4*' ' + f'{app:.<35}' + 'deployed')


def terminate_component(app):
    logger.debug(4*' Deleting ' + app)

    # indent prefix for following output
    prefix = 6*' ' + ' '

    # delete deployment
    delete_deploy_cmd = [
        'kubectl',
        'delete',
        '-f',
        f'{PATH_STANDALONE}/conf/{app}/{app}.yaml'
    ]
    logger.debug(delete_deploy_cmd)
    exec_cmd(delete_deploy_cmd, prefix=prefix)

    # delete service
    delete_svc_cmd = [
        'kubectl',
        'delete',
        'svc',
        f'{app}'
    ]
    logger.debug(delete_svc_cmd)
    exec_cmd(delete_svc_cmd, prefix=prefix)

    print(' '*100, end='\r')
    logger.info(4*' ' + f'{app:.<35}' + 'deleted')


def check_minikube_status():
    process = exec_cmd(['minikube', 'status'], show_output=False)
    if not process.returncode == 0:
        logger.debug(f'Minikube is not running. attempting to start...')

        restart_cmd = [
            'minikube',
            'start',
            '--cpus',
            f'{CPUS}',
            '--memory',
            f'{MEMORY}']

        process = exec_cmd(restart_cmd)
    if not process.returncode == 0:
        logger.error('Failed to start minikube - exiting')
        exit(1)
    else:
        logger.debug(f'Minikube is running')
    return process


def check_pod_status(app):
    process = exec_cmd([f'{PATH_STANDALONE}/util/check_pod_status.sh', app])
    return process


def check_requirements():
    required = ['minikube', 'kubectl']
    for req in required:
        if not is_available(req):
            logger.info(f'Missing requirement -- please install \'{req}\'')
            exit(1)


def exec_cmd(cmd, cwd=PATH_STANDALONE, prefix='', show_output=True):
    logger.debug(f'Executing command: {" ".join(cmd)}')
    try:

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
                    logger.debug(line.strip())

                for line in process.stderr:
                    logger.debug(line.strip())

            rc = process.poll()

            if rc == 0:
                logger.debug(f'Command executed successful: {" ".join(cmd)}')
            else:
                logger.error(
                    f'Command exited with return code {rc}: {" ".join(cmd)}')

    except subprocess.CalledProcessError as e:
        logger.error(f'Fatal error: {e}')
        sys.exit(1)

    return process


def exec_daemon_cmd(cmd, file):
    logger.debug(f'Executing daemon command: {" ".join(cmd)}')
    f = open(file + '.out', 'w')
    fe = open(file + '.err', 'w')
    process = subprocess.Popen(cmd, stdout=f, stderr=fe)
    return process


def is_available(name):
    process = exec_cmd(['which', name], show_output=False)
    return process.returncode == 0


def parse_args(parser, commands):
    # Divide argv by commands
    split_argv = [[]]
    for c in sys.argv[1:]:
        if c in commands.choices:
            split_argv.append([c])
        else:
            split_argv[-1].append(c)
    # Initialize namespace
    args = argparse.Namespace()
    for c in commands.choices:
        setattr(args, c, None)
        # Parse each command
        parser.parse_args(split_argv[0], namespace=args)  # Without command
        for argv in split_argv[1:]:  # Commands
            n = argparse.Namespace()
            setattr(args, argv[0], n)
            parser.parse_args(argv, namespace=n)
    return args


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
    process = exec_cmd(test_conn_cmd)
    if process.returncode == 0:
        return True
    else:
        return False


def check_error(process, msg=None):
    if not process.returncode == 0:
        if msg:
            logger.error(msg)
        raise Exception(f'Command failed with {process.returncode}')


def check_minikube_environment():
    p = exec_cmd(['minikube', 'docker-env'])
    if not p.returncode == 0:
        print('Minikube environment not set. Please run: eval `minikube docker-env`')


def build(args):

    if args.app is None and not args.deps and not args.jars:
        args.deps = True
        args.jars = True
        args.app = 'all'

    if args.jars:
        build_jars()

    if args.deps:
        print('build deps')
        build_deps()

    if args.app is not None:
        print('build apps')
        build_images(args.app)


# Deploy application(s) to Minikube cluster
def deploy(args):

    # indent prefix for following output
    prefix = 4*' ' + '> '
    # create configmap containing cluster data
    configmap_cmd = [
        'kubectl',
        'apply',
        '-f',
        str(CONFIGMAP_FILE),
    ]
    logger.debug(configmap_cmd)
    exec_cmd(configmap_cmd, prefix=prefix)

    app = args.app
    if not app == 'all':
        deploy_component(app)
        expose_component(app)
        expose_components()
    else:
        # Deploy all components
        deploy_components()
        expose_components()
        time.sleep(1)
        configure_rou_db()

    # Display status of deployments and exposed endpoints
    status()


def terminate(args):
    if args.app == 'all':

        logger.info('* Terminating Deployments')
        for component in components_all:
            terminate_component(component)

        close_tunnel_connections()

    else:
        terminate_component(args.app)


def restart(args):
    terminate(args)
    deploy(args)


def status(args=None):
    logger.info('\nStandalone Minikube Status')

    ui_status_file = open('/tmp/dp-ui-port-forward.out', 'r')
    for line in ui_status_file:
        print(line, end='')

    display_status('dp-api', API_PORT)
    display_status('dp-jobs-api', JOBS_API_PORT)
    display_status('dp-rou', ROU_PORT)
    # display_status('dp-spark-sql-controller', _spark_sql_controller_port)


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
        choices=component_names())
    parser_build.add_argument(
        '--deps',
        action='store_true',
        help='build dependencies')
    parser_build.add_argument(
        '--jars',
        action='store_true',
        help='build jars')
    parser_build.add_argument(
        '--use-branch',
        type=str,
        default='master')
    parser_build.set_defaults(func=build)

    # deploy
    parser_deploy = subparsers.add_parser(
        'deploy',
        help='Deploy to minikube')
    parser_deploy.add_argument(
        '--branch',
        type=str,
        default='master',
        help='branch to build from')
    parser_deploy.add_argument(
        '--app',
        type=str,
        default='all',
        help='deployment name',
        choices=component_names())
    parser_deploy.set_defaults(func=deploy)

    # terminate
    parser_terminate = subparsers.add_parser(
        'terminate',
        help='Terminate the application on minikube')
    parser_terminate.add_argument(
        '--app',
        type=str,
        default='all',
        help='deployment name',
        choices=component_names())
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
        choices=component_names())
    parser_restart.set_defaults(func=restart)

    # status
    parser_status = subparsers.add_parser(
        'status',
        help='Display the application\'s status')
    parser_status.set_defaults(func=status)

    args = parser.parse_args()
    print(args)
    if args.command is None:
        parser.print_help(sys.stderr)
        sys.exit(1)

    args.func(args)


if __name__ == '__main__':
    main()
