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
  Button,
  Chip,
  ClickAwayListener,
  Collapse,
  Divider,
  FormControl,
  IconButton,
  InputBase,
  Paper,
} from '@material-ui/core'
import { createStyles, Theme, withStyles } from '@material-ui/core/styles'
import { Search } from '@material-ui/icons'
import { Alert } from '@material-ui/lab'
import { bindActionCreators, Dictionary, Dispatch } from '@reduxjs/toolkit'
import { debounce, isBoolean, isEmpty, isUndefined } from 'lodash'
import React, { Fragment } from 'react'
import { connect, ConnectedProps } from 'react-redux'
import { setFilterValue, setHoveredValue, searchInFlight } from '../actions'
import { updateSelectedDrilldownWithRefresh } from '../compositeActions'
import {
  ACTION_BTN_COLOR,
  DARK_GREY,
  SEARCH_CHIP_BACKGROUND,
  SEARCH_CHIP_BORDER,
  SEARCH_CHIP_COLOR,
} from '../dpColors'
import { TITLE_CLICK_FEATURE_FLAG } from '@dp-ui/parent/src/features'
import { StoreState } from '../index'
import SelectedDrilldown from '../drilldown/models/SelectedDrilldown'
import { SELECTED_VIEW_ENUM } from '../models/SelectedView'
import { nicefyNumber } from '../nicefyNumber'
import {
  addChipPhrase,
  clearSearchSuggestions,
  removeChipPhrase,
  updateSearch,
} from './actions'
import AggregatedSearchResult from './models/AggregatedSearchResult'
import { ChipPhrase, ChipPhraseScopeEnum } from './models/ChipPhrase'
import {
  getDisplayedColumnSuggestions,
  getDisplayedSearchResults,
  getDisplayedTableSuggestions,
  getHoveredValue,
  getLastSearchToken,
  getSummedSearchResults,
} from './selectors'
import { getSelectedDrilldown } from '../drilldown/selectors'

// props from parent
export interface OwnProps {
  classes: Record<string, any>
}
// props from redux state
interface StateProps {}
// props from redux actions
type DispatchProps = ConnectedProps<typeof connector>
type Props = StateProps & DispatchProps & OwnProps
interface State {
  search?: string
  overrideCloseMenu: boolean
  scrollValuesSection: boolean
  scrollColumnsSection: boolean
  scrollTablesSection: boolean
  scrollDatasetsSection: boolean
  hasFocus: boolean
  hasEnteredSearch: boolean
}

class MultiSearch extends React.Component<Props, State> {
  onChangeDebouncedFn = undefined
  onChangeDebounceTimeMillis = 500
  defaultTopN = 4
  multiSearchInputRef = undefined
  static MAX_SEARCH_CHIP = 1

  constructor(props: Props) {
    super(props)
    this.state = {
      overrideCloseMenu: false,
      scrollValuesSection: false,
      scrollColumnsSection: false,
      scrollTablesSection: false,
      scrollDatasetsSection: false,
      hasFocus: false,
      hasEnteredSearch: false,
    }
    this.handleEscKey = this.handleEscKey.bind(this)
    this.handleClickAway = this.handleClickAway.bind(this)
    this.handleOnFocus = this.handleOnFocus.bind(this)
    this.handleOnBlur = this.handleOnBlur.bind(this)
    this.multiSearchInputRef = React.createRef()
  }

  componentDidMount(): void {
    document.addEventListener('keydown', this.handleEscKey, false)
  }

  componentWillUnmount(): void {
    document.removeEventListener('keydown', this.handleEscKey, false)
  }

  handleEscKey(event: KeyboardEvent): void {
    if (event.keyCode === 27) {
      this.handleClickAway()
    }
  }

  handleClickAway(): void {
    // NOTE: this check saves browser compute and paint cycles
    // this method is slow, and is called more often than I would like
    // short circuit the work if it is not needed
    if (isBoolean(this.state.hasFocus) && this.state.hasFocus !== true) {
      return
    }
    this.reset()
  }

