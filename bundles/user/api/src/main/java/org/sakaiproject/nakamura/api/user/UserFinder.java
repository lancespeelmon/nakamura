package org.sakaiproject.nakamura.api.user;

import java.util.Set;

public interface UserFinder {
  
  /**
   * 
   * @param name
   * @return Set of userIds
   * @throws Exception
   */
  Set<String> findUsersByName(String name) throws Exception;
  
  /**
   * find all the users with the specified email address
   * 
   * @param email
   * @return Set of userIds
   * @throws Exception
   */
  Set<String> findUsersByEmail(String email) throws Exception;
  
  /**
   * 
   * @param name
   * @return true if one or more users by that name found
   * false if not
   * @throws Exception
   */
  boolean userExists(String name) throws Exception;
  
  /**
   * 
   * @param email
   * @return true if there is someone with this email
   * @throws Exception
   */
  boolean userWithEmailExists(String email) throws Exception;
  
  /**
   * 
   * @return set with all users
   * @throws Exception
   */
  Set<String> allUsers() throws Exception;
  
}
