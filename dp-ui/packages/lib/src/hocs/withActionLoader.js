import React from 'react'
import { bindActionCreators } from 'redux'
import CircularProgress from '@material-ui/core/CircularProgress'
import { connect } from 'react-redux'
import { get } from 'lodash'
import { parseQueryString } from '../helpers/uriHelpers'

export default function withActionLoader(WrappedComponent, actionsObj) {
  class ActionLoader extends React.Component {
    state = {
      initialized: false,
    }

    async componentDidMount() {
      for (const [idx, actionObj] of actionsObj.entries()) {
        if (actionObj.actionParams) {
          const actionParams = actionObj.actionParams.map((actionParam) => {
            if (actionParam.startsWith('match.')) {
              return decodeURIComponent(get(this.props, actionParam))
            } else {
              return parseQueryString(this.props.location.search)[actionParam]
            }
          })
          await this.props[idx](...actionParams)
        } else {
          await this.props[idx]()
        }
      }
      this.setState({ initialized: true })
    }

    render() {
      if (!this.state.initialized) return <CircularProgress thickness={7} />
      return <WrappedComponent {...this.props} />
    }
  }
  const mapDispatchToProps = (dispatch) =>
    bindActionCreators({ ...actionsObj.map((a) => a.action) }, dispatch)

  return connect(null, mapDispatchToProps)(ActionLoader)
}
