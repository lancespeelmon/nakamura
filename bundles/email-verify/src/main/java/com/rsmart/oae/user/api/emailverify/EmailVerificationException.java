package com.rsmart.oae.user.api.emailverify;


public class EmailVerificationException extends RuntimeException {

  public EmailVerificationException(Throwable e) {
    super(e);
  }
  
  public EmailVerificationException() {
    super();
  }

  public EmailVerificationException(String arg0, Throwable arg1) {
    super(arg0, arg1);
  }

  public EmailVerificationException(String arg0) {
    super(arg0);
  }

  /**
   * 
   */
  private static final long serialVersionUID = -6810945661867564310L;

}
