package com.rsmart.oae.user.emailverify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

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

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.scheduler.Scheduler;
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
import org.sakaiproject.nakamura.api.user.LiteAuthorizablePostProcessor;
import org.sakaiproject.nakamura.api.user.UserFinder;
import org.sakaiproject.nakamura.lite.BaseMemoryRepository;
import org.sakaiproject.nakamura.util.LitePersonalUtils;
import org.sakaiproject.nakamura.util.parameters.ParameterMap;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.rsmart.oae.user.api.emailverify.EmailVerifyService;
import com.rsmart.oae.user.api.emailverify.InvalidLinkEmailVerificationException;

public class EmailVerifyServiceTest {
  
  private static String uid = "jde";
  private static String origEmail = "jde@dovevalleyapps.com";
  private static String newEmail = "jde-new@dovevalleyapps.com";
  
  private EmailVerifyService emailVerifyService;
  
  private Repository repository = null;
  
  private Session session = null;
  
  @Mock
  private SlingHttpServletRequest request = null;
  @Mock
  private RequestParameterMap requestParameterMap;
  @Mock
  RequestParameter newEmailRequestParameter;

  @Mock
  private SlingRepository slingRepository;
  @Mock
  private JackrabbitSession adminSession;
  @Mock
  private JackrabbitSession jcrSession;
  @Mock
  private ResourceResolver resourceResolver;
  @Mock
  private Scheduler scheduler;
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
  
  private Content lastMessage;
  

  private LiteMessageTransport mockTransport;

  
  private User user;
  
  private Content verify;
  
  private String guid;

  public EmailVerifyServiceTest() {
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
            Session session) throws StorageClientException, AccessDeniedException, IOException {
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
    emailVerifyService = new EmailVerifyServiceImpl();
    
    ((EmailVerifyServiceImpl)emailVerifyService).repository = repository;
    ((EmailVerifyServiceImpl)emailVerifyService).slingRepository = slingRepository;
    ((EmailVerifyServiceImpl)emailVerifyService).scheduler = scheduler;
    ((EmailVerifyServiceImpl)emailVerifyService).userFinder = userFinder;
    ((EmailVerifyServiceImpl)emailVerifyService).messageRouterManager = messageRouterManager;
    ((EmailVerifyServiceImpl)emailVerifyService).messagingService = messagingService;
          
    ((EmailVerifyServiceImpl)emailVerifyService).addTransport(mockTransport);
  }

  protected String getBundle() {
    return "/devwidgets/emailverify/bundles";
  }
  
  @Before
  public void setUp() throws Exception {
    when(slingRepository.loginAdministrative(null)).thenReturn(adminSession);
    
    when(request.getResourceResolver()).thenReturn(resourceResolver);
    when(resourceResolver.adaptTo(javax.jcr.Session.class)).thenReturn(jcrSession);

    when(jcrSession.getNode(getBundle())).thenReturn(bundlesNode);
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
    when(request.getParameter("email")).thenReturn(newEmail);
    when(request.getRequestParameterMap()).thenReturn(requestParameterMap);
    when(requestParameterMap.getValues("email")).thenReturn(
        new RequestParameter[] { newEmailRequestParameter });
    Map<String, RequestParameter[]> requestParameters = new HashMap<String, RequestParameter[]>();
    when(newEmailRequestParameter.getString()).thenReturn(newEmail);
    requestParameters.put("email", new RequestParameter[] {newEmailRequestParameter});
    when(requestParameterMap.keySet()).thenReturn(requestParameters.keySet());
  }
  
  
  /**
   * tests:
   * 
   * 1. test post process on user and group (group should do nothing), user should send email
   *    test "hasVerified" 
   * 
   * 
   * 
   */

  
  protected void createUser() throws Exception {
    session = repository.loginAdministrative();
    
    AuthorizableManager authzManager = session.getAuthorizableManager();
    
    Map<String, Object> userProps = new HashMap<String, Object>();
    userProps.put("email", origEmail);
    
    authzManager.createUser(uid, "john", "pass", userProps);
    
    user = (User) authzManager.findAuthorizable(uid);
    
    ContentManager contentManager = session.getContentManager();
    
    String homePath = LitePersonalUtils.getHomePath(uid);
    Builder<String, Object> props = ImmutableMap.builder();
    props.put("sling:resourceType", "sakai/user-home");
    contentManager.update(new Content(homePath, props.build()));

    List<AclModification> aclModifications = new ArrayList<AclModification>();
    AclModification.addAcl(true, Permissions.ALL, uid,
        aclModifications);    
    
    AccessControlManager accessControlManager = session.getAccessControlManager();
    
    AclModification[] aclMods = aclModifications
      .toArray(new AclModification[aclModifications.size()]);
    accessControlManager.setAcl(Security.ZONE_CONTENT, homePath, aclMods);

    accessControlManager.setAcl(Security.ZONE_AUTHORIZABLES, uid,
        aclMods);

    session = repository.loginAdministrative(uid);
    
    ((LiteAuthorizablePostProcessor) emailVerifyService).process(user, session, null,
        ParameterMap.extractParameters(request));
    
    refresh();
  }