  handleMouseEnterValue(
    value: AggregatedSearchResult,
    event?: React.MouseEvent
  ): void {
    if (event) {
      event.preventDefault()
    }

    this.props.setHoveredValue(value)
  }

  handleMouseLeaveValue(event?: React.MouseEvent): void {
    if (event) {
      event.preventDefault()
    }

    this.props.setHoveredValue(null)
  }

  handleSubmitNamedValue(
    result: Readonly<AggregatedSearchResult>,
    scope: Readonly<ChipPhraseScopeEnum>,
    event?: React.SyntheticEvent
  ): void {
    if (event) {
      event.preventDefault()
    }

    if (!TITLE_CLICK_FEATURE_FLAG) {
      return
    }

    if (this.hasMaxChips()) {
      return
    }
    const phrase = result.value
    this.props.setFilterValue(result)
    this.props.addChipPhrase({ phrase, scope })

    this.trackResultClick(result)

    this.reset()
  }

  handleSubmitValue(
    result: AggregatedSearchResult,
    scope: ChipPhraseScopeEnum,
    event?: React.SyntheticEvent
  ): void {
    if (event) {
      event.preventDefault()
    }

    if (this.hasMaxChips()) {
      return
    }

    const phrase = result.value
    this.props.setFilterValue(result)
    this.props.addChipPhrase({ phrase, scope })

    this.trackResultClick(result)

    this.reset()
  }

  trackResultClick(result: AggregatedSearchResult): void {
    const uniqueDatasets = [
      ...Array.from(
        new Set(result.elements.map((item: { dataset: any }) => item.dataset))
      ),
    ]

    for (const dataset of uniqueDatasets) {
      window['_paq'].push([
        'trackContentImpression',
        'Dataset Search Appearance',
        dataset,
        this.multiSearchInputRef.current.value,
      ])
    }
  }

  hasMaxChips(): boolean {
    const chipPhrases = this.props.chipPhrases
    // restrict the number of search terms we send to the backend
    return chipPhrases && chipPhrases.length > MultiSearch.MAX_SEARCH_CHIP
  }

  reset(): void {
    this.closePanel()
    this.handleOnBlur()

    if (this.multiSearchInputRef.current.value?.trim().length) {
      this.props.setHoveredValue(null)
      this.multiSearchInputRef.current.value = ''
      this.updateSearchToken('')
      this.props.clearSearchSuggestions()
    }
  }

  toggleShowAllValues(event?: React.MouseEvent) {
    if (event) {
      event.preventDefault()
    }

    this.setState((state) => {
      return {
        scrollValuesSection: !state.scrollValuesSection,
      }
    })
  }

  toggleShowAllColumns(event?: React.MouseEvent) {
    if (event) {
      event.preventDefault()
    }

    this.setState((state) => {
      return {
        scrollColumnsSection: !state.scrollColumnsSection,
      }
    })
  }

  toggleShowAllTables(event?: React.MouseEvent) {
    if (event) {
      event.preventDefault()
    }

    this.setState((state) => {
      return {
        scrollTablesSection: !state.scrollTablesSection,
      }
    })
  }

  toggleShowAllDatasets(event?: React.MouseEvent) {
    if (event) {
      event.preventDefault()
    }

    this.setState((state) => {
      return {
        scrollDatasetsSection: !state.scrollDatasetsSection,
      }
    })
  }

  closePanel(): void {
    this.setState({
      overrideCloseMenu: true,
    })
  }

  updatedDebouncedSearchToken(
    event?: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ): void {
    if (event) {
      event.preventDefault()
    }
    if (!this.onChangeDebouncedFn) {
      // https://medium.com/@anuhosad/debouncing-events-with-react-b8c405c33273
      // signal to React not to nullify the event object to be used later by debounce
      event.persist()
      this.onChangeDebouncedFn = debounce(() => {
        // console.log(this.multiSearchInputRef.current.value)
        // this.updateSearchToken(this.multiSearchInputRef.current.value)
        this.updateSearchToken(event.target.value)
        this.onChangeDebouncedFn = undefined
      }, this.onChangeDebounceTimeMillis)
    }
    this.onChangeDebouncedFn()
  }

