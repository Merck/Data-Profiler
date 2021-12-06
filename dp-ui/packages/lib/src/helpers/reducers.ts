import { useReducer, useRef, useCallback } from 'react'

export const persistingDispatch =
  (reducer: any, localStorageKey: string) => (state, action) => {
    const newState = reducer(state, action)
    window.localStorage.setItem(localStorageKey, JSON.stringify(newState))
    return newState
  }

export const hydrate = (key: string, defaultValue: any) => {
  const data = window.localStorage.getItem(key)
  return data ? JSON.parse(data) : defaultValue
}

export const useMemoizedReducer = (reducer: any, initState: any) => {
  const lastState = useRef(initState)
  const getState = useCallback(() => lastState.current, [])
  return [
    ...useReducer(
      (state, action) => (lastState.current = reducer(state, action)),
      initState
    ),
    getState,
  ]
}
