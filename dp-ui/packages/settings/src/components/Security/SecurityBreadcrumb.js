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
import { withStyles } from '@material-ui/core/styles'
import ArrowRightIcon from '@material-ui/icons/ArrowRight'
import { Grid } from '@material-ui/core'
import { Button } from '@material-ui/core'
import { DPContext } from '@dp-ui/lib'

const styles = {
  root: {
    marginTop: 15,
    display: 'flex',
    alignItems: 'center',
  },
  separator: {
    display: 'inline-flex',
    alignItems: 'center',
    '& svg': {
      opacity: 0.54,
    },
  },
  currentButton: {
    '&:disabled': {
      color: '#000',
    },
  },
}

const push = window.alert

const SecurityBreadcrumb = (props) => (
  <Grid item xs={12} className={props.classes.root}>
    <Button onClick={() => props.router.navigate('/settings/security')}>
      Admin: Access Control
    </Button>
    <span className={props.classes.separator}>
      <ArrowRightIcon />
    </span>
    {props.crumbs &&
      props.crumbs.map((crumb) => (
        <>
          <span key={crumb.label}>
            <Button
              onClick={() => props.router.navigate(`/settings${crumb.path}`)}>
              {crumb.label}
            </Button>
          </span>
          <span className={props.classes.separator}>
            <ArrowRightIcon />
          </span>
        </>
      ))}
    <Button className={props.classes.currentButton} disabled>
      {props.currentLocation}
    </Button>
  </Grid>
)

export default DPContext(withStyles(styles)(SecurityBreadcrumb), {
  requireAdmin: true,
})