  updateSearchToken(tokens: string): void {
    const { selectedView, selectedDrilldown, rowViewer } = this.props
    const currentView = selectedView?.view
    if (!currentView) {
      return
    }
    let drilldown
    switch (currentView) {
      case SELECTED_VIEW_ENUM.TREEMAP:
        // check if treemap view, if so use treemap.selectedDrilldown
        drilldown = selectedDrilldown
        break
      case SELECTED_VIEW_ENUM.ROW_VIEWER:
        // check if row view, if so use the row view dataset and table selection
        drilldown = SelectedDrilldown.of(rowViewer)
        break
      case SELECTED_VIEW_ENUM.LIST:
        drilldown = new SelectedDrilldown()
        break
      default:
        drilldown = new SelectedDrilldown()
        console.log('unknown view selected during search', currentView)
    }
    this.props.updateSearch(tokens, drilldown)
    this.setState({
      overrideCloseMenu: false,
      scrollValuesSection: false,
      scrollColumnsSection: false,
      scrollTablesSection: false,
      scrollDatasetsSection: false,
      hasEnteredSearch: this.state.hasEnteredSearch || true,
    })
  }

  removeChip(event: React.MouseEvent, phrase: Readonly<ChipPhrase>): void {
    if (event) {
      event.preventDefault()
    }

    this.props.removeChipPhrase(phrase)
    this.props.setFilterValue(null)
    this.props.searchInFlight(false)
  }

  handleOnFocus(event?: React.FocusEvent): void {
    if (event) {
      event.preventDefault()
    }

    this.setState((state) => {
      return {
        hasFocus: true,
        overrideCloseMenu: false,
        hasEnteredSearch: false,
      }
    })
  }

  handleOnBlur(event?: React.SyntheticEvent): void {
    if (event) {
      event.preventDefault()
    }

    this.setState((state) => {
      return {
        hasFocus: false,
        overrideCloseMenu: false,
        hasEnteredSearch: false,
      }
    })
  }

  render(): JSX.Element {
    const { classes } = this.props
    const label = 'Find a dataset, table, or a value'
    return (
      <div className={classes.searchContainer}>
        {this.generateChipPhrases(this.props.chipPhrases, classes)}
        <ClickAwayListener onClickAway={this.handleClickAway}>
          <FormControl onSubmit={(event) => event.preventDefault()}>
            <Paper
              component="form"
              className={`${classes.search} ${
                !isEmpty(this.props.chipPhrases) ? classes.minimizeSearch : ''
              }`}>
              <InputBase
                onFocus={this.handleOnFocus}
                // onBlur={this.handleOnBlur}
                autoCorrect="off"
                spellCheck="false"
                inputRef={this.multiSearchInputRef}
                className={`${classes.input} ${
                  this.state.hasFocus ? classes.inputFocus : classes.inputBlur
                }`}
                placeholder={label}
                inputProps={{ 'aria-label': 'treemap omni search' }}
                onChange={(event) => this.updatedDebouncedSearchToken(event)}
              />
              <Divider className={classes.divider} orientation="vertical" />
              <IconButton
                type="submit"
                className={`${classes.iconButton} ${classes.actionBtn}`}
                aria-label={label}>
                <Search />
              </IconButton>
            </Paper>
            <Collapse
              className={classes.overlay}
              in={this.state.hasFocus && !this.state.overrideCloseMenu}>
              <Paper elevation={4} className={classes.results}>
                {this.generateResultsComponent(
                  this.props.search,
                  this.props.summedSearchResult,
                  this.props.searchSuggestions,
                  this.props.datasetSuggestions,
                  this.props.tableSuggestions,
                  this.props.columnSuggestions,
                  classes
                )}
              </Paper>
            </Collapse>
          </FormControl>
        </ClickAwayListener>
      </div>
    )
  }

