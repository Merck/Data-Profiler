package org.apache.hive.service.auth;

/*-
 * 
 * spark-sql
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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import javax.naming.AuthenticationNotSupportedException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.security.sasl.AuthenticationException;

public class SparkSqlLdapAuthenticator implements PasswdAuthenticationProvider {

  public SparkSqlLdapAuthenticator() {}

  private Properties initialize() throws IOException {
    Properties props = new Properties();
    props.load(new FileInputStream("/etc/ldap.properties"));
    return props;
  }

  @Override
  public void Authenticate(String user, String password) throws AuthenticationException {
    Properties props = null;
    try {
      props = initialize();
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(-1);
    }

    // Input master user/pass
    if (user.equals("") && password.equals("")) {
      return;
    }

    // Check that user has access to this instance
    try {
      String defaultDb = System.getenv("DEFAULT_DATABASE");
      if (!defaultDb.equalsIgnoreCase(user)) {
        throw new AuthenticationException(
            String.format("User %s does not have authorization for this instance!!!", user));
      }
    } catch (Exception fileNotFoundException) {
      throw new AuthenticationException("User does not have authorization for this instance!!!");
    }

    String url = props.getProperty("ldap.url");
    String base = props.getProperty("ldap.base");
    String auth = props.getProperty("ldap.security.authentication");
    String serviceUserDn = props.getProperty("ldap.service.user.dn");
    String serviceUserPassword = props.getProperty("ldap.service.user.password");
    Map<String, String> env = System.getenv();

    String userAccountAttribute = props.getProperty("ldap.user.account.attribute");
    String searchTimeLimit = props.getProperty("ldap.search.timeout", "30000");

    DirContext ctx = null;
    try {
      ctx = bindConnect(url, serviceUserDn, serviceUserPassword, auth);
      SearchControls searchControls = new SearchControls();
      searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
      searchControls.setTimeLimit(Integer.parseInt(searchTimeLimit));
      String[] attrIDs = {userAccountAttribute};
      searchControls.setReturningAttributes(attrIDs);

      String searchFilter = "(" + userAccountAttribute + "=" + user + ")";
      NamingEnumeration<SearchResult> results = ctx.search(base, searchFilter, searchControls);
      ctx.close();

      if (results.hasMore()) {
        SearchResult result = results.next();
        String distinguishedName = result.getNameInNamespace();
        DirContext usrCtx = bindConnect(url, distinguishedName, password, auth);
        usrCtx.close();
      } else {
        throw new AuthenticationException(String.format("User '%s' is unknown.", user));
      }

      System.out.println("Authentication Successful.");
      return;

    } catch (NamingException e) {
      System.out.println(e.getMessage());
    }

    System.out.println("Authentication Failed.");

    throw new AuthenticationException("Error validating user.");
  }

  private DirContext bindConnect(String url, String username, String password, String auth)
      throws NamingException {
    Hashtable<String, Object> env = new Hashtable<>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put(Context.PROVIDER_URL, url);
    env.put(Context.SECURITY_AUTHENTICATION, auth);
    env.put(Context.SECURITY_PRINCIPAL, username);
    env.put(Context.SECURITY_CREDENTIALS, password);

    try {
      DirContext ctx = new InitialDirContext(env);
      System.out.println("Connected.");
      return ctx;

    } catch (AuthenticationNotSupportedException ex) {
      System.out.println("The authentication is not supported by the server.");
      throw ex;
    } catch (javax.naming.AuthenticationException ex) {
      System.out.println("Incorrect password or username.");
      throw ex;
    } catch (NamingException ex) {
      System.out.println("Error when trying to create the context.");
      throw ex;
    }
  }
}
