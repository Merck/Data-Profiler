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
#!/usr/bin/env python3

import subprocess
import os
import sys
import argparse
import logging
import time

from custom_formatter import CustomFormatter

_cpus = 4
_memory = '8096'
_root_path, _ = os.path.split(os.path.dirname(os.path.realpath(__file__)))
_dataprofiler_path = os.path.dirname(_root_path)
_username = 'developer'
_rou_host = 'dp-rou'
_rou_port = '8081'
_api_port = '9000'
_jobs_api_port = '8082'
_data_loading_daemon_port = '8083',
_spark_sql_controller_port = '7999'
_spark_ui_port = '4040'
_data_config_map_name = 'datasets'
_tools_jar = f'{_dataprofiler_path}/data_profiler_core/tools/target/dataprofiler-tools-1.jar'
_util_jar = f'{_dataprofiler_path}/data_profiler_core/util/target/dataprofiler-util-1.jar'
_iter_jar = f'{_dataprofiler_path}/data_profiler_core/iterators/target/dataprofiler-iterators-1.jar'

backend_components = [
    'dp-accumulo',
    'dp-postgres',
    'dp-api',
    'dp-rou',
    'dp-data-loading-daemon',
    'dp-jobs-api',
    'dp-spark-sql-controller'
]

frontend_components = [
    'dp-ui'
]

components = backend_components + frontend_components

docker_build_dir_dict = {
    'dp-accumulo': f'{_root_path}/conf/dp-accumulo',
    'dp-postgres': f'{_root_path}/conf/dp-postgres',
    'dp-api': f'{_dataprofiler_path}/web/api',
    'dp-rou': f'{_dataprofiler_path}/rules-of-use-api',
    'dp-data-loading-daemon': f'{_dataprofiler_path}/data-loading-daemon',
    'dp-jobs-api': f'{_dataprofiler_path}/jobs-api',
    'dp-spark-sql-controller': f'{_dataprofiler_path}/spark-sql/controller',
    'dp-ui': f'{_dataprofiler_path}/dp-ui'
}

logger = logging.getLogger("standalone")
logger.setLevel(logging.INFO)

# create console handler with a higher log level
console_handler = logging.StreamHandler()
console_handler.setLevel(logging.DEBUG)
console_handler.setFormatter(CustomFormatter())
logger.addHandler(console_handler)


def configure_rou_db():
    logger.info('* Configuring Rules Of Use')

    # Create ROU key
    logger.info('  * Created api key as \'dp-rou-key\'')
    p = exec_cmd([f'{_root_path}/util/create_api_key.sh'])
    if not p.returncode == 0:
        logger.error('Failed to create api key')

    # Activate ROU attributes
    logger.info('  * Activating ROU attributes')
    p = exec_cmd([f'{_root_path}/util/activate_attributes.sh'], cwd=f'{_root_path}/util/')
    if not p.returncode == 0:
        logger.error('Failed to activate attributes')

    # Update user ROU attributes
    logger.info('  * Updating ROU attributes for \'developer\'')
    p = exec_cmd([f'{_root_path}/util/update_user_attributes.sh'], show_output=False)
    if not p.returncode == 0:
        logger.error('Failed to update user attributes for \'developer\'')


def build(app, _build_libs=False):
    if _build_libs:
        build_libs()
        copy_jars()
    if not app == 'all':
        build_images(arg_app)
    else:
        build_images()


# Deploy application(s) to Minikube cluster
def run(app):
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


def status():
    logger.info('\nStandalone Minikube Status')
    
    ui_status_file = open('/tmp/dp-ui-port-forward.out', 'r')
    for line in ui_status_file:
        print(line, end='')

    display_status('dp-api', _api_port)
    display_status('dp-jobs-api', _jobs_api_port)
    display_status('dp-rou', _rou_port)
    display_status('dp-spark-sql-controller', _spark_sql_controller_port)


def display_status(component, port):
    if not test_connection('localhost', f'{port}'):
        expose_component(component)
    else:
        print(f'{component} available at http://localhost:{port}')


def expose_components():
    close_tunnel_connections()

    # backend
    for c in backend_components:
        expose_component(c)

    # frontend
    expose_ui()

    # port forwards
    create_local_connection_tunnel('dp-api', f'{_api_port}')
    create_local_connection_tunnel('dp-rou', f'{_rou_port}')
    create_local_connection_tunnel('dp-jobs-api', f'{_jobs_api_port}')
    create_local_connection_tunnel('dp-data-loading-daemon', f'{_data_loading_daemon_port}')
    create_local_connection_tunnel('dp-spark-sql-controller', f'{_spark_sql_controller_port}')


