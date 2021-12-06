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
import HighlightTokenizerService from '../../../../components/HighlightSentence/services/HighlightTokenizerService'

describe('given a higlight tokenizer service', () => {
  let service: HighlightTokenizerService

  beforeEach(() => {
    service = new HighlightTokenizerService()
  })

  test('then expect a service', () => {
    expect(service).toBeTruthy()
  })

  test('then expect to tokenize and match - single leading term', () => {
    const content = 'dog bite'
    const match = 'dog'
    const tokens = service.tokenizeAndMatch(content, new Set([match]))
    expect(tokens).toBeTruthy()
    expect(tokens.length).toBe(8)
    tokens.slice(0, 2).forEach((token) => {
      expect(token).toBeTruthy()
      expect(token.isMatch).toBeTruthy()
    })
    tokens.slice(3, tokens.length).forEach((token) => {
      expect(token).toBeTruthy()
      expect(token.isMatch).toBeFalsy()
    })
  })

  test('then expect to tokenize and match - single trailing term', () => {
    const content = 'barking dog'
    const match = 'dog'
    const tokens = service.tokenizeAndMatch(content, new Set([match]))
    expect(tokens).toBeTruthy()
    expect(tokens.length).toBe(11)
    tokens.slice(0, 8).forEach((token) => {
      expect(token).toBeTruthy()
      expect(token.isMatch).toBeFalsy()
    })
    tokens.slice(10, tokens.length).forEach((token) => {
      expect(token).toBeTruthy()
      expect(token.isMatch).toBeTruthy()
    })
  })

  test('then expect to tokenize and match - single embedded term', () => {
    const content = 'adoggoneit'
    const match = 'dog'
    const tokens = service.tokenizeAndMatch(content, new Set([match]))
    expect(tokens).toBeTruthy()
    expect(tokens.length).toBe(10)
    tokens.slice(0, 1).forEach((token) => {
      expect(token).toBeTruthy()
      expect(token.isMatch).toBeFalsy()
    })
    tokens.slice(1, 4).forEach((token) => {
      expect(token).toBeTruthy()
      expect(token.isMatch).toBeTruthy()
    })
    tokens.slice(4, tokens.length).forEach((token) => {
      expect(token).toBeTruthy()
      expect(token.isMatch).toBeFalsy()
    })
  })

  test('then expect to tokenize and match longest substring - single embedded term', () => {
    const content = 'adoggoneit'
    const match = 'dog'
    const tokens = service.tokenizeAndMatchLongestSubstring(
      content,
      new Set([match])
    )
    expect(tokens).toBeTruthy()
    expect(tokens.length).toBe(3)
    tokens.slice(0, 1).forEach((token) => {
      expect(token).toBeTruthy()
      expect(token.isMatch).toBeFalsy()
      expect(token.token).toBe('a')
    })
    tokens.slice(1, 2).forEach((token) => {
      expect(token).toBeTruthy()
      expect(token.isMatch).toBeTruthy()
      expect(token.token).toBe('dog')
    })
    tokens.slice(3, tokens.length).forEach((token) => {
      expect(token).toBeTruthy()
      expect(token.isMatch).toBeFalsy()
      expect(token.token).toBe('goneit')
    })
  })

  test('then expect to tokenize and match longest substring - no search/match terms', () => {
    const content = 'adoggoneit'
    const tokens = service.tokenizeAndMatchLongestSubstring(
      content,
      new Set([''])
    )
    expect(tokens).toBeTruthy()
    expect(tokens.length).toBe(1)
  })

  test('then expect to tokenize and match longest substring - two embeddeded terms', () => {
    const content = 'adoggoneit'
    const matches = new Set(['dog', 'one'])
    const tokens = service.tokenizeAndMatchLongestSubstring(content, matches)
    expect(tokens).toBeTruthy()
    expect(tokens.length).toBe(5)
    tokens.slice(0, 1).forEach((token) => {
      expect(token).toBeTruthy()
      expect(token.isMatch).toBeFalsy()
      expect(token.token).toBe('a')
    })
    tokens.slice(1, 2).forEach((token) => {
      expect(token).toBeTruthy()
      expect(token.isMatch).toBeTruthy()
      expect(token.token).toBe('dog')
    })
    tokens.slice(2, 3).forEach((token) => {
      expect(token).toBeTruthy()
      expect(token.isMatch).toBeFalsy()
      expect(token.token).toBe('g')
    })
    tokens.slice(3, 4).forEach((token) => {
      expect(token).toBeTruthy()
      expect(token.isMatch).toBeTruthy()
      expect(token.token).toBe('one')
    })
    tokens.slice(5, 6).forEach((token) => {
      expect(token).toBeTruthy()
      expect(token.isMatch).toBeFalsy()
      expect(token.token).toBe('it')
    })
  })
})
