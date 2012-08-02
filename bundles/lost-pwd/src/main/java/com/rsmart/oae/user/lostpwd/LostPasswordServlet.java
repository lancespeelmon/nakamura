package com.rsmart.oae.user.lostpwd;

import com.rsmart.oae.user.api.lostpwd.LostPasswordService;
import com.rsmart.oae.user.api.lostpwd.NotVerifiedLostPasswordException;
import com.rsmart.oae.user.api.lostpwd.UserNotFoundLostPasswordException;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;

import java.io.IOException;

import javax.servlet.ServletException;

@SlingServlet(methods = "GET", paths = {"/system/lostpasswordfind"})
@ServiceDocumentation(name = "Servlet lost password",
    okForVersion = "0.11",
    description = "used to start the lost password process... call it like: lostpassword?username=blah",
    bindings = @ServiceBinding(type = BindingType.PATH, bindings = "/lostpasswordfind"), 
    url = "/system/lostpassword",
    methods = { 
         @ServiceMethod(name = "GET", 
             description = "GETs to this servlet will send an email to the user with a link to recover their password.", 
             parameters = @ServiceParameter(name = "username", 
                 description = "the username or email to find the password for"),
             response= {
               @ServiceResponse(code=200,description="blank json document"),
               @ServiceResponse(code=403,description="Email not yet validated"),
               @ServiceResponse(code=404,description="Username not found")
             })
    })
public class LostPasswordServlet extends SlingSafeMethodsServlet {

  /**
   * 
   */
  private static final long serialVersionUID = -5959553173225667703L;
  
  
  @Reference
  protected transient LostPasswordService lostPasswordService;
  
  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    String username = request.getParameter("username");
    
    if (username == null) {
      throw new RuntimeException("username is required");
    }
    
    try {
      lostPasswordService.recoverPassword(request, username);
      response.setStatus(200); // everything ok
    }
    catch (UserNotFoundLostPasswordException e) {
      response.setStatus(404);
    }
    catch (NotVerifiedLostPasswordException e) {
      response.setStatus(403);
    }
    catch (RuntimeException e) {
      throw e;
    }
  }
}
