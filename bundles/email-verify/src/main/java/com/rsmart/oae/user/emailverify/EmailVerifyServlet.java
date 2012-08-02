package com.rsmart.oae.user.emailverify;

import com.rsmart.oae.user.api.emailverify.EmailVerificationException;
import com.rsmart.oae.user.api.emailverify.EmailVerifyService;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.parameters.ParameterMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;



@ServiceDocumentation(
    bindings = {
        @ServiceBinding(
            type = BindingType.TYPE,
            bindings = {"rsmart/emailVerify"},
            selectors = { @ServiceSelector(name = "verify") }
        )
    },
    methods = {
        @ServiceMethod(
            name = "GET",
            description = {
                "This servlet will handle email verification.",
                "The guid passed in must match the one in the guid property of the rsmart/emailVerify node."
            },
            response = {
                @ServiceResponse(code = 200, description = "User's email was verified.  The system will record that the user has been verified."),
                @ServiceResponse(code = 500, description = "The passed in guid did not match the stored guid.")
            }
        ) 
    },
    name = "EmailVerifyServlet",
    description = "Handles the link that is email to the user to verify their email address.",
    shortDescription = "Handles the link that is email to the user to verify their email address.",
    okForVersion = "0.11"
)
@SlingServlet(methods = { "GET", "POST" }, resourceTypes = { "rsmart/emailVerify" })
public class EmailVerifyServlet extends SlingAllMethodsServlet {

  /**
   * 
   */
  private static final long serialVersionUID = -2952378073503392788L;

  private static final Logger LOGGER = LoggerFactory.getLogger(EmailVerifyServlet.class);
  
  @Reference
  protected transient EmailVerifyService emailVerifyService;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    processRequest(request, response);
  }
  
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    processRequest(request, response);
  }
  
  protected void processRequest(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    Resource resource = request.getResource();
    Content verification = resource.adaptTo(Content.class);
    String guid = request.getParameter(EmailVerifyService.EMAIL_VERIFY_GUID_PARAM);
    
    String selector = request.getRequestPathInfo().getSelectorString();
    String ext = request.getRequestPathInfo().getExtension();
    
    Session session = StorageClientUtils.adaptToSession(request.getResourceResolver()
        .adaptTo(javax.jcr.Session.class));

    if (guid != null && selector.equals("verify")) {
      emailVerifyService.clearVerification(session, guid, verification);  // let the exception throw... we'll handle it
      response.sendRedirect("/me#!emailVerified");
    }
    else if (selector.equals("resend")) {
      try {
        AuthorizableManager authorizableManager = session.getAuthorizableManager();
        User user = (User) authorizableManager.findAuthorizable(session.getUserId());
        
        emailVerifyService.sendVerifyEmail(session, user,
            ParameterMap.extractParameters(request));
        
        response.setStatus(200);
        
        sendBlankResponse(response, ext);
      } catch (StorageClientException e) {
        LOGGER.warn("failed to resend email verification", e);
        response.setStatus(500);
      } catch (AccessDeniedException e) {
        LOGGER.warn("failed to resend email verification", e);
        response.setStatus(500);
      } catch (JSONException e) {
        LOGGER.warn("failed to resend email verification", e);
        response.setStatus(500);
      }
    }
    else if (selector.equals("seenWarning")) {
      emailVerifyService.seenWarning(session, verification);
      response.setStatus(200);
      try {
        sendBlankResponse(response, ext);
      } catch (JSONException e) {
        LOGGER.warn("failed to resend email verification", e);
        response.setStatus(500);
      }
    }
    else if (selector.equals("cancelEmailChange")) {
      AuthorizableManager authorizableManager;
      try {
        authorizableManager = session.getAuthorizableManager();
        User user = (User) authorizableManager.findAuthorizable(session.getUserId());

        emailVerifyService.cancelChangeEmail(request, session, user, verification);
        
        response.setStatus(200);

        sendBlankResponse(response, ext);
      } catch (StorageClientException e) {
        LOGGER.warn("failed to change email", e);
        response.setStatus(500);
      } catch (AccessDeniedException e) {
        LOGGER.warn("failed to change email", e);
        response.setStatus(500);
      } catch (JSONException e) {
        LOGGER.warn("failed to change email", e);
        response.setStatus(500);
      }
    }
    else if (selector.equals("emailChange")) {
      AuthorizableManager authorizableManager;
      try {
        authorizableManager = session.getAuthorizableManager();
        User user = (User) authorizableManager.findAuthorizable(session.getUserId());

        emailVerifyService.changeEmail(session, user, verification,
            ParameterMap.extractParameters(request));
        
        response.setStatus(200);

        sendBlankResponse(response, ext);
      } catch (StorageClientException e) {
        LOGGER.warn("failed to change email", e);
        response.setStatus(500);
      } catch (AccessDeniedException e) {
        LOGGER.warn("failed to change email", e);
        response.setStatus(500);
      } catch (JSONException e) {
        LOGGER.warn("failed to change email", e);
        response.setStatus(500);
      }
    }
    else {
      throw new EmailVerificationException(); // invalid selector
    }
  }

  protected void sendBlankResponse(SlingHttpServletResponse response, String ext)
      throws IOException, JSONException {
    if (ext.equals("json")) {
      Map<String, Object> valueMap = new HashMap<String, Object>();
      valueMap.put("success", "true");
      ExtendedJSONWriter writer = new ExtendedJSONWriter(response.getWriter());
      writer.setTidy(false);
      writer.valueMap(valueMap);
    }
  }
  
  

}
