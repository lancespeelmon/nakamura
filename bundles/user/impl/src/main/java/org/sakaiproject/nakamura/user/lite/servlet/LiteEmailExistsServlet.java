package org.sakaiproject.nakamura.user.lite.servlet;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.user.UserFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@ServiceDocumentation(name="User Exists Servlet", okForVersion = "0.11",
    description="Tests for existence of user's email. This servlet responds at /system/userManager/user.emailexists.html",
    shortDescription="Tests for existence of user's email",
    bindings=@ServiceBinding(type=BindingType.PATH,bindings="/system/userManager/user",
        selectors=@ServiceSelector(name="exists", description="Tests for existence of user's email."),
        extensions=@ServiceExtension(name="html", description="GETs produce HTML with request status.")),
    methods=@ServiceMethod(name="GET",
        description={"Checks for existence of user with email supplied in the email parameter."},
        parameters={
          @ServiceParameter(name="email", description="The email of the user to check for (required)")},
        response={
          @ServiceResponse(code=204,description="Success, user exists with email. No content returned."),
          @ServiceResponse(code=400,description="Bad request: the required email parameter was missing."),
          @ServiceResponse(code=404,description="The specified user with email does not exist in the system.")
        }))
@Component(immediate=true, metatype=true)
@SlingServlet(methods={"GET"}, selectors={"emailexists"}, resourceTypes={"sparse/users"}, generateComponent=false)
public class LiteEmailExistsServlet extends SlingSafeMethodsServlet {

  /**
   * 
   */
  private static final long serialVersionUID = -2596785815270828273L;

  private static final Logger LOGGER = LoggerFactory
    .getLogger(LiteUserExistsServlet.class);

  @Property(label="Delay (MS)",
    description="Number of milliseconds to delay before responding; 0 to return as quickly as possible",
    longValue=LiteEmailExistsServlet.USER_EMAIL_EXISTS_DELAY_MS_DEFAULT)
  public static final String USER_EMAIL_EXISTS_DELAY_MS_PROPERTY = "user.exists.delay.ms";
  public static final long USER_EMAIL_EXISTS_DELAY_MS_DEFAULT = 200;
  protected long delayMs;
  
  @Reference
  protected UserFinder userFinder;
  
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
    throws ServletException, IOException {
    long start = System.currentTimeMillis();
    try {
      RequestParameter idParam = request.getRequestParameter("email");
      if (idParam == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "This request must have a 'email' parameter.");
        return;
      }
    
      if ("".equals(idParam.getString())) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The 'email' parameter must not be blank.");
        return;
      }
      String email = idParam.getString();
      LOGGER.debug("Checking for existence of {}", email);
      // finding by email
      
      if (userFinder.userWithEmailExists(email)) {
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
      } else {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
      }
    } catch (Exception e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
      return;
    } finally {
      LOGGER.debug("checking for existence took {} ms", System.currentTimeMillis() - start);
      if (delayMs > 0) {
        long remainingTime = delayMs - (System.currentTimeMillis() - start);
        if (remainingTime > 0) {
          try {
            Thread.sleep(remainingTime);
          } catch (InterruptedException e) {
          }
        }
      }
    }
  }

  @Activate @Modified
  protected void modified(Map<?, ?> props) {
    delayMs = OsgiUtil.toLong(props.get(USER_EMAIL_EXISTS_DELAY_MS_PROPERTY),
        USER_EMAIL_EXISTS_DELAY_MS_DEFAULT);
  }
}
