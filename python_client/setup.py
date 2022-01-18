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

import setuptools

requires = []
with open("python_requirements.txt") as fd:
      requires = fd.read().split("\n")
requires = [x for x in requires if x != '']

setuptools.setup(name='dataprofiler-api',
      version='1.0',
      description='Python API library for the dataprofiler',
      packages=setuptools.find_packages(),
      scripts=["dataprofiler_loader", "daemon.py", "dpversions"],
      entry_points={
            'console_scripts': [
                  'dp-find-bad-load-commits = dataprofiler.tools.empty_table_finder:main'
            ]
      },
      install_requires=requires
      )