  /**
   * @return {boolean}
   *  true if the last search contains a value match
   *  or {dataset,table, column} name match
   */
  hasResults(): boolean {
    return (
      !isEmpty(this.props.searchSuggestions) ||
      !isEmpty(this.props.columnSuggestions) ||
      !isEmpty(this.props.tableSuggestions) ||
      !isEmpty(this.props.datasetSuggestions)
    )
  }

  /**
   * row containing dataset, table or column search matches
   * @param el
   * @param curIndex
   * @param lastIndex
   * @param classes
   * @param shouldShowAll
   * @param elementName
   */
  generateNameResultRow(
    hits: Readonly<AggregatedSearchResult>,
    scope: Readonly<ChipPhraseScopeEnum>,
    curIndex: number,
    lastIndex: number,
    classes: Dictionary<string>,
    shouldShowAll = false
  ): JSX.Element {
    const shouldDisplay = curIndex <= this.defaultTopN || shouldShowAll
    if (shouldDisplay) {
      const isLastElement = shouldShowAll
        ? curIndex === lastIndex
        : curIndex === this.defaultTopN || curIndex === lastIndex
      const isHovered = this.optionalHoverCss(hits)
      const classNames = [classes.resultsRowContentList, classes.pointer]
      if (!isLastElement) {
        classNames.push(classes.borderBottom)
      }
      if (isHovered) {
        classNames.push(classes.resultRowHover)
      }

      return (
        <div
          key={curIndex}
          onClick={(event) => this.handleSubmitNamedValue(hits, scope, event)}
          onMouseEnter={(event) => this.handleMouseEnterValue(hits, event)}
          onMouseLeave={(event) => this.handleMouseLeaveValue(event)}
          className={classNames.join(' ')}>
          <span className={classes.resultsRowContentLeft}>{hits?.value}</span>
        </div>
      )
    }
    return <Fragment key={curIndex}></Fragment>
  }

  generateScrollLink(
    showingAll: boolean,
    toggleFn: (event: React.MouseEvent) => void,
    classes: Dictionary<string>
  ): JSX.Element {
    return (
      <div className={classes.scrollAction}>
        <Button
          size="small"
          variant="outlined"
          href="#"
          onClick={(event) => toggleFn(event)}>
          Show {!showingAll ? 'All' : 'Less'}
        </Button>
      </div>
    )
  }

  optionalHoverCss(currentElement: AggregatedSearchResult): boolean {
    if (
      !currentElement ||
      !currentElement.id ||
      !this.props.hoveredValue ||
      !this.props.hoveredValue.id
    ) {
      return false
    }

    return this.props.hoveredValue.id === currentElement.id
  }

  generateChipPhrases(
    chipPhrases: Readonly<ChipPhrase[]>,
    classes: Dictionary<any>
  ): JSX.Element {
    if (isEmpty(chipPhrases)) {
      return <Fragment></Fragment>
    }

    return (
      <span
        className={`${classes.chips} ${
          this.props.isInFlight ? classes.loading : ''
        }`}>
        {chipPhrases.map((chipPhrase, i) => (
          <Chip
            className={classes.phraseChip}
            key={i}
            label={`${chipPhrase?.scope}: ${chipPhrase?.phrase}`}
            onDelete={(event) => this.removeChip(event, chipPhrase)}
          />
        ))}
      </span>
    )
  }

