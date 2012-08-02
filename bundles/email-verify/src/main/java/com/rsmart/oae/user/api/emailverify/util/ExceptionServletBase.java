package com.rsmart.oae.user.api.emailverify.util;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class ExceptionServletBase extends SlingSafeMethodsServlet {

  /**
   * 
   */
  private static final long serialVersionUID = 3022463987691707397L;

  private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionServletBase.class);
  
  @Override
  protected boolean mayService(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException,
      IOException {
    
    Throwable throwable = (Throwable) request.getAttribute(SlingConstants.ERROR_EXCEPTION);
    Resource resource = request.getResource();
    
    Servlet servlet = null;

    Class<?> tClass = throwable.getClass();
    while (servlet == null && tClass != Object.class) {
        servlet = getResolver().resolveServlet(resource, getBasePath() + tClass.getSimpleName() + ".jsp");

        // go to the base class
        tClass = tClass.getSuperclass();
    }

    if (servlet == null) {
      throw new RuntimeException("Could not find error handler for " + throwable.getClass().getSimpleName());
    }
    
    handleError(servlet, request, response);
    
    return true;
  }

  protected String getBasePath() {
    return null;
  }

  private void handleError(Servlet errorHandler, HttpServletRequest request, HttpServletResponse response)
    throws ServletException {

    request.setAttribute(SlingConstants.ERROR_REQUEST_URI, request.getRequestURI());
    
    // if there is no explicitly known error causing servlet, use
    // the name of the error handler servlet
    if (request.getAttribute(SlingConstants.ERROR_SERVLET_NAME) == null) {
      request.setAttribute(SlingConstants.ERROR_SERVLET_NAME, errorHandler.getServletConfig().getServletName());
    }
    
    try {
      errorHandler.service(request, response);
    } catch (IOException ioe) {
      // forware the IOException
      throw new ServletException(ioe);
    } catch (Throwable t) {
      getLogger().error("Calling the error handler resulted in an error", t);
      getLogger().error("Original error " + request.getAttribute(SlingConstants.ERROR_EXCEPTION_TYPE),
              (Throwable) request.getAttribute(SlingConstants.ERROR_EXCEPTION));
    }
  }

  protected Logger getLogger() {
    return LOGGER;
  }
  
  protected abstract ServletResolver getResolver();


}
