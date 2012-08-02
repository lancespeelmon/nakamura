package com.rsmart.oae.user.emailverify;


import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import com.rsmart.oae.system.api.upgrades.UpgradeUnit;
import com.rsmart.oae.system.api.upgrades.UpgradesManager;
import com.rsmart.oae.user.api.emailverify.EmailVerificationException;
import com.rsmart.oae.user.api.emailverify.EmailVerifyService;
import com.rsmart.oae.user.api.emailverify.InvalidLinkEmailVerificationException;
import com.rsmart.oae.user.api.emailverify.NotLoggedInEmailVerificationException;
import com.rsmart.oae.user.api.emailverify.WrongUserEmailVerificationException;
import com.rsmart.oae.user.api.emailverify.util.EmailServiceBase;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.commons.scheduler.Job;
import org.apache.sling.commons.scheduler.JobContext;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.servlets.post.Modification;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.message.LiteMessageRouterManager;
import org.sakaiproject.nakamura.api.message.LiteMessageTransport;
import org.sakaiproject.nakamura.api.message.LiteMessagingService;
import org.sakaiproject.nakamura.api.user.LiteAuthorizablePostProcessor;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.api.user.UserFinder;
import org.sakaiproject.nakamura.util.LitePersonalUtils;
import org.sakaiproject.nakamura.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

@Service
@Component(immediate = true, metatype=true)
@Reference(name = "MessageTransport", referenceInterface = LiteMessageTransport.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, bind = "addTransport", unbind = "removeTransport")
public class EmailVerifyServiceImpl extends EmailServiceBase implements EmailVerifyService, LiteAuthorizablePostProcessor {

  protected static final int HTTPS_PORT_443 = 443;

  protected static final int HTTP_PORT_80 = 80;

  private static final String EMAILVERIFIED = "emailverified";

  private static final String EMAIL = "email";

  public static final String PARAM_LANGUAGE = "l";

  protected static final String EMAIL_VERIFY_RESOURCE_PATH = "/emailVerify";

  protected static final String EMAIL_VERIFY_UUID_PROPERTY = "rsmart:emailVerifyUUID";
  
  protected static final String EMAIL_VERIFY_CHANGED_EMAIL_PROPERTY = "rsmart:changedEmail";

  protected static final String SLING_RESOURCE_TYPE = "sling:resourceType";

  protected static final String EMAIL_VERIFY_USER = "rsmart:verifyUser";

  protected static final String EMAIL_VERIFY_EMAIL = "rsmart:verifyEmail";

  @Property(boolValue = false)
  public static final String DISABLE_VERIFICATION = "disable.verification";

  @Property(value = "24")
  public static final String HOURS_TO_VERIFY = "emailverify.hours_to_verify";

  @Property(value = "60")
  public static final String MINUTES_BETWEEN_WARNINGS = "emailverify.minutes_between_warnings";

  @Property(value = "10")
  public static final String MINUTES_BEFORE_FIRST_WARNING = "emailverify.minutes_before_first_warnings";

  @Property(value = "23")
  public static final String HOURS_TIL_WARNING_EMAIL = "emailverify.hours_to_warning_email";

  private static final Logger
    LOG = LoggerFactory.getLogger(EmailVerifyServiceImpl.class);

  protected static final String EMAIL_VERIFY_PATH = "emailverify.verifyPath";

  private static final String USER_LOCALE = "emailverify.userLocale";

  protected static final String VERIFY_LINK = "emailverify.verifyLink";

  private boolean disableVerification;

  private long millisToVerify;

  private long millisBetweenWarnings;
  
  private long millisBeforeFirstWarning;

  private long millisToWarningEmail;
  
  @Reference
  protected transient Scheduler scheduler;

  @Reference
  protected transient Repository repository;

  @Reference
  protected transient SlingRepository slingRepository;

  @Reference
  protected transient UserFinder userFinder;
  
  @Reference
  protected transient UpgradesManager upgradesManager;

  @Reference
  protected transient LiteMessageRouterManager messageRouterManager;

  @Reference
  protected transient LiteMessagingService messagingService;

