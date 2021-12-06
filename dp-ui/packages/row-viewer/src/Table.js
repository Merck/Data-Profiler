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
import React, { useContext, useLayoutEffect, useState } from 'react'
import styled from 'styled-components'
import { RowViewerContext } from './store'
import { DPContext } from '@dp-ui/lib'
import {
  useTable,
  useBlockLayout,
  useResizeColumns,
  useFilters,
  useColumnOrder,
  useRowSelect,
} from 'react-table'
import { DragDropContext, Droppable, Draggable } from 'react-beautiful-dnd'
import { FixedSizeList } from 'react-window'
import InfiniteLoader from 'react-window-infinite-loader'
import { loadRows } from './actions'
import { columnOrderKey } from './helpers'

const scrollbarWidth = () => {
  // https://davidwalsh.name/detect-scrollbar-width
  const scrollDiv = document.createElement('div')
  scrollDiv.setAttribute(
    'style',
    'width: 100px; height: 100px; overflow: scroll; position:absolute; top:-9999px;'
  )
  document.body.appendChild(scrollDiv)
  const scrollbarWidth = scrollDiv.offsetWidth - scrollDiv.clientWidth
  document.body.removeChild(scrollDiv)
  return scrollbarWidth
}

const noop = () => {} //eslint-disable-line @typescript-eslint/no-empty-function

const Styles = styled.div`
  width: 100%;
  position: relative;

  .dragContainer {
    position: relative;
  }

  .resizer {
    display: inline-block;
    background: rgba(0, 0, 0, 0.5);
    width: 6px;
    height: 96%;
    position: absolute;
    right: 0;
    cursor: col-resize;
    top: 0;
    transform: translateX(50%);
    z-index: 1000;

    ${'' /* prevents from scrolling while dragging on touch devices */}
    touch-action:none;

    &.isResizing {
      background: rgba(243, 166, 171, 0.5);
    }
  }

  .button {
    margin: 6px;
  }

  .loading {
    cursor: progress;
    position: absolute;
    width: 100%;
    height: 100%;
    top: 0;
    left: 0;
    background-color: rgba(0, 0, 0, 0.1);
    z-index: 990000;
  }

  .tableBody {
    position: relative;
  }

  .table {
    display: inline-block;
    border-spacing: 0;
    border: 1px solid ddd;
    cursor: cell;

    .tr {
      :last-child {
        .td {
          border-bottom: 0;
        }
      }
    }

    .th {
      background-color: #fff;
      font-size: 16px;
    }

    .td {
      font-size: 13px;
    }

    .selected {
      background-color: rgba(174, 255, 0, 0.8) !important;
    }

    .even {
      background-color: #fff;
    }
    .odd {
      background-color: #eee;
    }

    .th,
    .td {
      margin: 0;
      padding: 0.5rem;
      border-bottom: 1px solid #ddd;
      border-right: 1px solid #ddd;

      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;

      :last-child {
        border-right: 1px solid #ddd;
      }
    }
  }
`

const getItemStyle = ({ isDragging, isDropAnimating }, draggableStyle) => ({
  ...draggableStyle,
  userSelect: 'none',
  background: isDragging ? 'rgba(243,166,171,0.5)' : '#fff',
  ...(!isDragging && { transform: 'translate(0,0)' }),
  ...(isDropAnimating && { transitionDuration: '0.001s' }),
})

const getTableViewport = (table) => {
  return (
    document.documentElement.clientHeight -
    table.getBoundingClientRect().bottom -
    5 -
    scrollbarWidth()
  )
}

