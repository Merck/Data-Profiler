# Introduction

muster is a tool for creating and managing dataprofiler clusters in AWS environment. It uses a combination of Ansible, Ambari, and the AWS python boto3 library.

## Installation

* Requires Python 3
* Requires aws-cli
* pip3 install -r python_requirements.txt will install all of the needed Python libraries.

## Usage

## Credentials

Muster will automatically download credentials for you if needed. This is either based on the cluster configuration file or, for commands such as list that aren't specific to a cluster.

For the cloud credentials, you will be prompted for your password. These credentials expire after an hour, so if there is an error that indicates they have expired the command will fail and the credentials will be deleted. Just repeat the command and the credentials will be downloaded automatically.

The cloud credentials will be saved to `~/.aws/credentials`.

## Environment

Before creating a cluster you need to create the environment file that contains all of the configuration. You can get
a template to start from by decrypting the example

    muster decrypt-file env-template.enc

This template will be the source for almost all information about the cluster and will typically be the first argument for any operation.
