package com.rsmart.oae.user.api.lostpwd;

import org.apache.sling.api.SlingHttpServletRequest;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;

public interface LostPasswordService {

  static final String LOST_PASSWORD_GUID_PARAM = "guid";
  static final String LOST_PASSWORD_RT = "rsmart/lostPassword";
  

  /**
   * used to trigger the password recovery process... 
   * this sends an email with a link to the user's registered and verified address
   * this will throw an exception if the username or email address isn't found
   * @param request
   * @param username or email address of the account to retrieve the password for
   */
  void recoverPassword(SlingHttpServletRequest request, String username);
  
  /**
   * this will verify the guid passed in with the recovery object
   * throws an exception if not found or doesn't match
   * @param request
   * @param guid the password recovery guid passed in
   * @param passwordRecovery the node that contains the guid
   * @return the url to redirect to
   * @throws LostPasswordException if the guid doesn't match or if the file has already been called
   */
  String checkPasswordRetrieval(SlingHttpServletRequest request, String guid, Content passwordRecovery) throws LostPasswordException;
  
  /**
   * this will verify the guid 
   * change the user's current password 
   * then log in the user for the session
   * @param session
   * @param guid
   * @param passwordRecovery the node that contains the guid (this will be deleted)
   * @param newPassword change the user's password to this
   * @return the user that has been authenticated
   * @throws LostPasswordException
   */
  User changePassword(Session session, String guid, Content passwordRecovery, String newPassword) throws LostPasswordException;
  
  
}