  generateResultsComponent(
    search: Readonly<string>,
    summedSearchResult: Readonly<AggregatedSearchResult>,
    searchResults: Readonly<AggregatedSearchResult[]>,
    datasetResults: Readonly<AggregatedSearchResult[]>,
    tableResults: Readonly<AggregatedSearchResult[]>,
    columnResults: Readonly<AggregatedSearchResult[]>,
    classes: Dictionary<any>
  ): JSX.Element {
    if (!this.state.hasEnteredSearch) {
      // no search has been entered, there is nothing to show
      return <Fragment></Fragment>
    }

    if (this.props.isSearchInFlight === true) {
      // search has been entered and we are fetching results from server
      return (
        <div className={classes.results}>
          <div className={classes.resultsMessage}>Processing...</div>
        </div>
      )
    }

    if (!this.hasResults()) {
      // search results have come back empty
      return (
        <div className={classes.results}>
          <div className={classes.resultsMessage}>No Results</div>
        </div>
      )
    }

    // display have search results
    const allElements = [...summedSearchResult.elements]
    const allHits: AggregatedSearchResult = {
      ...summedSearchResult,
      elements: [...allElements],
    }
    const hasValueSearchError = isUndefined(
      searchResults.find((el) => el.error)
    )
    return (
      <div className={classes.results}>
        {allHits.count > 0 && (
          <div key="search-all-0" className={classes.resultsRow}>
            <div className={classes.resultsRowLabel}>Search All</div>
            <div className={classes.resultsRowContent}>
              <div
                onClick={(event) =>
                  this.handleSubmitValue(
                    allHits,
                    ChipPhraseScopeEnum.ALL,
                    event
                  )
                }
                onMouseEnter={(event) =>
                  this.handleMouseEnterValue(allHits, event)
                }
                onMouseLeave={(event) => this.handleMouseLeaveValue(event)}
                className={[
                  classes.resultsRowContentList,
                  classes.pointer,
                  this.optionalHoverCss(allHits) ? classes.resultRowHover : '',
                ].join(' ')}>
                <span className={classes.resultsRowContentLeft}>{search}</span>
                <span className={classes.resultsRowContentRight}>
                  {nicefyNumber(allHits.count)}
                </span>
              </div>
            </div>
          </div>
        )}
        {searchResults.length > 0 && (
          <div key="search-values-0" className={classes.resultsRow}>
            <div className={classes.resultsRowLabel}>
              <div className={classes.resultsRowLabelTop}>
                Values {!hasValueSearchError && `(${searchResults.length})`}
              </div>
              {searchResults.length - 1 > this.defaultTopN &&
                this.generateScrollLink(
                  this.state.scrollValuesSection,
                  this.toggleShowAllValues.bind(this),
                  classes
                )}
            </div>
            <div className={classes.resultsRowContent}>
              {searchResults.map((el, index) => {
                if (el?.error) {
                  return <Alert severity="error">{el.error}</Alert>
                }
                const shouldDisplay =
                  index <= this.defaultTopN || this.state.scrollValuesSection
                if (!shouldDisplay) {
                  return
                }
                const isLastElement = this.state.scrollValuesSection
                  ? index === searchResults.length - 1
                  : index === this.defaultTopN ||
                    index === searchResults.length - 1
                const isHovered = this.optionalHoverCss(el)
                const classNames = [
                  classes.resultsRowContentList,
                  classes.pointer,
                ]
                if (!isLastElement) {
                  classNames.push(classes.borderBottom)
                }
                if (isHovered) {
                  classNames.push(classes.resultRowHover)
                }
                return (
                  <div
                    onClick={(event) =>
                      this.handleSubmitValue(
                        el,
                        ChipPhraseScopeEnum.VALUES,
                        event
                      )
                    }
                    onMouseEnter={(event) =>
                      this.handleMouseEnterValue(el, event)
                    }
                    onMouseLeave={(event) => this.handleMouseLeaveValue(event)}
                    key={index}
                    className={classNames.join(' ')}>
                    <span className={classes.resultsRowContentLeft}>
                      {el.value}
                    </span>
                    <span className={classes.resultsRowContentRight}>
                      {nicefyNumber(el.count)}
                    </span>
                  </div>
                )
              })}
            </div>
          </div>
        )}
        {this.generateDatasetSearchResults(datasetResults, classes)}
        {this.generateTableSearchResults(tableResults, classes)}
        {this.generateColumnSearchResults(columnResults, classes)}
      </div>
    )
  }

