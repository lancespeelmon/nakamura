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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import com.rsmart.oae.user.api.emailverify.EmailVerifyService;
import com.rsmart.oae.user.api.emailverify.util.EmailServiceBase;
import com.rsmart.oae.user.api.lostpwd.InvalidLinkLostPasswordException;
import com.rsmart.oae.user.api.lostpwd.LostPasswordException;
import com.rsmart.oae.user.api.lostpwd.LostPasswordService;
import com.rsmart.oae.user.api.lostpwd.NotVerifiedLostPasswordException;
import com.rsmart.oae.user.api.lostpwd.UserNotFoundLostPasswordException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.jcr.api.SlingRepository;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.message.LiteMessageRouterManager;
import org.sakaiproject.nakamura.api.message.LiteMessageTransport;
import org.sakaiproject.nakamura.api.message.LiteMessagingService;
import org.sakaiproject.nakamura.api.user.UserFinder;
import org.sakaiproject.nakamura.util.PathUtils;
import org.sakaiproject.nakamura.util.parameters.ParameterMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

@Service
@Component(immediate = true, metatype = true)
@Reference(name = "MessageTransport", referenceInterface = LiteMessageTransport.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, bind = "addTransport", unbind = "removeTransport")
public class LostPasswordServiceImpl extends EmailServiceBase implements
    LostPasswordService {

  protected static final String PASSWORD_RECOVERY_RESOURCE_PATH = "/passwordRecovery";

  protected static final String PASSWORD_RECOVERY_UUID_PROPERTY = "rsmart:pwdRecoveryUUID";

  protected static final String PASSWORD_RECOVERY_RECOVERY_CHECKED = "rsmart:pwdRecoveryRecoveryChecked";

  protected static final String PASSWORD_RECOVERY_LINK_STATUS = "rsmart:linkStatus";

  protected static final String SLING_RESOURCE_TYPE = "sling:resourceType";

  protected static final String PASSWORD_RECOVERY_USER = "rsmart:pwdRecoveryUser";

  public static final String PARAM_LANGUAGE = "l";

  @Reference
  protected transient Repository repository;

  @Reference
  protected transient UserFinder userFinder;

  @Reference
  protected transient EmailVerifyService emailVerifyService;

  @Reference
  protected transient LiteMessageRouterManager messageRouterManager;

  @Reference
  protected transient LiteMessagingService messagingService;

  @Reference
  protected transient SlingRepository slingRepository;

  private static final Logger LOG = LoggerFactory
      .getLogger(LostPasswordServiceImpl.class);

  /**
   * This will contain all the transports.
   */
  protected Map<LiteMessageTransport, LiteMessageTransport> transports = new ConcurrentHashMap<LiteMessageTransport, LiteMessageTransport>();

  public void recoverPassword(SlingHttpServletRequest request, String username) {
    // look for username as user, and as email...
    Session adminSession = null;
    try {
      adminSession = repository.loginAdministrative(User.ADMIN_USER);

      AuthorizableManager authorizableManager = adminSession.getAuthorizableManager();
      User user = (User) authorizableManager.findAuthorizable(username);

      if (user == null) {
        // try finding by email??
        Set<String> users = userFinder.findUsersByEmail(username);

        if (!users.isEmpty()) {
          user = (User) authorizableManager.findAuthorizable(users.iterator().next());
        }
      }

      if (user == null) {
        // not found
        throw new UserNotFoundLostPasswordException();
      }

      if (!emailVerifyService.hasVerified(adminSession, user)) {
        // not verified...
        throw new NotVerifiedLostPasswordException();
      }

      // create passwordRecovery file and send link to email
      Content recovery = createRecoveryFile(adminSession, user);

      sendRecoveryEmail(request, adminSession, user, recovery);
    } catch (StorageClientException e) {
      throw new LostPasswordException(e);
    } catch (AccessDeniedException e) {
      throw new LostPasswordException(e);
    } catch (LostPasswordException e) {
      throw e;
    } catch (Exception e) {
      throw new LostPasswordException(e);
    } finally {
      try {
        adminSession.logout();
      } catch (Exception e) {
        LOG.warn("Failed to logout of administrative session {} ", e.getMessage());
      }
    }
  }

  protected void sendRecoveryEmail(SlingHttpServletRequest request, Session session,
      User user, Content recovery) throws MalformedURLException, PathNotFoundException,
      RepositoryException, IOException {
    URL url = getVerifyExternalURL(request, recovery, "recover");

    String email = user.getProperty("email").toString();

    Map<String, String> templateParams = new HashMap<String, String>();
    templateParams.put("link", url.toExternalForm());
    templateParams.put("username", user.getId());
    sendEmail(
        session,
        user,
        email,
        "/var/templates/email/send_lost_password",
        templateParams,
        getLocaleString(ParameterMap.extractParameters(request),
            "LOST_PASSWORD_EMAIL_SUBJECT", slingRepository));
    LOG.debug("message sent");
  }

  protected URL getVerifyExternalURL(SlingHttpServletRequest request, Content recovery,
      String selector) throws MalformedURLException {
    String recoveryPath = PathUtils.translateAuthorizablePath(recovery.getPath())
        .toString();
    String baseUrl = getBaseUrl(request);

    String uuid = (String) recovery.getProperties().get(PASSWORD_RECOVERY_UUID_PROPERTY);

    URL url = new URL(new URL(baseUrl), recoveryPath + "." + selector + ".html?"
        + LOST_PASSWORD_GUID_PARAM + "=" + uuid);
    return url;
  }

  protected String getPath(String authId) {
    return "/system/lostpassword/" + authId + PASSWORD_RECOVERY_RESOURCE_PATH;
  }

  public Content createRecoveryFile(Session session, User user)
      throws StorageClientException, AccessDeniedException {
    ContentManager contentManager = session.getContentManager();

    String path = getPath(user.getId());

    if (contentManager.exists(path)) {
      contentManager.delete(path);
    }

    String resourceType = LostPasswordService.LOST_PASSWORD_RT;
    Map<String, Object> additionalProperties = new HashMap<String, Object>();

    additionalProperties.put(PASSWORD_RECOVERY_UUID_PROPERTY, createUUID());
    additionalProperties.put(PASSWORD_RECOVERY_USER, user.getId());

    Builder<String, Object> propertyBuilder = ImmutableMap.builder();
    propertyBuilder.put(SLING_RESOURCE_TYPE, resourceType);
    if (additionalProperties != null) {
      propertyBuilder.putAll(additionalProperties);
    }

    Content recovery = new Content(path, propertyBuilder.build());
    contentManager.update(recovery);

    return contentManager.get(path);
  }

  protected String createUUID() {
    return UUID.randomUUID().toString();
  }

  protected void checkPasswordRetrievalInternal(String guid, Content passwordRecovery,
      boolean checkRecoverLink) {
    if (!guid.equals(passwordRecovery.getProperty(PASSWORD_RECOVERY_UUID_PROPERTY))) {
      throw new InvalidLinkLostPasswordException("guid did not match",
          createFailRedirect("INVALID_URL"));
    }

    if (checkRecoverLink
        && passwordRecovery.hasProperty(PASSWORD_RECOVERY_RECOVERY_CHECKED)) {
      throw new InvalidLinkLostPasswordException("link already used",
          createFailRedirect("INVALID_URL"));
    }
  }

  protected String createFailRedirect(String failType) {
    return "/#!lostpwdfail:" + failType;
  }

  public String checkPasswordRetrieval(SlingHttpServletRequest request, String guid,
      Content passwordRecovery) throws LostPasswordException {
    checkPasswordRetrievalInternal(guid, passwordRecovery, true);

    Session adminSession = null;
    try {
      adminSession = repository.loginAdministrative(User.ADMIN_USER);

      ContentManager contentManager = adminSession.getContentManager();

      passwordRecovery.setProperty(PASSWORD_RECOVERY_RECOVERY_CHECKED, "true");

      contentManager.update(passwordRecovery);

      return "/#!lostpwdurl:"
          + URLEncoder.encode(
              getVerifyExternalURL(request, passwordRecovery, "changePass")
                  .toExternalForm(), "UTF-8");
    } catch (MalformedURLException e) {
      throw new LostPasswordException(e, createFailRedirect("UNKNOWN"));
    } catch (UnsupportedEncodingException e) {
      throw new LostPasswordException(e, createFailRedirect("UNKNOWN"));
    } catch (ClientPoolException e) {
      throw new LostPasswordException(e, createFailRedirect("UNKNOWN"));
    } catch (StorageClientException e) {
      throw new LostPasswordException(e, createFailRedirect("UNKNOWN"));
    } catch (AccessDeniedException e) {
      throw new LostPasswordException(e, createFailRedirect("UNKNOWN"));
    } finally {
      try {
        adminSession.logout();
      } catch (Exception e) {
        LOG.warn("Failed to logout of administrative session {} ", e.getMessage());
      }
    }
  }

  public User changePassword(Session session, String guid, Content passwordRecovery,
      String newPassword) throws LostPasswordException {
    Session adminSession = null;
    try {
      adminSession = repository.loginAdministrative(User.ADMIN_USER);

      ContentManager contentManager = adminSession.getContentManager();

      checkPasswordRetrievalInternal(guid, passwordRecovery, false);

      AuthorizableManager authorizableManager = adminSession.getAuthorizableManager();
      User user = (User) authorizableManager.findAuthorizable((String) passwordRecovery
          .getProperty(PASSWORD_RECOVERY_USER));

      authorizableManager.changePassword(user, newPassword, null);

      contentManager.delete(passwordRecovery.getPath());

      return user;
    } catch (ClientPoolException e) {
      throw new LostPasswordException(e);
    } catch (StorageClientException e) {
      throw new LostPasswordException(e);
    } catch (AccessDeniedException e) {
      throw new LostPasswordException(e);
    } finally {
      try {
        adminSession.logout();
      } catch (Exception e) {
        LOG.warn("Failed to logout of administrative session {} ", e.getMessage());
      }
    }
  }

  @Override
  protected Logger getLogger() {
    return LOG;
  }

  @Override
  protected LiteMessageRouterManager getLiteMessageRouterManager() {
    return messageRouterManager;
  }

  @Override
  protected LiteMessagingService getLiteMessagingService() {
    return messagingService;
  }

  /**
   * @param transport
   */
  public void removeTransport(LiteMessageTransport transport) {
    transports.remove(transport);
  }

  /**
   * @param transport
   */
  public void addTransport(LiteMessageTransport transport) {
    transports.put(transport, transport);
  }

  @Override
  protected Map<LiteMessageTransport, LiteMessageTransport> getTransports() {
    return transports;
  }

  @Override
  protected String getBundleName() {
    return "/devwidgets/lostpwd/bundles";
  }

}
