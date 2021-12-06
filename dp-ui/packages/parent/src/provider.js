/*
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
*/
import { api } from '@dp-ui/lib'
import { isEmpty } from 'lodash'

export const ensureFirstLoginSet = () => {
  return fetchUser()
    .then((user) => {
      if (!user || !user.allVisibilities) {
        return
      }
      let vizPresent = false
      const attrs = user.allVisibilities || []
      if (!isEmpty(attrs)) {
        vizPresent = attrs
          .map((el) => el.toLowerCase())
          .includes('system.seen_joyride')
      }

      if (!vizPresent) {
        completeFirstLogin()
          // this extra call is to refresh the ui's new auths
          .then((res) => fetchUser())
      }
    })
    .catch((err) => {
      console.log('ensureFirstLoginSet Failed')
      console.log(err)
      return {}
    })
}

export const completeFirstLogin = () => {
  // POST /rules_of_use/complete_first_login
  const resource = `rules_of_use/complete_first_login`
  return api()
    .post({ resource, overrideContentType: 'application/json' })
    .then((res) => res.body)
    .catch((err) => {
      console.log('completeFirstLogin Failed')
      console.log(err)
      return {}
    })
}

export const resetFirstLogin = () => {
  // DELETE /rules_of_use/complete_first_login
  const resource = `rules_of_use/complete_first_login`
  return api()
    .delete({ resource, overrideContentType: 'application/json' })
    .then((res) => {
      // this extra call is to refresh the ui's new auths
      const json = res.body
      fetchInfo().then((res) => json)
    })
    .catch((err) => {
      console.log('resetFirstLogin Failed')
      console.log(err)
      return {}
    })
}

export const fetchInfo = () => {
  return api()
    .get({ resource: 'info', overrideContentType: 'application/json' })
    .then((res) => res.body)
}

export const fetchUser = () => {
  const resource = 'rules_of_use/current_user'
  return api()
    .get({ resource, overrideContentType: 'application/json' })
    .then((res) => JSON.parse(res.text))
}
