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
import { isEmpty, isUndefined } from 'lodash'
import { HighlightToken } from '../models/HighlightToken'

export default class HighlightTokenizerService {
  tokenizeAndMatchLongestSubstring(
    content: string,
    matches: Set<string>
  ): Array<HighlightToken> {
    if (!content) {
      return []
    }
    if (this.isEmptySet(matches)) {
      return this.noMatches(content)
    }

    const tokens = this.tokenizeAndMatch(content, matches)
    // pull consecutive match tokens together
    let consecutiveTokens = []
    let current: HighlightToken = {
      isMatch: tokens[0].isMatch,
      token: '',
    }
    for (let i = 0; i < tokens.length; i++) {
      const letterToken = tokens[i]
      if (letterToken.isMatch === current.isMatch) {
        current.token += letterToken.token
      } else {
        consecutiveTokens = consecutiveTokens.concat(current)
        current = {
          isMatch: letterToken.isMatch,
          token: letterToken.token,
        }
      }
    }
    if (current.token !== '') {
      consecutiveTokens = consecutiveTokens.concat(current)
    }
    return consecutiveTokens
  }

  tokenizeAndMatch(
    content: string,
    matches: Set<string>
  ): Array<HighlightToken> {
    if (!content) {
      return []
    }
    if (this.isEmptySet(matches)) {
      return this.noMatches(content)
    }

    const grams = content.split('')
    const matchers = Array.from(matches)?.map((match) => new MatchState(match))
    const tokens: HighlightToken[] = [].fill(undefined, 0, grams.length)
    for (let i = 0; i < grams.length; i++) {
      const letter = grams[i]
      tokens[i] = {
        token: letter,
        isMatch: false,
      }
      matchers.forEach((matcherState) => {
        const isFullMatch = matcherState.advanceToken(letter)
        if (isFullMatch) {
          const foundMatch = matcherState.match
          const lookBackIndex = i + 1 - foundMatch.length
          if (lookBackIndex < 0) {
            throw new Error('illegal state error, look back index is < 0')
          }
          tokens
            .slice(lookBackIndex, i + 1)
            .forEach((token) => (token.isMatch = true))
        }
      })
    }
    return tokens
  }

  noMatches(content?: string): Array<HighlightToken> {
    return [
      {
        isMatch: false,
        token: content,
      },
    ]
  }

  isEmptySet(set: Set<string>): boolean {
    if (!set) {
      return true
    }

    return (
      Array.from(set)
        .filter((el) => !isUndefined(el))
        .filter((el) => !isEmpty(el.trim())).length === 0
    )
  }

  normalizeMatchTerms(matchTerms: Readonly<string[]>): Set<string> {
    return new Set(
      (matchTerms || [])
        .filter((el) => !isUndefined(el))
        .map((el) => el?.trim())
        .filter((el) => !isEmpty(el))
    )
  }
}

class MatchState {
  match: string
  isMatchInProgress: boolean
  isFullMatch: boolean
  curIndex: number
  constructor(match: string) {
    this.isMatchInProgress = false
    this.match = match
    this.reset()
  }

  reset(): void {
    this.curIndex = 0
    this.isMatchInProgress = false
    this.isFullMatch = false
  }

  /**
   *
   * @param letter
   */
  advanceToken(letter: string): boolean {
    if (!letter || letter.length === 0) {
      return false
    }
    if (letter.length > 1) {
      throw new Error(
        'illegal state error in match table, letter is > 1 character'
      )
    }

    const letter0 = letter[0]
    if (letter0.toLowerCase() === this.match[this.curIndex].toLowerCase()) {
      this.curIndex += 1
      this.isMatchInProgress = true
      if (this.curIndex === this.match.length) {
        //found a full match, reset state
        this.reset()
        this.isFullMatch = true
      }
    } else {
      this.reset()
    }
    return this.isFullMatch
  }
}
