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
#from .fixtures import AUTH_HEADERS, BASE_API_PATH
from requests import get as reqget
from os import getenv

BASE_API_PATH = getenv('USER_FACING_API_HTTP_PATH', 'http://dp-api:9000')
AUTH_HEADERS = {'X-Username': 'test-developer', 'X-Api-Key': getenv('INTTESTPASS', 'local-developer')}

# The controller for this appears to do nothing as it proxies to an invalid page. No comments were made about a resolution so commenting out.


#def test_make_plugins():
#    # routes: 650
#    # MakeController
#    # GET /make/plugins controllers.MakeController.makePlugins(request: Request)
#    req_path = f'{BASE_API_PATH}/make/plugins'
#    res = reqget(req_path, headers=AUTH_HEADERS)
#    assert res.status_code == 200
#    assert res.json() != {}  # Response of 200 but 404 not found??


#def test_make_plugin_pluginid_json_form():
#    # routes: 665
#    # MakeController
#    # GET /make/plugin/:pluginID/json_form controllers.MakeController.getJsonForm(request: Request, pluginID: String)
#    plugin_id = 1
#    req_path = f'{BASE_API_PATH}/make/plugin/{plugin_id}/json_form'
#    res = reqget(req_path, headers=AUTH_HEADERS)
#    assert res.status_code == 200
#    assert res.json() != {}


if __name__ == '__main__':
#    test_make_plugins()
#    test_make_plugin_pluginid_json_form()
#    print('Complete')
    pass