  /**
   * This will contain all the transports.
   */
  protected Map<LiteMessageTransport, LiteMessageTransport> transports =
      new ConcurrentHashMap<LiteMessageTransport, LiteMessageTransport>();


  /**
   * {@inheritDoc}
   *
   * @see com.rsmart.oae.user.api.emailverify.EmailVerifyService#sendVerifyEmail(org.sakaiproject.nakamura.api.lite.Session,
   *      org.sakaiproject.nakamura.api.lite.authorizable.User, java.util.Map)
   * @throws EmailVerificationException
   */
  public void sendVerifyEmail(final Session session, final User user,
      final Map<String, Object[]> parameters) {
    LOG.debug(
        "sendVerifyEmail(final Session session, final User {}, final Map<String, Object[]> parameters)",
        user);
	if (disableVerification){
	  LOG.debug("Email verification disabled. Not sending verify email to {}", user);
	  return;
	}

    String email = (String) user.getProperty(UserConstants.USER_EMAIL_PROPERTY);

    LOG.debug("verifying email: {0} for user {1}", new Object[] {email, user.getId()});

    Session adminSession = null;
    
    try {
      adminSession = repository.loginAdministrative();
      String authId = user.getId();
      String path = getPath(authId);
      ContentManager contentManager = session.getContentManager();

      Content verify = null;

      if (!verifyExists(contentManager, path)) {
        verify = createVerifyFile(session, user, email, authId, path, contentManager,
            true, parameters);
        setupAcl(false, user, adminSession.getAccessControlManager());
        user.setProperty("emailverifyseewarning", "" + (new Date().getTime() + millisBeforeFirstWarning));
        AuthorizableManager authorizableManager = session.getAuthorizableManager();
        authorizableManager.updateAuthorizable(user);
      }
      else {
        verify = getVerifyNode(contentManager, path);

        String expectedUser = verify.getProperty(EMAIL_VERIFY_USER).toString();

        if (!expectedUser.equals(authId)) {
          throw new EmailVerificationException();
        }

        String storedEmail = verify.getProperty(EMAIL_VERIFY_EMAIL).toString();

        if (!storedEmail.equalsIgnoreCase(email)) {
          // FIXME Dodgy - Dead store to local variable 
          email = storedEmail; // must be a change, right?
        }
      }
      
      String emailTemplate = "verify_email";

      sendVerificationEmail(session, user, verify, emailTemplate, parameters);
    } catch (StorageClientException e) {
      throw new EmailVerificationException(e);
    } catch (AccessDeniedException e) {
      throw new EmailVerificationException(e);
    } catch (MalformedURLException e) {
      throw new EmailVerificationException(e);
    } catch (PathNotFoundException e) {
      throw new EmailVerificationException(e);
    } catch (RepositoryException e) {
      throw new EmailVerificationException(e);
    } catch (IOException e) {
      throw new EmailVerificationException(e);
    }
    finally {
      try {
        adminSession.logout();
      } catch ( Exception e) {
        LOG.warn("Failed to logout of administrative session {} ", e);
      }
    }
  }

  protected void sendVerificationEmail(final Session session, final User user,
      final Content verify, final String emailTemplate,
      final Map<String, Object[]> parameters) throws RepositoryException, IOException {
    final URL url = getVerifyExternalURL(parameters, verify);
    String email = (String) verify.getProperty(EMAIL_VERIFY_EMAIL);

    Map<String, String> templateParams = new HashMap<String, String>();
    templateParams.put("link", url.toExternalForm());
    sendEmail(session, user, email, "/var/templates/email/" + emailTemplate,
        templateParams,
        getLocaleString(parameters, "VERIFY_EMAIL_SUBJECT", slingRepository));
    LOG.debug("message sent");
  }

  protected URL getVerifyExternalURL(final Map<String, Object[]> parameters,
      final Content verify) throws MalformedURLException {
    String verifyPath = PathUtils.translateAuthorizablePath(verify.getPath()).toString();
    String baseUrl = getBaseUrl(parameters);

    String uuid = (String) verify.getProperties().get(EMAIL_VERIFY_UUID_PROPERTY);

    URL url = new URL(new URL(baseUrl), verifyPath + ".verify.html?" +
        EmailVerifyService.EMAIL_VERIFY_GUID_PARAM + "=" + uuid);
    return url;
  }

