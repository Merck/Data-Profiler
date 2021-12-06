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
import React from 'react'
import { pullAll, trim } from 'lodash'
import {
  FormControlLabel,
  Checkbox,
  ExpansionPanel,
  ExpansionPanelSummary,
  ExpansionPanelDetails,
  Typography,
  Grid,
} from '@material-ui/core'
import ExpandMoreIcon from '@material-ui/icons/ExpandMore'
import { withStyles } from '@material-ui/core/styles'
import { DPContext } from '@dp-ui/lib'

const emptyText = (type) =>
  `You have no ${type} attributes assigned to you. Please check with the Data Profiler Administrator if you feel this is in error.`

const styles = (theme) => ({
  root: {
    width: '100%',
    margin: '25px 0',
  },
  heading: {
    fontSize: theme.typography.pxToRem(18),
    fontWeight: theme.typography.fontWeightBold,
  },
  explainer: {
    fontSize: theme.typography.pxToRem(12),
    marginBottom: theme.spacing(3),
  },
  attributeHeading: {
    fontSize: theme.typography.pxToRem(16),
    fontWeight: theme.typography.fontWeightBold,
  },
  attributeSection: {
    marginBottom: theme.spacing(3),
  },
  attributeItem: {
    marginRight: theme.spacing(2),
    minWidth: 200,
  },
})

const AttributePicker = (props) => {
  const { classes } = props

  const systemAttributes = props.dataprofiler.state.session.systemAttributes
  const attributesToReject =
    props.dataprofiler.state.session.attributesToReject || []

  const toggleAttributeToReject = (attribute) => {
    props.dataprofiler.setDPState(
      'session',
      'attributesToReject',
      attributesToReject.includes(attribute)
        ? attributesToReject.filter((e) => e !== attribute)
        : [...attributesToReject, attribute]
    )
    props.dataprofiler.setDPState('app', 'datasets', null)
  }

  const secondary = systemAttributes.filter(
    (el) => !!trim(el).startsWith('hr.')
  )
  const capabilities = systemAttributes.filter(
    (el) => !!trim(el).startsWith('system.')
  )
  const specials = systemAttributes.filter(
    (el) => !!trim(el).startsWith('special.')
  )
  const primary = pullAll(
    [...systemAttributes],
    [...secondary, ...capabilities, ...specials]
  )

  return (
    <div className={classes.root}>
      <ExpansionPanel>
        <ExpansionPanelSummary expandIcon={<ExpandMoreIcon />}>
          <Typography className={classes.heading}>Rules of Use</Typography>
        </ExpansionPanelSummary>
        <ExpansionPanelDetails>
          <Grid
            container
            direction="column"
            justify="flex-start"
            alignItems="flex-start">
            <Grid item>
              <Typography className={classes.explainer} gutterBottom>
                By default, you assume all attributes assigned to you. You can
                refuse specific attributes by unchecking them below
              </Typography>
            </Grid>
            <Grid item className={classes.attributeSection}>
              <Typography gutterBottom className={classes.attributeHeading}>
                Primary Attributes
              </Typography>
              <Grid container>
                {(primary &&
                  primary.length > 0 &&
                  primary.map((attribute) => (
                    <Grid
                      item
                      key={attribute}
                      className={classes.attributeItem}>
                      <FormControlLabel
                        control={
                          <Checkbox
                            onChange={() => toggleAttributeToReject(attribute)}
                            checked={!attributesToReject.includes(attribute)}
                            value={attribute}
                          />
                        }
                        label={attribute}
                      />
                    </Grid>
                  ))) ||
                  emptyText('primary')}
              </Grid>
            </Grid>
            <Grid item className={classes.attributeSection}>
              <Typography gutterBottom className={classes.attributeHeading}>
                HR Attributes
              </Typography>
              <Grid container>
                {(secondary &&
                  secondary.length > 0 &&
                  secondary.map((attribute) => (
                    <Grid
                      item
                      key={attribute}
                      className={classes.attributeItem}>
                      <FormControlLabel
                        control={
                          <Checkbox
                            onChange={() => toggleAttributeToReject(attribute)}
                            checked={!attributesToReject.includes(attribute)}
                            value={attribute}
                          />
                        }
                        label={attribute}
                      />
                    </Grid>
                  ))) ||
                  emptyText('HR')}
              </Grid>
            </Grid>
            <Grid item className={classes.attributeSection}>
              <Typography gutterBottom className={classes.attributeHeading}>
                System Capabilities
              </Typography>
              <Grid container>
                {(capabilities &&
                  capabilities.length > 0 &&
                  capabilities.map((attribute) => (
                    <Grid
                      item
                      key={attribute}
                      className={classes.attributeItem}>
                      <FormControlLabel
                        control={
                          <Checkbox
                            disabled={['system.admin', 'system.login'].includes(
                              attribute.toLowerCase()
                            )}
                            checked={!attributesToReject.includes(attribute)}
                            onChange={() => toggleAttributeToReject(attribute)}
                            value={attribute}
                          />
                        }
                        label={attribute}
                      />
                    </Grid>
                  ))) ||
                  emptyText('system')}
              </Grid>{' '}
            </Grid>
          </Grid>
        </ExpansionPanelDetails>
      </ExpansionPanel>
    </div>
  )
}

export default withStyles(styles)(DPContext(AttributePicker))
