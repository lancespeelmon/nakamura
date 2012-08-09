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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;

import com.rsmart.oae.user.api.emailverify.EmailVerifyService;
import com.rsmart.oae.user.api.lostpwd.InvalidLinkLostPasswordException;
import com.rsmart.oae.user.api.lostpwd.LostPasswordService;
import com.rsmart.oae.user.api.lostpwd.NotVerifiedLostPasswordException;
import com.rsmart.oae.user.api.lostpwd.UserNotFoundLostPasswordException;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.service.event.Event;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.message.LiteMessageRouterManager;
import org.sakaiproject.nakamura.api.message.LiteMessageTransport;
import org.sakaiproject.nakamura.api.message.LiteMessagingService;
import org.sakaiproject.nakamura.api.message.MessageRoutes;
import org.sakaiproject.nakamura.api.message.MessagingException;
import org.sakaiproject.nakamura.api.user.UserFinder;
import org.sakaiproject.nakamura.lite.BaseMemoryRepository;
import org.sakaiproject.nakamura.util.LitePersonalUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;

public class LostPasswordServiceTest {
  private static String uid = "jde";
  private static String oldPwd = "blah";
  private static String newPwd = "blahdblah";
  private static String origEmail = "jde@dovevalleyapps.com";

  @Mock
  private EmailVerifyService emailVerifyService;

  private Repository repository = null;

  private Session session = null;

  @Mock
  private SlingHttpServletRequest request = null;

  @Mock
  private SlingRepository slingRepository;
  @Mock
  private JackrabbitSession adminSession;
  @Mock
  private JackrabbitSession jcrSesson;
  @Mock
  private ResourceResolver resourceResolver;
  @Mock
  private UserFinder userFinder;
  @Mock
  private LiteMessageRouterManager messageRouterManager;
  @Mock
  private LiteMessagingService messagingService;
  @Mock
  private Node bundlesNode;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Node defaultPropsNode;
  @Mock
  private RequestParameterMap requestParameterMap;

  private Content lastMessage;

  private LostPasswordService lostPasswordService;

  private LiteMessageTransport mockTransport;

  @SuppressWarnings("unused")
  private User user;