  public void seenWarning(Session session, Content verification) {
    try {
      String userId = session.getUserId();
      if (!userId.equals(verification.getProperty(EMAIL_VERIFY_USER))) {
        throw new EmailVerificationException();
      }

      AuthorizableManager authorizableManager = session.getAuthorizableManager();
      User user = (User) authorizableManager.findAuthorizable(session.getUserId());

      if (user == null) {
        throw new EmailVerificationException();
      }

      user.setProperty("emailverifyseewarning", "" + (new Date().getTime() + millisBetweenWarnings));
      user.removeProperty("emailverifyerror");

      authorizableManager.updateAuthorizable(user);
    } catch (AccessDeniedException e) {
      throw new EmailVerificationException(e);
    } catch (StorageClientException e) {
      throw new EmailVerificationException(e);
    }
  }

  protected Content createVerifyFile(final Session session, final User user,
      final String email, final String authId, final String path,
      final ContentManager contentManager, final boolean startTimer,
      final Map<String, Object[]> parameters) throws StorageClientException,
      AccessDeniedException, RepositoryException, IOException {
    Content verify;
    AuthorizableManager authorizableManager = session.getAuthorizableManager();
    verify = createVerifyNode(contentManager, path, authId, email);
    Date verifyBy = new Date(System.currentTimeMillis() + millisToVerify);
    Date warnEmailBy = null;

    if (millisToWarningEmail > 0) {
      warnEmailBy = new Date(System.currentTimeMillis() + millisToWarningEmail);
    }

    user.setProperty("emailverifyby", "" + verifyBy.getTime());
    authorizableManager.updateAuthorizable(user);

    if (startTimer) {
      scheduleExpiry(verify, verifyBy, warnEmailBy, parameters);
    }

    return verify;
  }

  private void scheduleExpiry(final Content verify, final Date verifyBy,
      final Date warnEmailBy, final Map<String, Object[]> parameters)
      throws RepositoryException, IOException {
    Job warnEmail = new Job() {

      public void execute(JobContext jc) {
          Map<String, Serializable> config = jc.getConfiguration();
          javax.jcr.Session adminSession = null;
          try {
            adminSession = slingRepository.loginAdministrative(null);
            Session sparseSession = StorageClientUtils
              .adaptToSession(adminSession);

            String localVerifyPath = (String)config.get(EMAIL_VERIFY_PATH);
            Locale userLocale = (Locale)config.get(USER_LOCALE);
            URL verifyLink = (URL)config.get(VERIFY_LINK);

            sendWarningEmail(adminSession, sparseSession, userLocale, localVerifyPath, verifyLink);
          } catch (Exception e) {
            LOG.error("Unable to send email warning to user", e);
          }
          finally {
            try {
              adminSession.logout();
            } catch ( Error e) {
              LOG.warn("Failed to logout of administrative session {} ", e);
              throw e;
            }
        }
      }

    };

    Job killUser = new Job() {

      public void execute(JobContext jc) {
        Map<String, Serializable> config = jc.getConfiguration();
        javax.jcr.Session adminSession = null;
        try {
          adminSession = slingRepository.loginAdministrative(null);
          org.sakaiproject.nakamura.api.lite.Session sparseSession = StorageClientUtils
            .adaptToSession(adminSession);


          String localVerifyPath = (String)config.get(EMAIL_VERIFY_PATH);
          Locale userLocale = (Locale)config.get(USER_LOCALE);

          disableUser(adminSession, sparseSession, userLocale, localVerifyPath);
        } catch (Exception e) {
          LOG.error("Unable to send email or close user", e);
        }
        finally {
          try {
            adminSession.logout();
          } catch ( Exception e) {
            LOG.warn("Failed to logout of administrative session {} ", e);
          }
        }
      }

    };

    Map<String, Serializable> jobConfig = new HashMap<String, Serializable>();

    jobConfig.put(EMAIL_VERIFY_PATH, verify.getPath());
    jobConfig.put(USER_LOCALE, getLocale(parameters));
    // need to serialize cause request has the base url
    jobConfig.put(VERIFY_LINK, getVerifyExternalURL(parameters, verify));

    try {
      if (warnEmailBy != null) {
        scheduler.fireJobAt(null, warnEmail, new HashMap<String, Serializable>(jobConfig), warnEmailBy);
      }

      scheduler.fireJobAt(null, killUser, new HashMap<String, Serializable>(jobConfig), verifyBy);
    } catch (Exception e) {
      throw new EmailVerificationException(e);
    }
  }