  generateColumnSearchResults(
    results: Readonly<AggregatedSearchResult[]>,
    classes: Dictionary<any>
  ): JSX.Element {
    if (!results || results.length === 0) {
      return <Fragment></Fragment>
    }

    const hasError = isUndefined(results.find((el) => el.error))
    return (
      <div key="search-columns-1" className={classes.resultsRow}>
        <div className={classes.resultsRowLabel}>
          <div className={classes.resultsRowLabelTop}>
            Columns {!hasError && `(${results.length})`}
          </div>
          {results.length - 1 > this.defaultTopN &&
            this.generateScrollLink(
              this.state.scrollColumnsSection,
              this.toggleShowAllColumns.bind(this),
              classes
            )}
        </div>
        <div className={classes.resultsRowContent}>
          {results.map((el, index) => {
            if (el?.error) {
              return <Alert severity="error">{el.error}</Alert>
            }

            return this.generateNameResultRow(
              el,
              ChipPhraseScopeEnum.COLUMN_TITLE,
              index,
              results.length - 1,
              classes,
              this.state.scrollColumnsSection
            )
          })}
        </div>
      </div>
    )
  }

  generateTableSearchResults(
    results: Readonly<AggregatedSearchResult[]>,
    classes: Dictionary<any>
  ): JSX.Element {
    if (!results || isEmpty(results)) {
      return <Fragment></Fragment>
    }

    const hasError = isUndefined(results.find((el) => el.error))
    return (
      <div key="search-tables-1" className={classes.resultsRow}>
        <div className={classes.resultsRowLabel}>
          <div className={classes.resultsRowLabelTop}>
            Tables {!hasError && `(${results.length})`}
          </div>
          {results.length - 1 > this.defaultTopN &&
            this.generateScrollLink(
              this.state.scrollTablesSection,
              this.toggleShowAllTables.bind(this),
              classes
            )}
        </div>
        <div className={classes.resultsRowContent}>
          {results.map((el, index) => {
            if (el?.error) {
              return <Alert severity="error">{el.error}</Alert>
            }
            return this.generateNameResultRow(
              el,
              ChipPhraseScopeEnum.TABLE_TITLE,
              index,
              results.length - 1,
              classes,
              this.state.scrollTablesSection
            )
          })}
        </div>
      </div>
    )
  }

