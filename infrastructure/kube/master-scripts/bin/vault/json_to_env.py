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

import sys
import json
from typing import List
from typing import Dict

def read_json(in_file: str) -> Dict:
  with open(in_file) as input:
    return json.load(input)

def write_env(out_file: str, data: Dict) -> None:
  with open(out_file, "w") as output:
    for k,v in data.items():
      output.write("{}={}\n".format(k, v))

def main(argv: List[str]):
  in_file = argv[0]
  data = read_json(in_file)
  out_file = in_file
  if in_file.endswith(".json"):
    out_file = in_file.replace('.json', '.env')
  else:
    out_file = "{}.{}".format(in_file, 'env')
  write_env(out_file, data)


if __name__ == "__main__":
   main(sys.argv[1:])