def expose_component(app, port=None):
    logger.debug(f'Exposing component {app}')
    prefix = 4*' ' + '> '
    if port is None:
        cmd_suffix = ['--cluster-ip=None']
    else:
        cmd_suffix = ['--target-port', f'{port}']

    expose_cmd = ['kubectl', 'expose', 'deployment', f'{app}',
                  '--name', f'{app}'] + cmd_suffix
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
    exec_daemon_cmd(service_cmd, '/tmp/dp-ui-port-forward.out')
    logger.debug(ui_app)


def create_local_connection_tunnel(app, port):
    max_tries = 10
    tunnel_cmd = [
        'kubectl',
        'port-forward',
        f'deployment/{app}',
        f'{port}:{port}'
    ]
    exec_daemon_cmd(tunnel_cmd, f'/tmp/{app}-port-forward.out')
    success = test_connection('localhost', port)
    while not success and max_tries > 0:
        max_tries -= 1
        logger.debug(f'Waiting for {app} to report as \'RUNNING\'')
        time.sleep(2)
        exec_daemon_cmd(tunnel_cmd, f'/tmp/{app}-port-forward.out')
        success = test_connection('localhost', port)


def build_libs():
    logger.info('Building libraries')
    build_cmd = [
        'mvn',
        'clean',
        'install',
        '-DskipTests',
        '-P',
        'local'
    ]
    process = exec_cmd(build_cmd, cwd=f'{_dataprofiler_path}/data_profiler_core')
    return process


def copy_jars():
    logger.info('Copying libraries')
    logger.debug(f'Copying jar {_tools_jar}')

    # Copy jars to accumulo docker build directory
    print(f'{_root_path}/conf/dp-accumulo/jars/')
    copy_cmd = ['cp', _tools_jar, f'{_root_path}/conf/dp-accumulo/jars/']
    check_error(exec_cmd(copy_cmd))

    # Copy jars to api docker directory
    api_docker_build_lib_dir = f'{_dataprofiler_path}/web/api/data_profiler_core_jars/'
    print(f'{_dataprofiler_path}/web/api/data_profiler_core_jars/')

    copy_cmd = ['cp', _tools_jar, api_docker_build_lib_dir]
    check_error(exec_cmd(copy_cmd))

    copy_cmd = ['cp', _util_jar, api_docker_build_lib_dir]
    check_error(exec_cmd(copy_cmd))

    copy_cmd = ['cp', _iter_jar, api_docker_build_lib_dir]
    check_error(exec_cmd(copy_cmd))
    # return process


def build_images(app='all'):
    if app is None or app == 'all':
        logger.info('Building docker images')
        for c in components:
            build_docker_image(c)
    else:
        logger.info(f'Building docker image for {app}')
        build_docker_image(app)


def build_docker_image(tag):
    logger.info(f'{tag} image')
    image_tag = tag.replace('-', '/', 1)
    build_base = docker_build_dir_dict[tag]
    print(f'    build_base: {build_base}')
    docker_build_cmd = [
        'docker',
        'build',
        '-t',
        image_tag,
        '.'
    ]
    process = exec_cmd(docker_build_cmd, cwd=build_base)
    if not process.returncode == 0:
        logger.error(f'Failed to build {image_tag} image. exiting')
        exit(1)


def deploy_components():
    logger.info('* Deploying Components')
    for component in components:
        deploy_component(component)


