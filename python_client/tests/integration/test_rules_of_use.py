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
from requests import get as reqget, post as reqpost, delete as reqdel
from os import getenv

BASE_API_PATH = getenv('USER_FACING_API_HTTP_PATH', 'http://dp-api:9000')
AUTH_HEADERS = {'X-Username': 'test-developer', 'X-Api-Key': getenv('INTTESTPASS', 'local-developer')}


def test_rules_of_use():
    # routes: 863
    # RulesOfUseController
    # POST /rules_of_use  controllers.RulesOfUseController.proxy(request: Request)
    body = {
        'query': '{attributesActive}'
    }
    req_path = f'{BASE_API_PATH}/rules_of_use'
    res = reqpost(req_path, headers=AUTH_HEADERS, json=body)
    assert res.status_code == 200
    assert res.json() != {}
    assert res.json()['data'] != {}


def test_rules_of_use_user_authorizations():
    # routes: 878
    # RulesOfUseController
    # GET /rules_of_use/:user/authorizations controllers.RulesOfUseController.userAuthorizations(request: Request, user: String)
    user = 'test-developer'
    req_path = f'{BASE_API_PATH}/rules_of_use/{user}/authorizations'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert 'LIST.INTTESTING' in res.json()


def test_rules_of_use_user_user():
    # routes: 893
    # RulesOfUseController
    # GET /rules_of_use/user/:user controllers.RulesOfUseController.user(request: Request, user: String)
    user = 'test-developer'
    req_path = f'{BASE_API_PATH}/rules_of_use/user/{user}'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}
    assert res.json()['username'] == user


def test_rules_of_use_current_user():
    # routes: 908
    # LandingPageController
    # GET /rules_of_use/current_user controllers.LandingPageController.currentUser(request: Request)
    req_path = f'{BASE_API_PATH}/rules_of_use/current_user'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}
    assert res.json()['username'] == 'test-developer'


def test_rules_of_use_complete_first_login_delete():
    # routes: 939
    # LandingPageController
    # DELETE /rules_of_use/complete_first_login  controllers.LandingPageController.resetFirstLoginBadge(request: Request)
    req_path = f'{BASE_API_PATH}/rules_of_use/complete_first_login'
    res = reqdel(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.text == '{ status: true }'


#def test_rules_of_use_complete_first_login_post():
#    # routes: 924
#    # LandingPageController
#    # POST /rules_of_use/complete_first_login  controllers.LandingPageController.applyFirstLoginBadge(request: Request)
#    req_path = f'{BASE_API_PATH}/rules_of_use/complete_first_login'
#    res = reqpost(req_path, headers=AUTH_HEADERS, json={})
#    assert res.status_code == 200
#    assert res.text == '{ status: false }'


if __name__ == '__main__':
    test_rules_of_use()
    test_rules_of_use_user_authorizations()
    test_rules_of_use_user_user()
    test_rules_of_use_current_user()
    #test_rules_of_use_complete_first_login_post()
    test_rules_of_use_complete_first_login_delete()
    print('Complete')