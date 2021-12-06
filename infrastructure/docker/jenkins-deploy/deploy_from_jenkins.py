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
from argparse import ArgumentParser
from os import environ as os_environ, getcwd
from subprocess import CalledProcessError
from subprocess import run as process_run
from re import sub as resub
from sys import exit


def login_docker(url='container-registry.com', user='docker', pw='docker'):
    """
    Logs into the container registry
    :param url: str; the url to log into
    :param user: str; username to use
    :param pw: str; password
    :return: bool; True if success, False if not
    """
    try:
        process_run(['/usr/bin/docker', 'login', '-u', user, '--password-stdin', url], 
                    input=pw.encode('utf-8'), 
                    check=True)
    except CalledProcessError:
        return False
    return True


def build_image(build_args='', dockerfile_path='.', tag='', build_path='.', extra_args=list()):
    """
    Builds a docker image
    :param build_args: str; additional build arguments to pass to docker
    :param dockerfile_path: str; path to dockerfile (assumes starting from python caller location)
    :param tag: str; how to tag the image
    :param build_path: str; the build context starting location for docker (assumes starting point from python caller)
    :return: bool; True if successful, False otherwise
    """
    cmd = ['/usr/bin/docker', 'build']
    if build_args:
        cmd.append(build_args)
    cmd.append('--network=host')
    cmd += extra_args
    cmd += [f'--tag={tag}', f'--file=Dockerfile', build_path]
    print(' '.join(cmd))
    try:
        process_run(args=cmd, 
                    check=True,
                    cwd='/'.join([getcwd(), dockerfile_path]))
    except CalledProcessError:
        return False
    return True


def push_image(tag=''):
    """
    Pushes a docker image
    :param tag: str; tag to push
    :return: bool; True if successful, False otherwise
    """
    try:
        process_run(['/usr/bin/docker', 'push', tag], 
                    check=True)
    except CalledProcessError:
        return False
    return True


def set_kube_context(namespace):
    """
    Sets the kuberenetes context to a specific namespace
    :param namespace: str; namespace to use
    :return: bool; True if successful, False if not
    """
    try:
        process_run(['/usr/local/bin/kubectl', 'config', 'set-context', '--current', f'--namespace={namespace}'], 
                    check=True)
    except CalledProcessError:
        return False
    return True


def sed_version_replace(version, app, target_dir=None):
    """
    Replaces the version string in the Helm Chart.yaml
    :param version: str; versionto replace with
    :param app: str; the app to replace the version in
    :param target_dir: path to directory (assumes starting point from python caller)
    :return: bool; True if successful, False otherwise
    """
    with open('/'.join([target_dir, 'Chart.yaml']), 'r+') as fp:
        source = fp.read()
        new_source = resub(r'version:.*', f'version: {version}', source)
        fp.seek(0)
        fp.truncate()
        fp.write(new_source)


def helm_app_installed(app):
    """
    Checks if a specified helm app is installed
    :param app: str; app to check
    :return: bool; True if successful, False otherwise
    """
    try:
        process_run(['/usr/local/bin/helm', 'status', app], 
                    check=True)
    except CalledProcessError:
        return False
    return True


def helm_install_app(app, values='values.yaml', helm_args=list(), app_install_path='./', target_dir=None):
    """
    Attempts to install a helm app
    :param app: str; app name
    :param values: str; path and name of values yaml to install (context is from the helm chart dir)
    :param helm_args: list(str); additional helm args
    :param app_install_path: str; modification of app install path (context is from the helm chart dir)
    :param target_dir: str; path to directory (assumes starting point from python caller)
    :return: bool; True if successful, False otherwise
    """
    cmd = ['/usr/local/bin/helm', 'install', f'--values={values}', app]
    if helm_args:
        cmd += helm_args
    cmd.append(app_install_path)
    try:
        process_run(cmd, 
                    check=True, 
                    cwd='/'.join([getcwd(), target_dir]))
    except CalledProcessError:
        return False
    return True


def helm_upgrade_app(app, values='values.yaml', helm_args=list(), app_install_path='.', target_dir=None):
    """
    Attempts to upgrade a helm app
    :param app: str; app name
    :param values: str; path and name of values yaml to use for overrides (context is from the helm chart dir)
    :param helm_args: list(str); additional helm args
    :param app_install_path: str; modification of app install path (context is from the helm chart dir)
    :param target_dir: str; path to directory (assumes starting point from python caller)
    :return: bool; True if successful, False otherwise
    """
    cmd = ['/usr/local/bin/helm', 'upgrade', f'--values={values}', app]
    if helm_args:
        cmd += helm_args
    cmd.append(app_install_path)
    try:
        process_run(cmd, 
                    check=True, 
                    cwd='/'.join([getcwd(), target_dir]))
    except CalledProcessError:
        return False
    return True


def main():
    parser = ArgumentParser(description='Builds and deploys a container image to a target namespace')
    parser.add_argument('app', help='App to deploy')
    parser.add_argument('namespace', help='Kube namespace to deploy to')
    parser.add_argument('no_cache', choices=['true', 'false'], help='Use cache or not')
    parser.add_argument('revision', help='Revision number to use')
    parser.add_argument('dockerfile_path', help='Path to dockerfile')
    parser.add_argument('--load_balancer')
    parser.add_argument('--setup', action='store_true', default=False)
    parser.add_argument('--docker_build_path', default='.')
    parser.add_argument('--additional_docker_args', type=str, default='', help='Additional arguments for docker build')
    args = parser.parse_args()
    additional_args = list()
    if args.additional_docker_args != '':
        additional_args = args.additional_docker_args.split(' ')

    deploy_type = 'deploy'
    if args.setup is True:
        deploy_type = 'setup'

    build_number = os_environ.get('BUILD_NUMBER', 'DEVELOPMENT')

    tag_version = f'{args.namespace}-{args.revision}'
    tag = f'container-registry.com/{args.app}:{tag_version}'
    helm_args = [f'--set=image.tag={tag_version}']
    version = f'0.0.{build_number}'

    helm_path = f'kube/helm-charts/namespace-{deploy_type}/{args.app}'

    explicit_build_args = ''
    if args.no_cache == 'true':
        explicit_build_args = '--no-cache'

    failed = False
    if login_docker():
        if build_image(explicit_build_args, args.dockerfile_path, tag, args.docker_build_path, additional_args):
            if push_image(tag):
                set_kube_context(args.namespace)
                sed_version_replace(version, args.app, helm_path)
                if helm_app_installed(args.app):
                    if not helm_upgrade_app(args.app, helm_args=helm_args, target_dir=helm_path):
                        print(f'Failed to upgrade app with tag {tag}')
                        failed = True
                else:
                    if not helm_install_app(args.app, helm_args=helm_args, target_dir=helm_path):
                        print(f'Failed to install app with tag {tag}')
                        failed = True
            else:
                print(f'Failed to push image with tag: {tag}')
                failed = True
        else:
            print(f'Failed to build image with tag: {tag}')
            failed = True
    else:
        print(f'Failed to login to docker registry!')
        failed = True
    
    if failed:
        exit(-1)


if __name__ == '__main__':
    main()
