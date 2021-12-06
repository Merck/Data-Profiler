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
import {
  Typography,
  // ExpansionPanel,
  // ExpansionPanelSummary,
  // ExpansionPanelDetails,
} from '@material-ui/core'
import { withStyles } from '@material-ui/core/styles'
// import ExpandMoreIcon from '@material-ui/icons/ExpandMore'

const styles = (theme) => ({
  root: {
    padding: theme.spacing(5),
  },
  details: {
    flexDirection: 'column',
  },
})

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, error: null, errorInfo: null }
  }

  // static getDerivedStateFromError(error) {
  //   return { hasError: true, error }
  // }

  // componentDidCatch(error, errorInfo) {
  //   this.setState({ error, errorInfo })
  //   console.error(error, errorInfo)
  // }

  render() {
    // if (this.state.hasError) {
    return (
      <div className={this.props.classes.root}>
        <Typography variant="h4">Oops!</Typography>
        <Typography variant="subtitle1">
          We're sorry; Data Profiler has experienced an unexpected error.
        </Typography>
        <Typography variant="subtitle1">
          The Data Profiler Team has been notified, but you are welcome to try
          again
        </Typography>
        {/* <ExpansionPanel>
            <ExpansionPanelSummary expandIcon={<ExpandMoreIcon />}>
              <Typography className={this.props.classes.heading}>
                Technical Details
              </Typography>
            </ExpansionPanelSummary>
            <ExpansionPanelDetails className={this.props.classes.details}>
              <code>{this.state.error && this.state.error.toString()}</code>
              <code
                dangerouslySetInnerHTML={{
                  __html:
                    this.state.errorInfo &&
                    this.state.errorInfo.componentStack.replace(
                      /(?:\r\n|\r|\n)/g,
                      '<br />'
                    ),
                }}
              />
            </ExpansionPanelDetails>
          </ExpansionPanel> */}
      </div>
    )
    // }

    return this.props.children
  }
}

export default withStyles(styles)(ErrorBoundary)
