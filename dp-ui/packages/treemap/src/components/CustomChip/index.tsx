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
import { createStyles, Theme, withStyles } from '@material-ui/core'
import React from 'react'

// props from parent
export interface Props {
  classes: Record<string, any>
  style: Record<string, any>
}
interface State {}

class CustomChip extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props)
  }

  render() {
    const { classes } = this.props
    return (
      <span {...this.props} className={classes.chip}>
        <strong>{this.props.children}</strong>
      </span>
    )
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = (theme: Theme) =>
  createStyles({
    root: {},
    chip: {
      fontSize: '.65em',
      borderRadius: '15px',
      padding: '2px 15px',
    },
  })

export default withStyles(styles)(CustomChip)
