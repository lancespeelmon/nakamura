package com.rsmart.oae.user.api.lostpwd;

public class InvalidLinkLostPasswordException extends LostPasswordException {

  /**
   * 
   */
  private static final long serialVersionUID = 3096808578406138180L;

  public InvalidLinkLostPasswordException() {
    super();
  }

  public InvalidLinkLostPasswordException(String arg0, String redirectLink) {
    super(arg0, redirectLink);
  }

  public InvalidLinkLostPasswordException(String message, Throwable cause,
      String redirectLink) {
    super(message, cause, redirectLink);
  }

  public InvalidLinkLostPasswordException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidLinkLostPasswordException(String message) {
    super(message);
  }

  public InvalidLinkLostPasswordException(Throwable cause, String redirectLink) {
    super(cause, redirectLink);
  }

  public InvalidLinkLostPasswordException(Throwable cause) {
    super(cause);
  }
  
}
