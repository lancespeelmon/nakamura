package com.rsmart.oae.user.lostpwd;

import com.rsmart.oae.user.api.emailverify.util.ExceptionServletBase;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.servlets.ServletResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SlingServlet(methods = { "LostPasswordException" }, resourceTypes = { "sling/servlet/errorhandler" })
public class LostPasswordExceptionServlet extends ExceptionServletBase {


  @Reference
  protected ServletResolver resolver;
  
  /**
   * 
   */
  private static final long serialVersionUID = 3716780477529017681L;

  private static final Logger LOGGER = LoggerFactory.getLogger(LostPasswordExceptionServlet.class);
  
  protected Logger getLogger() {
    return LOGGER;
  }
  
  protected String getBasePath() {
    return "/lostpwd/sling/servlet/errorhandler/";
  }

  @Override
  protected ServletResolver getResolver() {
    return resolver;
  }
  
}