  private void sendWarningEmail(javax.jcr.Session adminSession, Session session,
      Locale userLocale, String verifyPath, URL externalLink)
      throws StorageClientException, AccessDeniedException, RepositoryException,
      IOException {
    ContentManager contentManager = session.getContentManager();

    if (!verifyExists(contentManager, verifyPath)) {
      return; // we are not needed
    }

    Content verify = getVerifyNode(contentManager, verifyPath);

    if (hasVerified(verify)) {
      return; // not needed here either
    }

    Map<String, String> templateParams = new HashMap<String, String>();
    templateParams.put("link", externalLink.toExternalForm());

    AuthorizableManager authorizableManager = session.getAuthorizableManager();
    User user = (User) authorizableManager.findAuthorizable((String) verify.getProperty(EMAIL_VERIFY_USER));

    sendEmail(session, user, null, "/var/templates/email/warn_verify_email", templateParams,
        getLocaleString(userLocale, adminSession, "EMAIL_WARNING_SUBJECT"));
  }

  private void disableUser(javax.jcr.Session adminSession, Session session, Locale userLocale, String verifyPath)
      throws StorageClientException, AccessDeniedException,
      RepositoryException, IOException {
    ContentManager contentManager = session.getContentManager();

    if (!verifyExists(contentManager, verifyPath)) {
      return; // we are not needed
    }

    Content verify = getVerifyNode(contentManager, verifyPath);

    if (hasVerified(verify)) {
      return; // not needed here either
    }

    Map<String, String> templateParams = new HashMap<String, String>();

    AuthorizableManager authorizableManager = session.getAuthorizableManager();
    User user = (User) authorizableManager.findAuthorizable((String) verify.getProperty(EMAIL_VERIFY_USER));

    sendEmail(session, user, null, "/var/templates/email/disabled_verify_email", templateParams,
        getLocaleString(userLocale, adminSession, "EMAIL_DISABLED_SUBJECT"));

    authorizableManager.disablePassword(user);
  }

  protected String getPath(String authId) {
    return LitePersonalUtils.getHomePath(authId) + EMAIL_VERIFY_RESOURCE_PATH;
  }

