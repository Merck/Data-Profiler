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
import { db, Sequelize } from "../../connectors";
import { UserModel } from "../user/user";
import { UserTableModel } from "../usertable/usertable";

const TableModel = db.define(
  "table",
  {
    id: {
      allowNull: false,
      autoIncrement: true,
      primaryKey: true,
      type: Sequelize.BIGINT
    },
    environment: Sequelize.STRING,
    datasetname: Sequelize.STRING,
    tablename: Sequelize.STRING,
    external_name: Sequelize.STRING,
    visibility: Sequelize.STRING,
    enabled: Sequelize.BOOLEAN,
  },
  {
    tableName: "tables",
    createdAt: "created_at",
    updatedAt: "updated_at",
    timestamps: true,
    underscored: true
  }
);

TableModel.belongsToMany(UserModel, { through: "user_tables" });
UserModel.belongsToMany(TableModel, { through: "user_tables" });
TableModel.hasOne(UserTableModel);

export { TableModel };
