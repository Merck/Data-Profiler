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
import ExpandMoreIcon from '@material-ui/icons/ExpandMore'
import {
  Typography,
  ExpansionPanel,
  ExpansionPanelSummary,
  ExpansionPanelDetails,
} from '@material-ui/core'
import AllowBubblesPicker from './AllowBubblesPicker'

const styles = (theme) => ({
  experimental: {
    marginTop: 30,
  },
  button: {
    display: 'block',
    marginTop: 8,
    marginLeft: 0,
  },
  heading: {
    fontSize: theme.typography.pxToRem(18),
    fontWeight: theme.typography.fontWeightBold,
  },
})

const ExperimentalSettings = (props) => (
  <ExpansionPanel className={props.classes.experimental}>
    <ExpansionPanelSummary expandIcon={<ExpandMoreIcon />}>
      <Typography className={props.classes.heading}>
        Experimental Settings
      </Typography>
    </ExpansionPanelSummary>
    <ExpansionPanelDetails>
      <AllowBubblesPicker />
    </ExpansionPanelDetails>
  </ExpansionPanel>
)

export default withStyles(styles)(ExperimentalSettings)