  /**
   * @throws EmailVerificationException
   *           {@inheritDoc}
   * @see com.rsmart.oae.user.api.emailverify.EmailVerifyService#clearVerification(org.sakaiproject.nakamura.api.lite.Session,
   *      java.lang.String, org.sakaiproject.nakamura.api.lite.content.Content)
   */
  public void clearVerification(Session session, String guid, Content verification) {
    LOG.debug("clearVerification(Session session, String {}, Content {})", guid,
        verification);

    if (disableVerification){
      LOG.debug("Email verification disabled. Not clearing {}", guid);
      return;
    }

    if (session == null) {
      throw new IllegalArgumentException("Session session == null");
    }
    if (guid == null) {
      throw new IllegalArgumentException("String guid == null");
    }
    if (verification == null) {
      throw new IllegalArgumentException("Content verification == null");
    }
    // confirm that the guid matches that in the verification and that the current user is the verification user
    final String currentUserId = session.getUserId();

    final String expectedUserId = verification.getProperty(EMAIL_VERIFY_USER).toString();

    if (!currentUserId.equals(expectedUserId)) {
      if (currentUserId.equals(UserConstants.ANON_USERID)) {
        throw new NotLoggedInEmailVerificationException();
      }

      throw new WrongUserEmailVerificationException();
    }

    if (verification.getProperty(EMAIL_VERIFY_UUID_PROPERTY) == null) {
      throw new InvalidLinkEmailVerificationException();
    }
    
    final String expectedGuid = verification.getProperty(EMAIL_VERIFY_UUID_PROPERTY)
        .toString();

    try {
      AuthorizableManager authorizableManager = session.getAuthorizableManager();
      User user = (User) authorizableManager.findAuthorizable(currentUserId);

      if (user == null) {
        throw new NotLoggedInEmailVerificationException();
      }

      if (!guid.equals(expectedGuid)) {
        user.setProperty("emailverifyerror", true);
        authorizableManager.updateAuthorizable(user);
        throw new InvalidLinkEmailVerificationException();
      }

      if (Boolean.TRUE.equals(verification.getProperty(EMAIL_VERIFY_CHANGED_EMAIL_PROPERTY))) {
        // we have a changed email
        verification.removeProperty(EMAIL_VERIFY_CHANGED_EMAIL_PROPERTY);
        String newEmail = verification.getProperty(EMAIL_VERIFY_EMAIL).toString();

        Session adminSession = null;
        
        try {
          // login as admin
          adminSession = repository.loginAdministrative(UserConstants.ADMIN_USERID);
          
          authorizableManager = adminSession.getAuthorizableManager();
          user = (User) authorizableManager.findAuthorizable(currentUserId);
          user.removeProperty("newemail");
          user.setProperty(EMAIL, newEmail);
          authorizableManager.updateAuthorizable(user);
        }
        catch (Exception exp) {
          LOG.error("failed to update email", exp);
        } finally {
          // Destroy the admin session.
          adminSession.logout();
          authorizableManager = session.getAuthorizableManager();
          user = (User) authorizableManager.findAuthorizable(currentUserId);
        }
      }
      
      user.removeProperty("emailverifyby");
      user.setProperty(EMAILVERIFIED, true);

      authorizableManager.updateAuthorizable(user);

      ContentManager contentManager = session.getContentManager();

      verification.removeProperty(EMAIL_VERIFY_UUID_PROPERTY);
      
      contentManager.update(verification);
    } catch (AccessDeniedException e) {
      throw new EmailVerificationException(e);
    } catch (StorageClientException e) {
      throw new EmailVerificationException(e);
    }
  }

  public boolean hasVerified(Content verification) {
    String uuid = (String) verification.getProperties().get(EMAIL_VERIFY_UUID_PROPERTY);
    return uuid == null;
  }

  public void process(final Authorizable authz, final Session session,
      final Modification change, final Map<String, Object[]> parameters) {
    if (User.class.isAssignableFrom(authz.getClass())) {
      sendVerifyEmail(session, (User) authz, parameters);
    }
  }

  protected String createUUID() {
    return UUID.randomUUID().toString();
  }

  protected boolean verifyExists(ContentManager contentManager, String path) {
	if (disableVerification){
		LOG.debug("Email verification disabled. Not verifying content at {}", path);
		return true;
	}
	return contentManager.exists(path);
  }

  protected Content getVerifyNode(ContentManager contentManager, String path) throws AccessDeniedException, StorageClientException {
    return contentManager.get(path);
  }

  protected Content createVerifyNode(ContentManager contentManager, 
      String path, String authId, String email)
      throws StorageClientException, AccessDeniedException {

    if (contentManager.exists(path)) {
      throw new StorageClientException("users verify already exists");
    }
    else {
      String resourceType = EmailVerifyService.EMAIL_VERIFY_RT;
      Map<String, Object> additionalProperties = new HashMap<String, Object>();

      additionalProperties.put(EMAIL_VERIFY_UUID_PROPERTY, createUUID());
      additionalProperties.put(EMAIL_VERIFY_USER, authId);
      additionalProperties.put(EMAIL_VERIFY_EMAIL, email);

      Builder<String, Object> propertyBuilder = ImmutableMap.builder();
      propertyBuilder.put(SLING_RESOURCE_TYPE, resourceType);
      if (additionalProperties != null) {
        propertyBuilder.putAll(additionalProperties);
      }

      Content verify = new Content(path, propertyBuilder.build());
      contentManager.update(verify);

      return contentManager.get(path);
    }
  }
  
