package com.rsmart.oae.user.api.lostpwd;

public class UserNotFoundLostPasswordException extends LostPasswordException {

  /**
   * 
   */
  private static final long serialVersionUID = 1052615815702685178L;

  public UserNotFoundLostPasswordException() {
    // TODO Auto-generated constructor stub
  }

  public UserNotFoundLostPasswordException(String message, Throwable cause) {
    super(message, cause);
    // TODO Auto-generated constructor stub
  }

  public UserNotFoundLostPasswordException(String message) {
    super(message);
    // TODO Auto-generated constructor stub
  }

  public UserNotFoundLostPasswordException(Throwable cause) {
    super(cause);
    // TODO Auto-generated constructor stub
  }

}