def deploy_component(app):
    print(4*' ' + '> deploying ' + app, end='\r')

    # indent prefix for following output
    prefix = 4*' ' + '> '

    # create configmap containing cluster data
    if app == 'dp-accumulo':
        configmap_cmd = [
            'kubectl',
            'create',
            'configmap',
            _data_config_map_name,
            '--from-file=data'
        ]
        logger.debug(configmap_cmd)
        exec_cmd(configmap_cmd, cwd=_root_path, prefix=prefix)

    # create deployment
    deploy_cmd = [
        'kubectl',
        'create',
        '-f',
        f'{_root_path}/conf/{app}/{app}.yaml'
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


def terminate_components():
    logger.info('* Terminating Deployments')
    for component in components:
        terminate_component(component)


def terminate_component(app):
    logger.debug(4*' Deleting ' + app)

    # indent prefix for following output
    prefix = 6*' ' + ' '

    # delete deployment
    delete_deploy_cmd = [
        'kubectl',
        'delete',
        '-f',
        f'{_root_path}/conf/{app}/{app}.yaml'
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

    # delete configmap containing cluster data
    if app == 'dp-accumulo':
        delete_configmap_cmd = [
            'kubectl',
            'delete',
            'configmap',
            _data_config_map_name
        ]
        exec_cmd(delete_configmap_cmd, prefix=prefix)

    print(' '*100, end='\r')
    logger.info(4*' ' + f'{app:.<35}' + 'deleted')


def check_minikube_status():
    process = exec_cmd(['minikube', 'status'], show_output=False)
    if not process.returncode == 0:
        logger.debug(f'Minikube is not running. attempting to start...')
        restart_cmd = ['minikube', 'start', '--cpus', f'{_cpus}',
                       '--memory', f'{_memory}']
        process = exec_cmd(restart_cmd)
    if not process.returncode == 0:
        logger.error('Failed to start minikube - exiting')
        exit(1)
    else:
        logger.debug(f'Minikube is running')
    return process


def check_pod_status(app):
    process = exec_cmd(['./util/check_pod_status.sh', app])
    return process


def check_requirements():
    required = ['minikube', 'kubectl']
    for req in required:
        if not is_available(req):
            logger.info(f'Missing requirement -- please install \'{req}\'')
            exit(1)


def exec_cmd(cmd, cwd=_root_path, prefix='', show_output=True):
    logger.debug(f'Running {cmd}')
    try:
        process = subprocess.Popen(cmd, cwd=cwd, text=True, close_fds=True,
                                   stdin=subprocess.PIPE,
                                   stdout=subprocess.PIPE,
                                   stderr=subprocess.PIPE)
        output, error = process.communicate()
        if process.returncode == 0:
            logger.debug(f'\'{cmd}\' successful.')
            if show_output and output:
                logger.debug('%s%s' % (prefix, output.rstrip()))
                print(' '*100, end='\r')
                print('%s%s' % (prefix, output.rstrip()), end='\r')
        elif process.returncode == 1:
            if show_output and output:
                logger.error('%r' % output)
        else:
            logger.error('Error occurred: %r -- return code (%d)' % (error, process.returncode))

    except subprocess.CalledProcessError as e:
        logger.error(f'Fatal error: {e}')
        sys.exit(1)

    return process


def exec_daemon_cmd(cmd, file):
    logger.debug(f'Running daemon cmd \'{cmd}\'')
    f = open(file, 'w')
    fe = open(file + 'err', 'w')
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


parser = argparse.ArgumentParser()
subparsers = parser.add_subparsers(title='sub-commands')

# run
deploy_parser = subparsers.add_parser('run')
deploy_parser.add_argument('--branch', type=str, default='master', help='branch to build from')
deploy_parser.add_argument('--app', type=str, default='all', help='deployment name', choices=components)

# clean
clean_parser = subparsers.add_parser('clean')
clean_parser.add_argument('--app', type=str, default='all', help='deployment name', choices=components)

# build
build_parser = subparsers.add_parser('build')
build_parser.add_argument('--app', type=str, default='all', help='deployment name', choices=components)
build_parser.add_argument('--jars', action='store_true', help='build jars')
build_parser.add_argument('--use-branch', type=str, default='master')

# push
build_parser = subparsers.add_parser('push')

# restart
restart_parser = subparsers.add_parser('restart')

# status
status_parser = subparsers.add_parser('status')

args = parse_args(parser, subparsers)

# check for required executables
check_requirements()

# check minikube status and attempt to start if possible
check_minikube_status()

if args.run:
    arg_app = getattr(args.run, 'app')
    run(arg_app)

elif args.clean:
    arg_app = getattr(args.clean, 'app')
    if not arg_app == 'all':
        terminate_component(arg_app)
    else:
        terminate_components()
        close_tunnel_connections()

elif args.build:
    arg_app = getattr(args.build, 'app')
    arg_jars = getattr(args.build, 'jars')

    # build libraries and copy jars
    build(arg_app, arg_jars)

elif args.restart:
    terminate_components()
    close_tunnel_connections()
    deploy_components()
    expose_components()

elif args.status:
    status()