  protected Content createVerifyNode(ContentManager contentManager, User user) throws StorageClientException, AccessDeniedException {
    String email = (String) user.getProperty(UserConstants.USER_EMAIL_PROPERTY);
    String path = getPath(user.getId());
    return createVerifyNode(contentManager, path, user.getId(), email);
  }

  protected void activate(ComponentContext componentContext) {
    Dictionary<String, Object> props = componentContext.getProperties();

    millisToVerify = OsgiUtil.toLong (props.get(HOURS_TO_VERIFY), 24) * 60 * 60 * 1000;
    millisBetweenWarnings =  OsgiUtil.toLong (props.get(MINUTES_BETWEEN_WARNINGS), 60) * 60 * 1000;
    millisBeforeFirstWarning = OsgiUtil.toLong(props.get(MINUTES_BEFORE_FIRST_WARNING), 10) * 60 * 1000;
    millisToWarningEmail = OsgiUtil.toLong (props.get(HOURS_TIL_WARNING_EMAIL), 23) * 60 * 60 * 1000;
    disableVerification = OsgiUtil.toBoolean(props.get(DISABLE_VERIFICATION), false);
    
    upgradesManager.runUpgradeCode("emailverify", "verify existing users", new UpgradeUnit() {
      public void runUpgrade(Session sparseSession) throws Exception {
        runVerifyExistingUsers(sparseSession);
      }
    });
  }

  protected void runVerifyExistingUsers(Session sparseSession) throws Exception {
    ContentManager contentManager = sparseSession.getContentManager();
    AuthorizableManager authManager = sparseSession.getAuthorizableManager();
    AccessControlManager accessControlManager = sparseSession.getAccessControlManager();
    
    Set<String> allUsers = userFinder.allUsers();
    
    for (String uid : allUsers) {
      verifyExistingUser(uid, contentManager, authManager, accessControlManager);
    }
    
    verifyExistingUser(UserConstants.ADMIN_USERID, contentManager, authManager, accessControlManager);
  }
  

  private void verifyExistingUser(String authId, ContentManager contentManager, AuthorizableManager authManager, 
        AccessControlManager accessControlManager) throws AccessDeniedException, StorageClientException {
    User user = (User) authManager.findAuthorizable(authId);
    
    Content verify = getVerifyNode(contentManager, getPath(authId));
    
    if (verify != null) {
      return; // this user is already processed or something...
    }

    setupAcl(false, user, accessControlManager); // no user can change their email address w/o verifying
    
    // create verify file
    verify = createVerifyNode(contentManager, user);
    verify.removeProperty(EMAIL_VERIFY_UUID_PROPERTY);
    contentManager.update(verify);
    
    // set user properties emailverified
    user.setProperty(EMAILVERIFIED, true);
    
    authManager.updateAuthorizable(user);
  }

  protected String getBaseUrl(final Map<String, Object[]> parameters) {
    final Integer serverPort = (Integer) getFirstValueFromArray(
        "SlingHttpServletRequest.serverPort", parameters);
    final String scheme = (String) getFirstValueFromArray(
        "SlingHttpServletRequest.scheme", parameters);
    final String serverName = (String) getFirstValueFromArray(
        "SlingHttpServletRequest.serverName", parameters);
    final String contextPath = (String) getFirstValueFromArray(
        "SlingHttpServletRequest.contextPath", parameters);
    if ((serverPort.equals(HTTP_PORT_80)) || (serverPort.equals(HTTPS_PORT_443))) {
      return scheme + "://" + serverName + contextPath;
    } else {
      return scheme + "://" + serverName + ":" + serverPort + contextPath;
    }
  }

  protected void setupAcl(boolean verified, User user, AccessControlManager accessControlManager)
      throws StorageClientException, AccessDeniedException {

    List<AclModification> modifications = new ArrayList<AclModification>();

    AclModification.addAcl(verified, Permissions.CAN_WRITE_PROPERTY, AclModification.getPropertyKey(user.getId(), EMAIL),
        modifications);

    accessControlManager.setAcl(Security.ZONE_AUTHORIZABLES, user.getId(),
        modifications.toArray(new AclModification[modifications.size()]));
    
    modifications.clear();

    AclModification.addAcl(true, Permissions.CAN_WRITE_PROPERTY, AclModification.getPropertyKey(user.getId(), EMAIL),
        modifications);

    accessControlManager.setAcl(Security.ZONE_AUTHORIZABLES, UserConstants.ADMIN_USERID,
        modifications.toArray(new AclModification[modifications.size()]));    
  }