function Table(props) {
  const { data, columns } = props
  const [state, dispatch] = useContext(RowViewerContext)
  const tableHeader = React.useRef()
  const [tableViewport, setTableViewport] = useState(400)
  const defaultColumn = React.useMemo(
    () => ({
      minWidth: 125,
      width: 250,
      maxWidth: 800,
    }),
    []
  )

  const scrollBarSize = React.useMemo(() => scrollbarWidth(), [])

  const currentColOrder = React.useRef()

  const {
    getTableProps,
    getTableBodyProps,
    headerGroups,
    rows,
    totalColumnsWidth,
    prepareRow,
    allColumns,
    setColumnOrder,
    state: { selectedRowIds },
  } = useTable(
    {
      columns,
      data,
      defaultColumn,
    },
    useBlockLayout,
    useResizeColumns,
    useFilters,
    useColumnOrder,
    useRowSelect
  )

  const RenderRow = React.useCallback(
    ({ index, style }) => {
      const row = rows[index]
      prepareRow(row)
      return (
        <div
          {...row.getRowProps({
            style,
          })}
          onClick={() => {
            row.toggleRowSelected()
          }}
          className={`tr ${index % 2 === 0 ? 'even' : 'odd'} ${
            row.isSelected ? 'selected' : ''
          }`}>
          {row.cells.map((cell) => {
            return (
              <div key={cell} {...cell.getCellProps()} className="td">
                {cell.render('Cell')}
              </div>
            )
          })}
        </div>
      )
    },
    [prepareRow, rows, selectedRowIds]
  )

  const currentColOrder = React.useRef()

  useLayoutEffect(() => {
    // set table height on resize
    if (!tableHeader.current) {
      return
    }

    setTableViewport(getTableViewport(tableHeader.current))

    let timeoutId = null
    const resizeListener = () => {
      clearTimeout(timeoutId)
      timeoutId = setTimeout(() => {
        setTableViewport(getTableViewport(tableHeader.current))
      }, 150)
    }
    window.addEventListener('resize', resizeListener)

    return () => {
      window.removeEventListener('resize', resizeListener)
    }
  }, [])

  // Render the UI for your table
  return (
    <Styles>
      <div {...getTableProps()} className={`table`}>
        <div ref={tableHeader}>
          {headerGroups.map((headerGroup) => (
            <DragDropContext
              key={headerGroup}
              onDragStart={() => {
                currentColOrder.current = allColumns.map((o) => o.id)
              }}
              onDragUpdate={(dragUpdateObj) => {
                const colOrder = [...currentColOrder.current]
                const sIndex = dragUpdateObj.source.index
                const dIndex =
                  dragUpdateObj.destination && dragUpdateObj.destination.index
                if (typeof sIndex === 'number' && typeof dIndex === 'number') {
                  colOrder.splice(sIndex, 1)
                  colOrder.splice(dIndex, 0, dragUpdateObj.draggableId)
                  setColumnOrder(colOrder)
                }
              }}>
              <Droppable droppableId="droppable" direction="horizontal">
                {(droppableProvided) => (
                  <div
                    {...headerGroup.getHeaderGroupProps()}
                    ref={droppableProvided.innerRef}
                    className="row header-group">
                    {headerGroup.headers.map((column, index) => (
                      <div key={column.id} className="dragContainer">
                        <Draggable
                          draggableId={column.id}
                          index={index}
                          isDragDisabled={!column.accessor}>
                          {(provided, snapshot) => (
                            <div
                              {...column.getHeaderProps()}
                              className="cell header th">
                              <div
                                {...provided.draggableProps}
                                {...provided.dragHandleProps}
                                ref={provided.innerRef}
                                style={{
                                  ...getItemStyle(
                                    snapshot,
                                    provided.draggableProps.style
                                  ),
                                }}>
                                {column.render('Header')}
                                <div>
                                  {column.canFilter && column.render('Filter')}
                                </div>
                              </div>
                            </div>
                          )}
                        </Draggable>
                        <div
                          {...column.getResizerProps()}
                          className={`resizer ${
                            column.isResizing ? 'isResizing' : ''
                          }`}
                        />
                      </div>
                    ))}
                    {droppableProvided.placeholder}
                  </div>
                )}
              </Droppable>
            </DragDropContext>
          ))}
        </div>

        <div {...getTableBodyProps()} className={'tableBody'}>
          <InfiniteLoader
            isItemLoaded={(index) => !state.endLocation || index < rows.length}
            itemCount={state.endLocation ? rows.length + 1 : rows}
            loadMoreItems={
              state.loading
                ? noop
                : () => loadRows(dispatch, state, props.api, true)
            }>
            {({ onItemsRendered, ref }) => (
              <FixedSizeList
                height={props.tableBodyHeightPx || tableViewport}
                itemCount={rows.length}
                onItemsRendered={onItemsRendered}
                itemSize={35}
                ref={ref}
                width={totalColumnsWidth + scrollBarSize}>
                {RenderRow}
              </FixedSizeList>
            )}
          </InfiniteLoader>
        </div>
      </div>
    </Styles>
  )
}

export default DPContext(Table)
