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
import { Application, User, UserAttribute, Attribute } from '../models'

const message = 'SEEDING ROU DATABASE FOR INTEGRATION TESTING'
const application = 'Data Profiler'

console.log(`STARTING ${message}`)
new Promise(async (resolve,reject) => {
  Application.findOrCreate({where: { app: application, key: 'password' }})
  const admin = await Attribute.findOrCreate({where: { value: 'system.admin' }})
  const login = await Attribute.findOrCreate({where:{value: 'system.login' }})
  const discover = await Attribute.findOrCreate({where:{value: 'system.ui_discover_tab' }})
  const understand = await Attribute.findOrCreate({where:{value: 'system.ui_understand_tab' }})
  const use = await Attribute.findOrCreate({where:{value: 'system.ui_use_tab' }})
  const listpublic = await Attribute.findOrCreate({where:{value: 'LIST.PUBLIC_DATA', is_active: true }})
  const user = await User.findOrCreate({where:{username: 'developer' }})
  const user_id = Number(JSON.parse(JSON.stringify(user))[0].id)
  const admin_id = Number(JSON.parse(JSON.stringify(admin))[0].id)
  const discover_id = Number(JSON.parse(JSON.stringify(discover))[0].id)
  const use_id = Number(JSON.parse(JSON.stringify(use))[0].id)
  const understand_id = Number(JSON.parse(JSON.stringify(understand))[0].id)
  const login_id = Number(JSON.parse(JSON.stringify(login))[0].id)
  const listpublic_id = Number(JSON.parse(JSON.stringify(listpublic))[0].id)
  const action_performed_by_id = user_id
  await UserAttribute.findOrCreate({where: {user_id, attribute_id: admin_id}, application, action_performed_by_id})
  await UserAttribute.findOrCreate({where: {user_id, attribute_id: login_id}, application, action_performed_by_id})
  await UserAttribute.findOrCreate({where: {user_id, attribute_id: listpublic_id}, application, action_performed_by_id})
  await UserAttribute.findOrCreate({where: {user_id, attribute_id: discover_id}, application, action_performed_by_id})
  await UserAttribute.findOrCreate({where: {user_id, attribute_id: use_id}, application, action_performed_by_id})
  await UserAttribute.findOrCreate({where: {user_id, attribute_id: understand_id}, application, action_performed_by_id})
  console.log(`DONE ${message}`)
  resolve()
}).then(() => process.exit())