  public void changeEmail(Session session, User user, Content verification,
      final Map<String, Object[]> parameters) {
    if (!hasVerified(verification)) {
      throw new EmailVerificationException("user has not verified yet");
    }
    
    verification.setProperty(EMAIL_VERIFY_CHANGED_EMAIL_PROPERTY, new Boolean(true));
    verification.setProperty(EMAIL_VERIFY_EMAIL,
        getFirstValueFromArray(EMAIL, parameters));
    verification.setProperty(EMAIL_VERIFY_UUID_PROPERTY, createUUID());

    user.setProperty("newemail", verification.getProperty(EMAIL_VERIFY_EMAIL));
    user.removeProperty(EMAILVERIFIED);  // they are no longer verified
    
    try {
      AuthorizableManager authorizableManager = session.getAuthorizableManager();
      
      ContentManager contentManager = session.getContentManager();
      contentManager.update(verification);

      authorizableManager.updateAuthorizable(user);

      String emailTemplate = "verify_email_change";

      sendVerificationEmail(session, user, verification, emailTemplate, parameters);
    } catch (AccessDeniedException e) {
      throw new EmailVerificationException(e);
    } catch (StorageClientException e) {
      throw new EmailVerificationException(e);
    } catch (MalformedURLException e) {
      throw new EmailVerificationException(e);
    } catch (PathNotFoundException e) {
      throw new EmailVerificationException(e);
    } catch (RepositoryException e) {
      throw new EmailVerificationException(e);
    } catch (IOException e) {
      throw new EmailVerificationException(e);
    }
  }

  public void cancelChangeEmail(SlingHttpServletRequest request, Session session,
      User user, Content verification) {
    if (hasVerified(verification)) {
      throw new EmailVerificationException("user has already verified");
    }

    String expectedUserId = verification.getProperty(EMAIL_VERIFY_USER).toString();

    if (!user.getId().equals(expectedUserId)) {
      throw new WrongUserEmailVerificationException();
    }

    if (!verification.getProperty(EMAIL_VERIFY_CHANGED_EMAIL_PROPERTY).equals(true)) {
      throw new EmailVerificationException("email change is not in progress");
    }
    
    verification.removeProperty(EMAIL_VERIFY_CHANGED_EMAIL_PROPERTY);
    verification.removeProperty(EMAIL_VERIFY_EMAIL);
    verification.removeProperty(EMAIL_VERIFY_UUID_PROPERTY);

    user.removeProperty("newemail");
    user.setProperty(EMAILVERIFIED, true);  // they are back to verified
    
    try {
      AuthorizableManager authorizableManager = session.getAuthorizableManager();
      
      ContentManager contentManager = session.getContentManager();
      contentManager.update(verification);

      authorizableManager.updateAuthorizable(user);
    } catch (AccessDeniedException e) {
      throw new EmailVerificationException(e);
    } catch (StorageClientException e) {
      throw new EmailVerificationException(e);
    }
    
  }

  public boolean hasVerified(Session session, User user) {
	if (disableVerification){
	  LOG.debug("Email verification disabled. Not verifying {}", user);
	  return true;
	}

    try {
      ContentManager contentManager = session.getContentManager();
      Content verification = getVerifyNode(contentManager, getPath(user.getId()));
      
      return hasVerified(verification);
    } catch (StorageClientException e) {
      throw new EmailVerificationException(e);
    } catch (AccessDeniedException e) {
      throw new EmailVerificationException(e);
    }
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
    transports.put(transport,transport);
  }

  @Override
  protected Map<LiteMessageTransport, LiteMessageTransport> getTransports() {
    return transports;
  }

  @Override
  protected String getBundleName() {
    return "/devwidgets/emailverify/bundles";
  }

}
