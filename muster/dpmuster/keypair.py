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
import os
from .credentials import Credentials


class KeyPairExistsException(Exception):
    pass


class KeyPair:
    def __init__(self, cluster_name):
        self.cluster_name = cluster_name
        self.session = Credentials().get_session()
        self.ec2_client = self.session.client("ec2")

    @property
    def name(self):
        return "%s" % self.cluster_name

    def __path(self, extension=""):
        ssh_path = os.path.expanduser("~/.ssh/")
        return ssh_path + self.name + extension

    @property
    def private_key_path(self):
        return self.__path()

    @property
    def public_key_path(self):
        return self.__path(".pub")

    def __src_path(self, extension=""):
        prefix = 'configs/'
        return prefix + self.name + extension

    @property
    def private_key_src_path(self):
        return self.__src_path()

    @property
    def public_key_src_path(self):
        return self.__src_path(".pub")

    def private_key_file_exists(self):
        return os.path.exists(self.private_key_path)

    def public_key_file_exists(self):
        return os.path.exists(self.public_key_path)

    def aws_key_fingerprint(self):
        try:
            r = self.ec2_client.describe_key_pairs(KeyNames=[self.name])
            keypairs = r["KeyPairs"]
            assert (len(keypairs) == 1)
            return keypairs[0]["KeyFingerprint"]
        except:
            return None

    def create(self):
        if self.aws_key_fingerprint():
            raise KeyPairExistsException(
                "AWS key pair %s already exists." % self.name)

        if self.private_key_file_exists():
            raise KeyPairExistsException(
                "Private key %s already exists." % self.private_key_path)

        if self.public_key_file_exists():
            raise KeyPairExistsException(
                "Public key %s already exists." % self.public_key_path)

        key = self.ec2_client.create_key_pair(KeyName=self.name)

        with open(self.private_key_path, "w") as fd:
            fd.write(key["KeyMaterial"])

        os.chmod(self.private_key_path, 0o600)

        r = subprocess.run(["ssh-keygen", "-y", "-f", self.private_key_path],
                           stdout=subprocess.PIPE)
        if r.returncode != 0:
            raise Exception("Error extracting public key from private key")

        with open(self.public_key_path, "wb") as fd:
            fd.write(r.stdout)

        os.chmod(self.public_key_path, 0o600)

    def delete(self):
        print("Deleting AWS keypair %s and key files %s and %s." % (
            self.name, self.private_key_path, self.public_key_path))
        self.ec2_client.delete_key_pair(KeyName=self.name)
        try:
            os.unlink(self.private_key_path)
        except:
            pass

        try:
            os.unlink(self.public_key_path)
        except:
            pass

    def install_if_needed(self):
        if not self.public_key_file_exists():
            print("Installing public key from %s to %s" % (
                self.public_key_src_path, self.public_key_path))
            shutil.copy(self.public_key_src_path, self.public_key_path)

            os.chmod(self.public_key_path, 0o600)

        if not self.private_key_file_exists():
            if not os.path.exists(self.private_key_src_path):
                print("Decrypting private key")
                # TODO FIX THIS

            print("Installing private key from %s to %s" % (
                self.private_key_src_path, self.private_key_path))
            shutil.copy(self.private_key_src_path, self.private_key_path)
            os.chmod(self.private_key_path, 0o600)
