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
import {
  Box,
  CircularProgress,
  Select,
  MenuItem,
  ListItemIcon,
  ListItemText,
} from '@material-ui/core'
import WarningIcon from '@material-ui/icons/Warning'
import { Alert } from '@material-ui/lab'
import { createStyles, withStyles } from '@material-ui/core/styles'
import React from 'react'
import AlertStat from '../../models/quality/AlertStat'
import PopupState, { bindHover, bindPopover } from 'material-ui-popup-state'
import Popover from 'material-ui-popup-state/HoverPopover'
import ExpandMoreIcon from '@material-ui/icons/ExpandMore'
import { PieChart, Pie, Tooltip, Sector, Cell } from 'recharts'

// props from parent
export interface OwnProps {
  classes: Record<string, any>
  title: string
  value: number
  color?: string
  alerts: AlertStat[]
  tables?: { name: string; problematic: boolean }[]
  handleTableChange?
  ensureTableNames?
  selectedTable?: string
  pieData?: { count: number; dataType: string }[]
  pieTitle?: string
}
type Props = OwnProps

interface State {
  activeIndex?: number
  selectedTable?: string
}

class StatCard extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = {
      activeIndex: null,
    }

    this.onMouseOver = this.onMouseOver.bind(this)
    this.onMouseLeave = this.onMouseLeave.bind(this)
  }

  generateValueLabel(value: number) {
    switch (true) {
      case value > 74:
        return 'High'
      case value > 49:
        return 'Medium'
      default:
        return 'Low'
    }
  }

  renderActiveShape(props) {
    const RADIAN = Math.PI / 180
    const { cx, cy, innerRadius, outerRadius, startAngle, endAngle, midAngle } =
      props
    return (
      <Sector
        cx={cx}
        cy={cy}
        innerRadius={innerRadius}
        outerRadius={outerRadius}
        startAngle={startAngle}
        endAngle={endAngle}
        fill="#ff9800"
      />
    )
  }

  onMouseOver(data, index): void {
    this.setState({
      activeIndex: index,
    })
  }

  onMouseLeave(): void {
    this.setState({
      activeIndex: null,
    })
  }

  render(): JSX.Element {
    const {
      classes,
      title,
      value,
      color,
      alerts,
      tables,
      handleTableChange,
      ensureTableNames,
      selectedTable,
      pieData,
      pieTitle,
    } = this.props
    const { activeIndex } = this.state
    return (
      <div className={classes.root}>
        <Box
          display="flex"
          justifyContent="space-between"
          mb={3}
          style={{ height: '32px', whiteSpace: 'nowrap' }}>
          <span style={{ marginRight: 10 }}>{title}</span>
          {!!tables && (
            <Select
              // MenuProps={{
              //   disablePortal: true,
              // }}
              displayEmpty
              value={selectedTable}
              className={classes.select}
              onChange={handleTableChange}
              onOpen={ensureTableNames}>
              <MenuItem value="">Select a table</MenuItem>
              {tables.map((el, i) => {
                return (
                  <MenuItem key={`table-select-${i}`} value={el.name}>
                    {el.problematic && (
                      <ListItemIcon className={classes.icon}>
                        <WarningIcon />
                      </ListItemIcon>
                    )}
                    <ListItemText primary={el.name} />
                  </MenuItem>
                )
              })}
            </Select>
          )}
        </Box>
        <Box display="flex" pb={2}>
          <div>
            <Box
              position="relative"
              display="inline-flex"
              mr={1}
              alignItems="center"
              justifyContent="center">
              {pieData ? (
                <>
                  <span className={classes.pieTitle}>{pieTitle}</span>
                  <PieChart
                    width={114}
                    height={114}
                    margin={{ top: 0, left: 0, right: 0, bottom: 0 }}>
                    <Pie
                      data={pieData}
                      dataKey="count"
                      nameKey="dataType"
                      outerRadius="100%"
                      innerRadius="72%"
                      // fill={color}
                      activeIndex={activeIndex}
                      activeShape={this.renderActiveShape}
                      onMouseOver={this.onMouseOver}
                      onMouseLeave={this.onMouseLeave}>
                      {pieData.map((entry, index) => (
                        <Cell
                          key={`cell-${index}`}
                          fill={`rgba(${color},${
                            1 - (1 / pieData.length) * index
                          })`}
                        />
                      ))}
                    </Pie>
                    <Tooltip separator=": " position={{ x: 20, y: 20 }} />
                  </PieChart>
                </>
              ) : (
                <>
                  <CircularProgress
                    variant="determinate"
                    size={114}
                    thickness={6}
                    value={value}
                    classes={{
                      circle: classes.circle,
                    }}
                    style={{ color }}
                  />
                  <Box
                    className={classes.valueContainer}
                    top={0}
                    left={0}
                    bottom={0}
                    right={0}
                    position="absolute"
                    display="flex"
                    alignItems="center"
                    justifyContent="center"
                    flexDirection="column">
                    {!isNaN(value) && (
                      <>
                        <span className={classes.value}>{`${Math.round(
                          value
                        )}`}</span>
                        <span className={classes.label}>
                          {this.generateValueLabel(value)}
                        </span>
                      </>
                    )}
                  </Box>
                </>
              )}
            </Box>
          </div>
          <div className={classes.quality}>
            {alerts?.length ? (
              alerts.map((item, i) => (
                <Alert key={`alert-${i}`} severity={item.type}>
                  {item.tooltip ? (
                    <PopupState variant="popover" popupId="demo-popup-popover">
                      {(popupState) => (
                        <div>
                          <Box
                            display="flex"
                            justifyContent="space-between"
                            {...bindHover(popupState)}>
                            <span>{item.message}</span> <ExpandMoreIcon />
                          </Box>
                          <Popover
                            {...bindPopover(popupState)}
                            anchorOrigin={{
                              vertical: 'bottom',
                              horizontal: 'center',
                            }}
                            transformOrigin={{
                              vertical: 'top',
                              horizontal: 'center',
                            }}
                            disableRestoreFocus>
                            {Array.isArray(item.tooltip) ? (
                              <Box p={1} className={classes.tooltip}>
                                <ul>
                                  {item.tooltip.map((comment, i) => (
                                    <li key={`tooltip-${i}`}>{comment.note}</li>
                                  ))}
                                </ul>
                              </Box>
                            ) : (
                              <Box
                                p={1}
                                className={classes.tooltipHTML}
                                dangerouslySetInnerHTML={{
                                  __html: item.tooltip,
                                }}
                              />
                            )}
                          </Popover>
                        </div>
                      )}
                    </PopupState>
                  ) : (
                    item.message
                  )}
                </Alert>
              ))
            ) : (
              <Box ml={2}>Select a table to view data quality statistics</Box>
            )}
          </div>
        </Box>
      </div>
    )
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = createStyles((theme) => ({
  root: {
    background: '#fff',
    borderRadius: '10px',
    padding: '18px 16px',
    textAlign: 'center',
    minWidth: 300,
    flex: 1,
    '& .MuiAlert-root': {
      backgroundColor: 'transparent',
      paddingTop: 0,
      paddingBottom: 0,
      '& .MuiAlert-message': {
        color: 'rgba(0,0,0,0.87)',
        fontSize: '1rem',
      },
    },
    '& .recharts-tooltip-wrapper .recharts-default-tooltip': {
      fontSize: '10px',
      padding: '0 !important',
      // backgroundColor: 'transparent !important',
      border: 'none !important',
      whiteSpace: 'normal !important',
      width: '74px',
      height: '74px',
      borderRadius: '50%',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
    },
  },
  circle: {
    strokeLinecap: 'round',
    zIndex: 1,
  },
  valueContainer: {
    borderRadius: '50%',
    boxShadow: 'inset 0px 0px 0px 15px rgba(0, 0, 0, 0.1)',
    zIndex: 0,
  },
  value: {
    fontSize: '28pt',
    lineHeight: 1.1,
    fontWeight: 300,
  },
  label: {
    fontSize: '8pt',
    fontWeight: 500,
  },
  quality: {
    flex: 1,
    display: 'flex',
    justifyContent: 'center',
    flexDirection: 'column',
    textAlign: 'left',
  },
  select: {
    top: -4,
    '& .MuiListItemIcon-root': {
      display: 'none',
    },
  },
  icon: {
    color: theme.palette.warning.main,
    minWidth: 38,
  },
  tooltip: {
    '& ul': {
      margin: 0,
      paddingLeft: 20,
    },
  },
  tooltipHTML: {
    '& ul': {
      margin: 0,
      padding: 0,
      '& li': {
        display: 'flex',
        justifyContent: 'space-between',
        '& > span:last-of-type': {
          marginLeft: 20,
          opacity: 0.5,
        },
      },
    },
  },
  pieTitle: {
    position: 'absolute',
    top: '50%',
    textAlign: 'center',
    display: 'block',
    fontSize: '9px',
    transform: 'translateY(-50%)',
  },
}))

export default withStyles(styles)(StatCard)
