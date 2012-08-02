package com.rsmart.oae.user.api.lostpwd;


public class LostPasswordException extends RuntimeException {

  /**
   * 
   */
  private static final long serialVersionUID = 6804827709793216858L;

  private String redirectLink;
  
  public LostPasswordException() {
    super();
  } 

  public LostPasswordException(String message, Throwable cause) {
    super(message, cause);
  }

  public LostPasswordException(String message) {
    super(message);
  }

  public LostPasswordException(Throwable cause) {
    super(cause);
  }

  public LostPasswordException(String message, Throwable cause, String redirectLink) {
    super(message, cause);
    this.redirectLink = redirectLink;
  }

  public LostPasswordException(Throwable cause, String redirectLink) {
    super(cause);
    this.redirectLink = redirectLink;
  }

  public LostPasswordException(String arg0, String redirectLink) {
    super(arg0);
    this.redirectLink = redirectLink;
  }

  public String getRedirectLink() {
    return redirectLink;
  }

  public void setRedirectLink(String redirectLink) {
    this.redirectLink = redirectLink;
  }

  
  
  
  
}