  generateDatasetSearchResults(
    results: Readonly<AggregatedSearchResult[]>,
    classes: Dictionary<any>
  ): JSX.Element {
    if (!results || isEmpty(results)) {
      return <Fragment></Fragment>
    }

    const hasError = isUndefined(results.find((el) => el.error))
    return (
      <div key="search-datasets-1" className={classes.resultsRow}>
        <div className={classes.resultsRowLabel}>
          <div className={classes.resultsRowLabelTop}>
            Datasets {!hasError && `(${results.length})`}
          </div>
          {results.length - 1 > this.defaultTopN &&
            this.generateScrollLink(
              this.state.scrollDatasetsSection,
              this.toggleShowAllDatasets.bind(this),
              classes
            )}
        </div>
        <div className={classes.resultsRowContent}>
          {results.map((el, index) => {
            if (el?.error) {
              return <Alert severity="error">{el.error}</Alert>
            }
            return this.generateNameResultRow(
              el,
              ChipPhraseScopeEnum.DATASET_TITLE,
              index,
              results.length - 1,
              classes,
              this.state.scrollDatasetsSection
            )
          })}
        </div>
      </div>
    )
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = ({ palette, spacing }: Theme) =>
  createStyles({
    root: {},
    search: {
      margin: '2px 4px',
      display: 'flex',
      alignItems: 'center',
      width: '40vw',
      maxWidth: '40vw',
      height: 38,
    },
    minimizeSearch: {
      width: 60,
    },
    input: {
      flex: 1,
      margin: '4px 0px 4px 4px',
      marginLeft: '8px',
    },
    iconButton: {
      padding: 10,
    },
    divider: {
      height: 28,
      margin: 4,
    },
    resultsMessage: {
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      minHeight: '350px',
    },
    results: {
      padding: 0,
      maxHeight: '75vh',
      display: 'flex',
      flexFlow: 'column',
      overflowY: 'auto',
    },
    resultsRow: {
      flex: '1',
      display: 'flex',
      flexFlow: 'row',
      borderBottom: '1px solid #ccc',
      // minHeight: '48px',
    },
    pointer: {
      cursor: 'pointer',
    },
    resultsRowLabel: {
      flex: '0 0 25%',
      borderRight: '1px solid #ccc',
      padding: '16px',
      background: '#eee',
      display: 'flex',
      flexFlow: 'column',
      justifyContent: 'space-between',
    },
    resultsRowLabelTop: {
      // flex: '1 1 auto',
      position: 'sticky',
      top: '16px',
    },
    resultsRowContent: {
      flex: '1 1 auto',
      display: 'flex',
      flexFlow: 'column',
      '& .MuiAlert-root': {
        flex: 1,
        borderRadius: 0,
        alignItems: 'center',
      },
    },
    resultsRowContentList: {
      display: 'flex',
      padding: '16px',
      margin: 0,
    },
    borderBottom: {
      borderBottom: '1px solid #ccc',
    },
    resultsRowContentLeft: {
      flex: '1 1 auto',
      textOverflow: 'ellipsis',
      wordBreak: 'break-all',
    },
    resultsRowContentRight: {
      flex: '0 0 auto',
      textAlign: 'end',
      margin: 'auto auto',
      paddingLeft: '8px',
      opacity: 0.4,
    },
    scrollAction: {
      position: 'sticky',
      bottom: '16px',
      marginTop: '40px',
    },
    overlay: {
      position: 'absolute',
      zIndex: 10001,
      top: '42px',
      width: '100%',
    },
    resultRowHover: {
      backgroundColor: DARK_GREY,
    },
    chips: {
      margin: 'auto 0',
    },
    searchContainer: {
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
    },
    phraseChip: {
      fontWeight: 600,
      boxShadow:
        '0px 1px 5px 0px rgba(0, 0, 0, 0.2), 0px 2px 2px 0px rgba(0, 0, 0, 0.14), 0px 3px 1px -2px rgba(0, 0, 0, 0.12)',
      height: 38,
      background: SEARCH_CHIP_BACKGROUND,
      borderRadius: 5,
      border: `1px solid ${SEARCH_CHIP_BORDER}`,
      '& > svg': {
        color: SEARCH_CHIP_COLOR,
      },
      '& .MuiChip-label': {
        maxWidth: '33vw',
      },
    },
    actionBtn: {
      color: ACTION_BTN_COLOR,
    },
    inputBlur: {
      width: '55px',
    },
    inputFocus: {
      width: '40vw',
      maxWidth: '40vw',
    },
    loading: {
      opacity: 0.6,
      cursor: 'progress',
      '& > *': {
        pointerEvents: 'none',
      },
    },
  })

const mapStateToProps = (state: StoreState) => ({
  ...state.multisearch,
  search: getLastSearchToken(state),
  searchSuggestions: getDisplayedSearchResults(state),
  tableSuggestions: getDisplayedTableSuggestions(state),
  columnSuggestions: getDisplayedColumnSuggestions(state),
  summedSearchResult: getSummedSearchResults(state),
  hoveredValue: getHoveredValue(state),
  selectedDrilldown: getSelectedDrilldown(state),
  selectedView: state.treemap.selectedView,
  rowViewer: state.treemap.rowViewer,
  isInFlight: state.treemap.isSearchInFlight,
})

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators(
    {
      updateSearch,
      addChipPhrase,
      removeChipPhrase,
      setHoveredValue,
      setFilterValue,
      updateSelectedDrilldownWithRefresh,
      clearSearchSuggestions,
      searchInFlight,
    },
    dispatch
  )

const connector = connect(mapStateToProps, mapDispatchToProps)
export default withStyles(styles)(connector(MultiSearch))
