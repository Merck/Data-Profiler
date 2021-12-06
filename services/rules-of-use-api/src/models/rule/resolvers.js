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
import { Rule, Attribute, Statement, StatementAttribute } from '../index.js'
import { createExpression } from '../helpers/createexpression'
import { queryExpression } from '../helpers/queryexpression'
import { Sequelize } from '../../connectors'
const Op = Sequelize.Op

export const resolvers = {
  Query: {
    rule(_, args) {
      return Rule.find({ where: args })
    },
    allRules() {
      return Rule.findAll()
    }
  },
  Mutation: {
    createRule(_, args) {
      return Rule.create({
        name: args.name,
        visible: args.visible,
        created_by_id: args.createdById
      })
        .then(async rule => {
          const statementPromises = args.statements.map(statementArg => {
            return Statement.create({ rule_id: rule.id }).then(
              async statement => {
                const createdStatement = statement
                const staAttrPromises = statementArg.attributes.map(
                  attributeArg => {
                    return Attribute.find({
                      where: {
                        name: attributeArg.name,
                        value: attributeArg.value
                      }
                    }).then(attribute => {
                      StatementAttribute.findOrCreate({
                        where: {
                          statement_id: createdStatement.id,
                          attribute_id: attribute.id
                        }
                      })
                    })
                  }
                )
                await Promise.all(staAttrPromises)
              }
            )
          })
          await Promise.all(statementPromises)
          return rule
        })
        .then(rule => Rule.find({ where: { id: rule.id } }))
    },
    updateRule(_, args) {
      return Rule.update(
        {
          name: args.name,
          visible: args.visible
        },
        {
          where: {
            id: args.id
          }
        }
      )
        .then(() => {
          return Rule.findOne({ where: { id: args.id } })
        })
        .then(rule => {
          args.statements.forEach(argStatement => {
            if (argStatement.id) {
              Statement.findOne({ where: { id: argStatement.id } }).then(
                argStatementObj => {
                  argStatementObj.getAttributes().then(curAttributes => {
                    const curAttributeIds = curAttributes.map(
                      attribute => attribute.id
                    )
                    const createStAttributes = argStatement.attributes.filter(
                      att => !new Set(curAttributeIds).has(att.id)
                    )
                    if (createStAttributes.length != 0) {
                      StatementAttribute.bulkCreate(
                        createStAttributes.map(att => {
                          return {
                            statement_id: argStatement.id,
                            attribute_id: att.id
                          }
                        })
                      )
                    }

                    const argAttributeIds = argStatement.attributes.map(
                      attribute => attribute.id
                    )
                    const destroyStAttributes = curAttributes.filter(
                      curAtt => !new Set(argAttributeIds).has(curAtt.id)
                    )
                    if (destroyStAttributes.length != 0) {
                      StatementAttribute.destroy({
                        where: {
                          statement_id: argStatement.id,
                          attribute_id: {
                            [Op.in]: destroyStAttributes.map(att => att.id)
                          }
                        }
                      })
                    }
                  })
                }
              )
            } else {
              Statement.create({ rule_id: rule.id }).then(statement => {
                StatementAttribute.bulkCreate(
                  argStatement.attributes.map(att => {
                    return { statement_id: statement.id, attribute_id: att.id }
                  })
                )
              })
            }
          })
          rule.getStatements().then(currentStatements => {
            const argStatementIds = args.statements.map(
              statement => statement.id
            )
            const destroyStatements = currentStatements.filter(
              stat => !new Set(argStatementIds).has(stat.id)
            )
            StatementAttribute.destroy({
              where: {
                statement_id: {
                  [Op.in]: destroyStatements.map(statement => statement.id)
                }
              }
            }).then(() => {
              Statement.destroy({
                where: {
                  id: {
                    [Op.in]: destroyStatements.map(statement => statement.id)
                  }
                }
              })
            })
          })
        })
        .then(() => Rule.findOne({ where: { id: args.id } }))
    },
    deleteRule(_, args) {
      Statement.findAll({
        where: { rule_id: args.id }
      }).then(statements => {
        StatementAttribute.destroy({
          where: { statement_id: { [Op.in]: statements.map(sta => sta.id) } }
        }).then(() => {
          Statement.destroy({
            where: { rule_id: args.id }
          }).then(() =>
            Rule.destroy({
              where: { id: args.id }
            })
          )
        })
      })
    }
  },
  Rule: {
    statements(rule) {
      // This is chaos! there must be a better way to do this - not sure why sequelize is returning promises for each of these objects
      return Rule.find({
        where: { id: rule.id }
      }).then(newRule => {
        return newRule.getStatements().then(statements => {
          return statements
        })
      })
    },
    visibilityExpression(rule) {
      return Rule.find({
        where: { id: rule.id }
      }).then(newRule => {
        return newRule
          .getStatements({ include: [{ model: Attribute }] })
          .then(statements => createExpression(statements))
      })
    },
    findAuthorizedUsers(rule) {
      return Rule.find({
        where: { id: rule.id }
      }).then(newRule => {
        return newRule
          .getStatements({ include: [{ model: Attribute }] })
          .then(statements => queryExpression(statements))
      })
    }
  }
}