  public LostPasswordServiceTest() {
    System.out.println("constructing test");

    try {
      MockitoAnnotations.initMocks(this);

      messagingService = new LiteMessagingService() {

        public String getFullPathToStore(String rcpt, Session session)
            throws MessagingException {
          return null;
        }

        public String getFullPathToMessage(String rcpt, String messageId, Session session)
            throws MessagingException {
          return null;
        }

        public List<String> expandAliases(String localRecipient) {
          return null;
        }

        public Content create(Session session, Map<String, Object> mapProperties,
            String messageId, String messagePathBase) throws MessagingException {
          return null;
        }

        public Content create(Session session, Map<String, Object> mapProperties)
            throws MessagingException {
          return new Content("message", mapProperties);
        }

        public Content create(Session session, Map<String, Object> mapProperties,
            String messageId) throws MessagingException {
          return null;
        }

        public void copyMessageNode(Content sourceMessage, String targetMessageStore,
            Session session) throws StorageClientException, AccessDeniedException,
            IOException {
        }

        public boolean checkDeliveryAccessOk(String recipient, Content originalMessage,
            Session session) {
          return false;
        }
      };

      repository = new BaseMemoryRepository().getRepository();

      mockTransport = new LiteMessageTransport() {
        public void send(MessageRoutes routes, Event event, Content message) {
          lastMessage = message;
        }
      };

      init();
    } catch (ClientPoolException e) {
      throw new RuntimeException(e);
    } catch (StorageClientException e) {
      throw new RuntimeException(e);
    } catch (AccessDeniedException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  protected void init() {
    lostPasswordService = new LostPasswordServiceImpl();

    ((LostPasswordServiceImpl) lostPasswordService).emailVerifyService = emailVerifyService;
    ((LostPasswordServiceImpl) lostPasswordService).repository = repository;
    ((LostPasswordServiceImpl) lostPasswordService).slingRepository = slingRepository;
    ((LostPasswordServiceImpl) lostPasswordService).userFinder = userFinder;
    ((LostPasswordServiceImpl) lostPasswordService).messageRouterManager = messageRouterManager;
    ((LostPasswordServiceImpl) lostPasswordService).messagingService = messagingService;
    ((LostPasswordServiceImpl) lostPasswordService).addTransport(mockTransport);

  }

  protected String getBundle() {
    return "/devwidgets/lostpwd/bundles";
  }

  @Before
  public void setUp() throws Exception {
    when(slingRepository.loginAdministrative(null)).thenReturn(adminSession);

    when(request.getResourceResolver()).thenReturn(resourceResolver);
    when(resourceResolver.adaptTo(javax.jcr.Session.class)).thenReturn(jcrSesson);

    when(jcrSesson.getNode(getBundle())).thenReturn(bundlesNode);
    when(adminSession.getNode(getBundle())).thenReturn(bundlesNode);
    when(bundlesNode.getNode("default.properties")).thenReturn(defaultPropsNode);
    when(bundlesNode.getNode("en.properties")).thenThrow(new PathNotFoundException());

    Property binary1Property = Mockito.mock(Property.class);
    when(defaultPropsNode.getNode("jcr:content").getProperty("jcr:data")).thenReturn(
        binary1Property);
    Binary binary1 = Mockito.mock(Binary.class);
    when(binary1Property.getBinary()).thenReturn(binary1);
    when(binary1.getStream()).thenReturn(
        new ByteArrayInputStream("REPLACE_ME=Yay, In the language bundle!"
            .getBytes("UTF-8")));

    when(request.getScheme()).thenReturn("http");
    when(request.getServerName()).thenReturn("localhost");
    when(request.getServerPort()).thenReturn(8080);
    when(request.getContextPath()).thenReturn("/");
    when(request.getLocale()).thenReturn(new Locale("en"));

    when(userFinder.findUsersByEmail(origEmail)).thenReturn(ImmutableSet.of(uid));

    when(request.getRequestParameterMap()).thenReturn(requestParameterMap);
    Map<String, RequestParameter[]> requestParameters = new HashMap<String, RequestParameter[]>();
    when(requestParameterMap.keySet()).thenReturn(requestParameters.keySet());
  }

  protected void createUser() throws Exception {
    session = repository.loginAdministrative();

    AuthorizableManager authzManager = session.getAuthorizableManager();

    Map<String, Object> userProps = new HashMap<String, Object>();
    userProps.put("email", origEmail);

    authzManager.createUser(uid, "john", oldPwd, userProps);

    user = (User) authzManager.findAuthorizable(uid);

    ContentManager contentManager = session.getContentManager();

    String homePath = LitePersonalUtils.getHomePath(uid);
    Builder<String, Object> props = ImmutableMap.builder();
    props.put("sling:resourceType", "sakai/user-home");
    contentManager.update(new Content(homePath, props.build()));

    List<AclModification> aclModifications = new ArrayList<AclModification>();
    AclModification.addAcl(true, Permissions.ALL, uid, aclModifications);

    AccessControlManager accessControlManager = session.getAccessControlManager();

    AclModification[] aclMods = aclModifications
        .toArray(new AclModification[aclModifications.size()]);
    accessControlManager.setAcl(Security.ZONE_CONTENT, homePath, aclMods);

    accessControlManager.setAcl(Security.ZONE_AUTHORIZABLES, uid, aclMods);

    session = repository.loginAdministrative(uid);

    refresh();
  }

  protected void refresh() throws Exception {
    session = repository.loginAdministrative(uid);

    user = (User) session.getAuthorizableManager().findAuthorizable(uid);
  }

  protected void checkEmail(Map<String, Object> targets) {
    for (String key : targets.keySet()) {
      Object targetValue = targets.get(key);

      System.out.println("comparing key: " + key);
      System.out.println(lastMessage.getProperty(key));
      System.out.println("to:");
      System.out.println(targetValue);

      assertEquals("email should have value: " + targetValue + " for key " + key,
          targetValue, lastMessage.getProperty(key));
    }
    lastMessage = null;
  }

  @Test
  public void testLostPassword() throws Exception {
    createUser();

    // test user not verified
    when(emailVerifyService.hasVerified(any(Session.class), any(User.class))).thenReturn(
        false);

    try {
      lostPasswordService.recoverPassword(request, uid); // should not send
      fail();
    } catch (NotVerifiedLostPasswordException e) {
      // this should happen
    }

    // test user not found
    try {
      lostPasswordService.recoverPassword(request, "blah");
      fail();
    } catch (UserNotFoundLostPasswordException e) {
      // should happen
    }

    System.out.println("test a verified user");

    // test user verfied
    when(emailVerifyService.hasVerified(any(Session.class), any(User.class))).thenReturn(
        true);
    lostPasswordService.recoverPassword(request, uid); // should send

    // check the email
    Content recovery = session.getContentManager().get(
        ((LostPasswordServiceImpl) lostPasswordService).getPath(uid));

    String guid = (String) recovery
        .getProperty(LostPasswordServiceImpl.PASSWORD_RECOVERY_UUID_PROPERTY);

    checkEmail(ImmutableMap
        .of("sakai:to",
            (Object) "smtp:" + origEmail,
            "sakai:templatePath",
            (Object) "/var/templates/email/send_lost_password",
            "sakai:templateParams",
            (Object) "sender="
                + uid
                + "|username="
                + uid
                + "|link=http://localhost:8080/system/lostpassword/jde/passwordRecovery.recover.html?guid="
                + guid));

    try {
      lostPasswordService.checkPasswordRetrieval(request, guid + "bad", recovery);
      fail(); // testing bad guid
    } catch (InvalidLinkLostPasswordException e) {
      // this should happen
    }

    lostPasswordService.checkPasswordRetrieval(request, guid, recovery); // should succeed

    recovery = session.getContentManager().get(
        ((LostPasswordServiceImpl) lostPasswordService).getPath(uid));

    try {
      lostPasswordService.checkPasswordRetrieval(request, guid, recovery);
      fail(); // should not allow a second call here
    } catch (InvalidLinkLostPasswordException e) {
      // this should happen
    }

    try {
      lostPasswordService.changePassword(session, guid + "bad", recovery, newPwd);
      fail(); // testing bad guid
    } catch (InvalidLinkLostPasswordException e) {
      // this should happen
    }

    // make sure we can still login with old pwd
    repository.login(uid, oldPwd);

    lostPasswordService.changePassword(session, guid, recovery, newPwd);

    repository.login(uid, newPwd); // should have changed the pwd

    recovery = session.getContentManager().get(
        ((LostPasswordServiceImpl) lostPasswordService).getPath(uid));

    assertEquals(null, recovery);
  }

}
