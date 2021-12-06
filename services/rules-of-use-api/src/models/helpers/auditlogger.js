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
// stolen mostly from https://bl.ocks.org/twolfson/79c863c57bd9d5ca25054f72335fcc80
import { AuditLog } from '../index.js'
import _ from 'lodash'

export function saveAuditLog(action, model, options) {
  // Resolve our model's constructor
  var auditLog = AuditLog.build({
    table_name: model._modelOptions.tableName,
    table_row_id: model.dataValues.id,
    action: action,
    timestamp: new Date(),
    // https://github.com/sequelize/sequelize/blob/v3.25.0/lib/instance.js#L86-L87
    // https://github.com/sequelize/sequelize/blob/v3.25.0/lib/instance.js#L417-L433
    previous_values: model._previousDataValues,
    current_values: model.dataValues,
    application: options.application || null,
    action_performed_by_id: options.action_performed_by_id || null
  })
  return auditLog.save({ transaction: options.transaction })
}
