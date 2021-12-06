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
import { Table, User, UserTable } from "../index.js";
import { Sequelize } from "../../connectors";
import { normalize } from "../helpers/stringOps";

const Op = Sequelize.Op;
export const resolvers = {
  Query: {
    tables(_, args) {
      return Table.findAll({ where: args });
    },
    tablesLike(_, args) {
      let where = {};
      Object.entries(args).forEach((entry) => {
        const [key, value] = entry;
        where[key] = { [Op.iLike]: value.trim() + "%"};
      });
      return Table.findAll({
        where: where
      });
    },
    allTables(_, args) {
      return Table.findAll();
    }
  },
  Mutation: {
    createUpdateTable(_, args) {
      // console.log("createUpdateTable args: ", args)
      // All values are REQUIRED as per graphql schema and can be assumed to be set
      return Table.findOrCreate({
        where: {
          external_name: normalize(args.external_name), // PK
        },
        defaults: {
          enabled: args.enabled,
          datasetname: normalize(args.datasetname),
          tablename: normalize(args.tablename),
          environment: normalize(args.environment),
          visibility: normalize(args.visibility),
        }
      })
        .spread(async function(table, createdTable) {
          // console.log("createUpdateTable spread args: ", args)
          console.log("Creating table");
          console.log(table.get({ plain: true }));
          console.log("Table created:" + createdTable);
          console.log("Setting the enabled flag");
          await Promise.all([
            table.update({ 
              enabled: args.enabled,
              datasetname: normalize(args.datasetname),
              tablename: normalize(args.tablename),
              environment: normalize(args.environment),
              visibility: normalize(args.visibility),
            }),
            args.users.map(user =>
              User.findOrCreate({
                where: { username: normalize(user) }
              }).spread(function(user, createdUser) {
                console.log("Creating user");
                console.log(user.get({ plain: true }));
                console.log("User created: " + createdUser);
                UserTable.findOrCreate({
                  where: {
                    user_id: user.id,
                    table_id: table.id
                  }
                }).spread(function(userTable, createdUserTable) {
                  console.log("Creating user link to table");
                  console.log(userTable.get({ plain: true }));
                  console.log("Link created: " + createdUserTable);
                });
              })
            )
          ]);

          return table;
        })
        .then(table => table.reload());
    },
    removeUsersFromTable(_, args) {
      return Table.findOne({
        where: {
          datasetname: normalize(args.datasetname),
          tablename: normalize(args.tablename),
          environment: normalize(args.environment),
          external_name: normalize(args.external_name),
          visibility: normalize(args.visibility)
        }
      })
        .then(async table => {
          console.log("Found table");
          console.log(table.get({ plain: true }));

          // Get all of the user IDs from the users table
          const foundUserIds = await User.findAll({
            where: { username: { [Op.in]: args.users } }
          }).map(user => user.id);

          console.log("Found User IDs");
          foundUserIds.forEach(user => {
            console.log(user);
          });

          // Get all of the user_table links
          const foundUserTables = await UserTable.findAll({
            where: {
              [Op.and]: [{ user_id: foundUserIds }, { table_id: table.id }]
            }
          });

          // Delete all of the user_table links
          foundUserTables.forEach(async foundUserTable => {
            await foundUserTable.destroy();
          });

          // Remove users that don't exist in the user_tables table
          const remainingUserIds = await UserTable.findAll().map(
            userTable => userTable.user_id
          );

          User.destroy({
            where: { id: { [Op.notIn]: remainingUserIds } }
          });

          return table;
        })
        .then(table => table.reload());
    },
    deleteTables(_, args) {
      return Table.destroy({ where: args });
    },
    deleteAllTables(_, args) {
      return Table.findAll()
        .then(async tables => {
          const foundTableIds = tables.map(table => table.id);

          // Get the user Ids
          const foundUserIds = await UserTable.findAll({
            where: { table_id: { [Op.in]: foundTableIds } }
          }).map(user => user.user_id);

          // Get user objects
          const foundUsers = await User.findAll({
            where: { id: { [Op.in]: foundUserIds } }
          });

          console.log("Deleting tables");
          tables.forEach(async table => {
            console.log(table.get({ plain: true }));
            await table.destroy();
          });

          console.log("Deleting users");
          foundUsers.forEach(async foundUser => {
            await foundUser.destroy();
          });

          return tables;
        })
        .then(table => table.reload());
    }
  },
  Table: {
    users(table) {
      return table.getUsers();
    }
  }
};
