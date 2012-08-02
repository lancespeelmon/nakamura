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
   * will determine if the user and the guid match and, if so, clear it and make the user a "verified" user (whatever that means)
   * @param token
   * @param user
   */
  void clearVerification(Session session, String guid, Content verification) throws EmailVerificationException;
  
  boolean hasVerified(Session session, User user);
  
  void seenWarning(Session session, Content verification);

  void changeEmail(Session session, User user, Content verification,
      final Map<String, Object[]> parameters);

  void cancelChangeEmail(SlingHttpServletRequest request, Session session,
      User user, Content verification);
  
}
