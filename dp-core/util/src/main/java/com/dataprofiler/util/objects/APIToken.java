package com.dataprofiler.util.objects;

/*-
 * 
 * dataprofiler-util
 *
 * Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
 *
 * 	Licensed to the Apache Software Foundation (ASF) under one
 * 	or more contributor license agreements. See the NOTICE file
 * 	distributed with this work for additional information
 * 	regarding copyright ownership. The ASF licenses this file
 * 	to you under the Apache License, Version 2.0 (the
 * 	"License"); you may not use this file except in compliance
 * 	with the License. You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * 	Unless required by applicable law or agreed to in writing,
 * 	software distributed under the License is distributed on an
 * 	"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * 	KIND, either express or implied. See the License for the
 * 	specific language governing permissions and limitations
 * 	under the License.
 * 
 */

import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Const;
import com.dataprofiler.util.Context;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Map.Entry;
import java.util.Random;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;

/** These tokens are used */
public class APIToken extends AccumuloObject<APIToken> {
  private static final int TOKEN_LENGTH = 64;
  private static final String ALLOWED_CHARS = "ABCDEFGJKLMNPRSTUVWXYZ0123456789";

  private String token;
  private String username;

  public APIToken() {
    super(Const.ACCUMULO_API_TOKENS_TABLE_ENV_KEY);
  }

  public static ObjectScannerIterable<APIToken> list(Context context) {
    APIToken tok = new APIToken();
    return tok.scan(context);
  }

  public static APIToken fetchByToken(Context context, String token) {
    APIToken tok = new APIToken();
    tok.token = token;

    return tok.fetch(context, tok.createAccumuloKey());
  }

  public static APIToken fetchByUsername(Context context, String username) {
    // Nothing to do other than a full scan
    for (APIToken tok : new APIToken().scan(context)) {
      if (tok.username.equals(username)) {
        return tok;
      }
    }

    return null;
  }

  public static Boolean deleteByToken(Context context, String token) throws BasicAccumuloException {
    APIToken tok = fetchByToken(context, token);
    tok.destroy(context);
    return true;
  }

  public static APIToken createToken(String username) {
    // Allowed characters
    Random random = new SecureRandom();
    char[] buf = new char[TOKEN_LENGTH];
    for (int idx = 0; idx < buf.length; ++idx)
      buf[idx] = ALLOWED_CHARS.charAt(random.nextInt(ALLOWED_CHARS.length()));

    APIToken tok = new APIToken();
    tok.username = username;
    tok.token = new String(buf);

    return tok;
  }

  public static APIToken createTokenIfNeeded(Context context, String username) {
    APIToken tok = fetchByUsername(context, username);

    if (tok == null) {
      return createToken(username);
    } else {
      return tok;
    }
  }

  @Override
  public APIToken fromEntry(Entry<Key, Value> entry) {
    APIToken t = new APIToken();
    t.token = entry.getKey().getRow().toString();
    t.username = entry.getValue().toString();

    t.updatePropertiesFromEntry(entry);

    return t;
  }

  @Override
  public Key createAccumuloKey() throws InvalidDataFormat {
    return new Key(token);
  }

  @Override
  public Value createAccumuloValue() throws InvalidDataFormat, IOException {
    return new Value(username.getBytes());
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  @Override
  public String toString() {
    return "APIToken{" + "token='" + token + '\'' + ", username='" + username + '\'' + '}';
  }
}
