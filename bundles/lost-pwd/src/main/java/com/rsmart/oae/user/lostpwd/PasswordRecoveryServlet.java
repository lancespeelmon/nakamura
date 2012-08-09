/**
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.rsmart.oae.user.lostpwd;

import com.rsmart.oae.user.api.emailverify.EmailVerifyService;
import com.rsmart.oae.user.api.lostpwd.LostPasswordException;
import com.rsmart.oae.user.api.lostpwd.LostPasswordService;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.sakaiproject.nakamura.api.auth.trusted.TrustedTokenService;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;

import java.io.IOException;

import javax.servlet.ServletException;

@ServiceDocumentation(bindings = { @ServiceBinding(type = BindingType.TYPE, bindings = { "rsmart/lostPassword" }, selectors = { @ServiceSelector(name = "recover") }) }, methods = { @ServiceMethod(name = "GET", description = {
    "This servlet will handle recovering password.",
    "The guid passed in must match the one in the guid property of the rsmart/lostPassword node." }, response = {
    @ServiceResponse(code = 200, description = "User's email was verified.  The system will record that the user has been verified."),
    @ServiceResponse(code = 500, description = "The passed in guid did not match the stored guid.") }) }, name = "EmailVerifyServlet", description = "Handles the link that is email to the user to verify their email address.", shortDescription = "Handles the link that is email to the user to verify their email address.", okForVersion = "0.11")
@SlingServlet(methods = { "GET", "POST" }, resourceTypes = { "rsmart/lostPassword" })
public class PasswordRecoveryServlet extends SlingAllMethodsServlet {

  private static final long serialVersionUID = -5786481053604095983L;

  @Reference
  protected transient LostPasswordService lostPasswordService;

  @Reference
  protected transient TrustedTokenService trustedTokenService;

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

  protected void processRequest(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {

    Resource resource = request.getResource();
    Content passwordRecovery = resource.adaptTo(Content.class);
    String guid = request.getParameter(EmailVerifyService.EMAIL_VERIFY_GUID_PARAM);

    String selector = request.getRequestPathInfo().getSelectorString();

    if ("recover".equals(selector)) {
      try {
        trustedTokenService.dropCredentials(request, response);

        String redirect = lostPasswordService.checkPasswordRetrieval(request, guid,
            passwordRecovery);

        response.sendRedirect(redirect);
      } catch (LostPasswordException e) {
        if (e.getRedirectLink() != null) {
          response.sendRedirect(e.getRedirectLink());
        } else {
          throw e;
        }
      }
    } else if ("changePass".equals(selector)) {
      Session session = StorageClientUtils.adaptToSession(request.getResourceResolver()
          .adaptTo(javax.jcr.Session.class));

      String newPassword = request.getParameter("password");

      if (newPassword == null) {
        throw new ServletException("password is required");
      }
      User user = lostPasswordService.changePassword(session, guid, passwordRecovery,
          newPassword);

      // user is authn...
      trustedTokenService.switchToUser(user.getId(), request, response);

      response.setStatus(200);
    }

  }

  /**
   * notes:
   * 
   * checkPwd: first, call checkPasswordRetrieval to see if the guid matches and it hasn't
   * already been called if no exception, redirect the user to the home screen that will
   * then pop-up a dialog to change the password (with the guid and recoveryFile path as
   * hidden params)
   * 
   * changePwd: call changePassword... return json success or fail
   * 
   */

}