  protected void refresh() throws Exception {
    session = repository.loginAdministrative(uid);
    
    user = (User) session.getAuthorizableManager().findAuthorizable(uid);
    
    verify = session.getContentManager().get(((EmailVerifyServiceImpl)emailVerifyService).getPath(uid));
    
    guid = (String) verify.getProperty(EmailVerifyServiceImpl.EMAIL_VERIFY_UUID_PROPERTY);
  }
  
  protected void checkEmail(Map<String, Object> targets) {
    for (String key : targets.keySet()) {
      Object targetValue = targets.get(key);
      
      System.out.println("comparing key: " + key);
      System.out.println(lastMessage.getProperty(key));
      System.out.println("to:");
      System.out.println(targetValue);
      
      assertEquals("email should have value: " + targetValue + " for key " + key, targetValue, lastMessage.getProperty(key));
    }
    lastMessage = null;
  }

  @Test
  public void testDoPostProcessCreateUser()
      throws Exception {
    createUser();
    
    checkEmail(ImmutableMap.of("sakai:to", (Object)"smtp:"+origEmail, 
        "sakai:templatePath", (Object)"/var/templates/email/verify_email", 
        "sakai:templateParams", 
        (Object)"sender=" + uid + "|link=http://localhost:8080/~" + uid + "/emailVerify.verify.html?guid=" + guid));
    
    assertFalse(emailVerifyService.hasVerified(session, user));
    
    try {
      emailVerifyService.clearVerification(session, guid + "bad", verify);
      fail(); // should throw... shouldn't get here...
    }
    catch (InvalidLinkEmailVerificationException e) {
      // this should happen... ignore
    }
    
    emailVerifyService.seenWarning(session, verify);
    
    // resend email
    lastMessage = null;
    emailVerifyService.sendVerifyEmail(session, user,
        ParameterMap.extractParameters(request));
    checkEmail(ImmutableMap.of("sakai:to", (Object)"smtp:"+origEmail, 
        "sakai:templatePath", (Object)"/var/templates/email/verify_email", 
        "sakai:templateParams", 
        (Object)"sender=" + uid + "|link=http://localhost:8080/~" + uid + "/emailVerify.verify.html?guid=" + guid));
    
    emailVerifyService.clearVerification(session, guid, verify);
    
    refresh();
    
    assertTrue(emailVerifyService.hasVerified(session, user));
  }
  
  @Test
  public void testChangeEmail() throws Exception {
    createUser();
    
    emailVerifyService.clearVerification(session, guid, verify);
    assertTrue(emailVerifyService.hasVerified(session, user));
    
    refresh();

    emailVerifyService.changeEmail(session, user, verify,
        ParameterMap.extractParameters(request));
    
    refresh();

    checkEmail(ImmutableMap.of("sakai:to", (Object)"smtp:"+newEmail, 
        "sakai:templatePath", (Object)"/var/templates/email/verify_email_change", 
        "sakai:templateParams", 
        (Object)"sender=" + uid + "|link=http://localhost:8080/~" + uid + "/emailVerify.verify.html?guid=" + guid));
    
    assertFalse(emailVerifyService.hasVerified(session, user));

    assertEquals(newEmail, user.getProperty("newemail"));
    
    assertEquals(origEmail, user.getProperty("email"));
    
    emailVerifyService.cancelChangeEmail(request, session, user, verify);
    
    refresh();
    
    assertTrue(emailVerifyService.hasVerified(session, user));

    // test changing the email manually
    user.setProperty("email", "blah@blah.com");
    
    session.getAuthorizableManager().updateAuthorizable(user);
    
    refresh();

    // didn't change
    assertEquals(origEmail, user.getProperty("email"));
    
    emailVerifyService.changeEmail(session, user, verify,
        ParameterMap.extractParameters(request));

    refresh();
    
    checkEmail(ImmutableMap.of("sakai:to", (Object)"smtp:"+newEmail, 
        "sakai:templatePath", (Object)"/var/templates/email/verify_email_change", 
        "sakai:templateParams", 
        (Object)"sender=" + uid + "|link=http://localhost:8080/~" + uid + "/emailVerify.verify.html?guid=" + guid));
    
    emailVerifyService.clearVerification(session, guid, verify);
    
    refresh();
    
    assertTrue(emailVerifyService.hasVerified(session, user));
    
    assertEquals(newEmail, user.getProperty("email"));
  }
  
}
