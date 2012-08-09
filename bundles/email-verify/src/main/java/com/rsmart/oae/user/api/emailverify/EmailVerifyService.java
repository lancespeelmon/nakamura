/**
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.rsmart.oae.user.api.emailverify;

import org.apache.sling.api.SlingHttpServletRequest;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;

import java.util.Map;

public interface EmailVerifyService {

  static final String EMAIL_VERIFY_GUID_PARAM = "guid";
  static final String EMAIL_VERIFY_RT = "rsmart/emailVerify";

  /**
   * 
   * @param session
   * @param user
   * @param parameters
   * @throws EmailVerificationException
   */
  void sendVerifyEmail(final Session session, final User user,
      final Map<String, Object[]> parameters);

  /**
   * will determine if the user and the guid match and, if so, clear it and make the user
   * a "verified" user (whatever that means)
   * 
   * @param token
   * @param user
   */
  void clearVerification(Session session, String guid, Content verification)
      throws EmailVerificationException;

  boolean hasVerified(Session session, User user);

  void seenWarning(Session session, Content verification);

  void changeEmail(Session session, User user, Content verification,
      final Map<String, Object[]> parameters);

  void cancelChangeEmail(SlingHttpServletRequest request, Session session, User user,
      Content verification);

}
