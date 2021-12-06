/**
*  Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
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
**/
package helpers;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Properties;

public class EmailNotifier {

  private static final String FROM = "";
  private static final String SMTP_HOST = "";

  private static final String[] LOGIN_NOTIFICATIONS =
      new String[] {
      };

  private static final String[] WHITELIST_ENVIRONMENTS =
      new String[] {
      };

  public static Boolean sendEmail(String[] to, String subject, String contents) {
    String host = "localhost";
    Properties properties = System.getProperties();
    properties.setProperty("mail.smtp.host", SMTP_HOST);
    Session session = Session.getDefaultInstance(properties);

    try {
      MimeMessage message = new MimeMessage(session);
      message.setFrom(new InternetAddress(FROM));
      for (String address : to) {
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(address));
      }
      message.setSubject(subject);
      message.setText(contents);
      Transport.send(message);
      return true;
    } catch (MessagingException e) {
      e.printStackTrace();
      return false;
    }
  }

  public static Boolean sendEmailFromArbitrary(String[] to, String from, String subject, String contents) {
    String host = "localhost";
    Properties properties = System.getProperties();
    properties.setProperty("mail.smtp.host", SMTP_HOST);
    Session session = Session.getDefaultInstance(properties);

    try {
      MimeMessage message = new MimeMessage(session);
      message.setFrom(new InternetAddress(from));
      for (String address : to) {
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(address));
      }
      message.setSubject(subject);
      message.setText(contents);
      Transport.send(message);
      return true;
    } catch (MessagingException e) {
      e.printStackTrace();
      return false;
    }
  }

  public static Boolean sendLoginNotification(String username, String environment) {
    if (Arrays.asList(WHITELIST_ENVIRONMENTS).contains(environment)) {
      String subject = "User Login Notification (" + hashUsername(username) + ")";
      String contents = username + " has logged into " + environment;
      return sendEmail(LOGIN_NOTIFICATIONS, subject, contents);
    }
    return true;
  }

  public static String hashUsername(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] messageDigest = md.digest(input.getBytes());
      BigInteger no = new BigInteger(1, messageDigest);
      String hashtext = no.toString(16);
      while (hashtext.length() < 32) {
        hashtext = "0" + hashtext;
      }
      return hashtext.substring(0, Math.min(hashtext.length(), 8));
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}
