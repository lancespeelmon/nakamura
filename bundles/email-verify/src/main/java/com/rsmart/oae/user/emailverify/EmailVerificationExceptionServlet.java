package com.rsmart.oae.user.emailverify;

import com.rsmart.oae.user.api.emailverify.util.ExceptionServletBase;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.servlets.ServletResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SlingServlet(methods = { "EmailVerificationException" }, resourceTypes = { "sling/servlet/errorhandler" })
public class EmailVerificationExceptionServlet extends ExceptionServletBase {

  
  @Reference
  protected transient ServletResolver resolver;
  
  private static final long serialVersionUID = 4160031321957514098L;

  private static final Logger LOGGER = LoggerFactory.getLogger(EmailVerifyServlet.class);
  

  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  protected ServletResolver getResolver() {
    return resolver;
  }
  
}
