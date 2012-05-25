package org.sakaiproject.nakamura.basiclti;

import static org.sakaiproject.nakamura.basiclti.LiteBasicLTIServletUtils.sensitiveKeys;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permission;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification.Operation;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.LTI_KEY;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.LTI_SECRET;

@RunWith(MockitoJUnitRunner.class)
public class LiteBasicLTIPostOperationTest {
  LiteBasicLTIPostOperation liteBasicLTIPostOperation;
  String sensitiveNodePath = "/foo/bar/baz";
  String currentUserId = "lance";
  Map<String, String> sensitiveData = new HashMap<String, String>(sensitiveKeys.size());
  Permission[] userPrivs;
  final String adminUserId = User.ADMIN_USER;

  @Mock
  Repository repository;
  @Mock
  EventAdmin eventAdmin;
  @Mock
  Session adminSession;
  @Mock
  Session userSession;
  @Mock
  AccessControlManager accessControlManager;
  @Mock
  Content parent;
  @Mock
  ContentManager adminContentManager;

  @Before
  public void setUp() throws Exception {
    liteBasicLTIPostOperation = new LiteBasicLTIPostOperation();
    liteBasicLTIPostOperation.repository = repository;
    liteBasicLTIPostOperation.eventAdmin = eventAdmin;
    when(adminSession.getAccessControlManager()).thenReturn(accessControlManager);
    when(parent.getPath()).thenReturn(sensitiveNodePath);
    when(repository.loginAdministrative()).thenReturn(adminSession);
    when(adminSession.getContentManager()).thenReturn(adminContentManager);
    when(userSession.getAccessControlManager()).thenReturn(accessControlManager);
    sensitiveData.put(LTI_KEY, "ltiKey");
    sensitiveData.put(LTI_SECRET, "ltiSecret");
    when(userSession.getUserId()).thenReturn(currentUserId);
    userPrivs = new Permission[] {};
    when(accessControlManager.getPermissions(eq(Security.ZONE_CONTENT), anyString()))
        .thenReturn(userPrivs);
  }

  /**
   * Validate happy case for
   * {@link LiteBasicLTIPostOperation#accessControlSensitiveNode(String, Session, String)}
   * 
   * @throws StorageClientException
   * @throws AccessDeniedException
   */
  @Test
  public void testAccessControlSensitiveNode() throws StorageClientException,
      AccessDeniedException {
    liteBasicLTIPostOperation.accessControlSensitiveNode(sensitiveNodePath, adminSession,
        currentUserId);
    verifyAclModification();
  }

  /**
   * Happy case where everything goes as expected.
   * {@link LiteBasicLTIPostOperation#createSensitiveNode(Content, Session, Map)}
   * 
   * @throws Exception
   */
  @Test
  public void testCreateSensitiveNode() throws Exception {
    liteBasicLTIPostOperation.createSensitiveNode(parent, userSession, sensitiveData);
    verify(adminContentManager, atLeastOnce()).update(any(Content.class));
    verify(adminSession, atMost(1)).logout();
    verifyAclModification();
  }

  /**
   * Edge case where deny acls were applied but the sanity check fails.
   * {@link LiteBasicLTIPostOperation#createSensitiveNode(Content, Session, Map)}
   * 
   * @throws Exception
   */
  @Test(expected = IllegalStateException.class)
  public void testCreateSensitiveNodeFailedAclModification() throws Exception {
    userPrivs = new Permission[] { Permissions.CAN_READ };
    when(accessControlManager.getPermissions(eq(Security.ZONE_CONTENT), anyString()))
        .thenReturn(userPrivs);

    liteBasicLTIPostOperation.createSensitiveNode(parent, userSession, sensitiveData);
  }

  /**
   * Test argument passing null parent node.
   * {@link LiteBasicLTIPostOperation#createSensitiveNode(Content, Session, Map)}
   * 
   * @throws Exception
   */
  @Test(expected = IllegalArgumentException.class)
  public void testCreateSensitiveNodePassNullParent() throws Exception {
    liteBasicLTIPostOperation.createSensitiveNode(null, userSession, sensitiveData);
  }

  /**
   * Test argument passing null userSession.
   * {@link LiteBasicLTIPostOperation#createSensitiveNode(Content, Session, Map)}
   * 
   * @throws Exception
   */
  @Test(expected = IllegalArgumentException.class)
  public void testCreateSensitiveNodePassUserSession() throws Exception {
    liteBasicLTIPostOperation.createSensitiveNode(parent, null, sensitiveData);
  }

  /**
   * Test argument passing null sensitiveData.
   * {@link LiteBasicLTIPostOperation#createSensitiveNode(Content, Session, Map)}
   * 
   * @throws Exception
   */
  @Test
  public void testCreateSensitiveNodePassNullSensitiveData() throws Exception {
    liteBasicLTIPostOperation.createSensitiveNode(parent, userSession, null);
    verify(adminContentManager, never()).update(any(Content.class));
    verify(adminSession, never()).logout();
  }

  /**
   * Test argument passing empty sensitiveData.
   * {@link LiteBasicLTIPostOperation#createSensitiveNode(Content, Session, Map)}
   * 
   * @throws Exception
   */
  @Test
  public void testCreateSensitiveNodePassEmptySensitiveData() throws Exception {
    sensitiveData.clear();
    liteBasicLTIPostOperation.createSensitiveNode(parent, userSession, sensitiveData);
    verify(adminContentManager, never()).update(any(Content.class));
    verify(adminSession, never()).logout();
  }

  /**
   * Test exception handling for AccessDeniedException.
   * {@link LiteBasicLTIPostOperation#createSensitiveNode(Content, Session, Map)}
   * 
   * @throws Exception
   */
  @Test(expected = IllegalStateException.class)
  public void testCreateSensitiveNodeWhenAccessDeniedExceptionThrown() throws Exception {
    doThrow(AccessDeniedException.class).when(adminContentManager).update(
        any(Content.class));
    liteBasicLTIPostOperation.createSensitiveNode(parent, userSession, sensitiveData);
  }

  /**
   * Test exception handling for StorageClientException.
   * {@link LiteBasicLTIPostOperation#createSensitiveNode(Content, Session, Map)}
   * 
   * @throws Exception
   */
  @Test(expected = IllegalStateException.class)
  public void testCreateSensitiveNodeWhenStorageClientExceptionThrown() throws Exception {
    doThrow(StorageClientException.class).when(adminContentManager).update(
        any(Content.class));
    liteBasicLTIPostOperation.createSensitiveNode(parent, userSession, sensitiveData);
  }

  /**
   * Test exception handling for ClientPoolException.
   * {@link LiteBasicLTIPostOperation#createSensitiveNode(Content, Session, Map)}
   * 
   * @throws Exception
   */
  @Test(expected = IllegalStateException.class)
  public void testCreateSensitiveNodeWhenClientPoolExceptionThrown() throws Exception {
    doThrow(ClientPoolException.class).when(adminSession).logout();
    liteBasicLTIPostOperation.createSensitiveNode(parent, userSession, sensitiveData);
  }

  /**
   * Code coverage case when current user could be an admin.
   * {@link LiteBasicLTIPostOperation#createSensitiveNode(Content, Session, Map)}
   * 
   * @throws Exception
   */
  @Test
  public void testCreateSensitiveNodeAsAdminUser() throws Exception {
    when(userSession.getUserId()).thenReturn(adminUserId);

    liteBasicLTIPostOperation.createSensitiveNode(parent, userSession, sensitiveData);
    verify(adminContentManager, atLeastOnce()).update(any(Content.class));
    verify(adminSession, atMost(1)).logout();
  }

  /**
   * Code coverage case when adminSession could be null.
   * {@link LiteBasicLTIPostOperation#createSensitiveNode(Content, Session, Map)}
   * 
   * @throws Exception
   */
  @Test
  public void testCreateSensitiveNodeNullAdminSession() throws Exception {
    when(repository.loginAdministrative()).thenReturn(null);

    liteBasicLTIPostOperation.createSensitiveNode(parent, userSession, sensitiveData);
    verify(adminContentManager, never()).update(any(Content.class));
    verify(adminSession, never()).logout();
  }

  /**
   * Code coverage case where userPrivs could be null.
   * {@link LiteBasicLTIPostOperation#createSensitiveNode(Content, Session, Map)}
   * 
   * @throws Exception
   */
  @Test
  public void testCreateSensitiveNodeNullUserPrivs() throws Exception {
    userPrivs = null;
    when(accessControlManager.getPermissions(eq(Security.ZONE_CONTENT), anyString()))
        .thenReturn(userPrivs);

    liteBasicLTIPostOperation.createSensitiveNode(parent, userSession, sensitiveData);
    verify(adminContentManager, atLeastOnce()).update(any(Content.class));
    verify(adminSession, atMost(1)).logout();
    verifyAclModification();
  }

  /**
   * Happy case. Code coverage case where userPrivs are not empty or null but also cannot
   * be matched.
   * {@link LiteBasicLTIPostOperation#createSensitiveNode(Content, Session, Map)}
   * 
   * @throws Exception
   */
  @Test
  public void testCreateSensitiveNodeUserPrivsNotMatched() throws Exception {
    userPrivs = new Permission[] { Permissions.CAN_WRITE_PROPERTY };
    when(accessControlManager.getPermissions(eq(Security.ZONE_CONTENT), anyString()))
        .thenReturn(userPrivs);

    liteBasicLTIPostOperation.createSensitiveNode(parent, userSession, sensitiveData);
    verify(adminContentManager, atLeastOnce()).update(any(Content.class));
    verify(adminSession, atMost(1)).logout();
    verifyAclModification();
  }

  /**
   * Code coverage case where StorageClientException is thrown but not rethrown; only
   * logged. {@link LiteBasicLTIPostOperation#createSensitiveNode(Content, Session, Map)}
   * 
   * @throws Exception
   */
  @Test
  public void testCreateSensitiveNodeWhenStorageClientExceptionThrown2() throws Exception {
    doThrow(StorageClientException.class).when(accessControlManager).getPermissions(
        anyString(), anyString());

    liteBasicLTIPostOperation.createSensitiveNode(parent, userSession, sensitiveData);
    verify(adminContentManager, atLeastOnce()).update(any(Content.class));
    verify(adminSession, atMost(1)).logout();
    verifyAclModification();
  }

  // --------------------------------------------------------------------------

  private void verifyAclModification() throws StorageClientException,
      AccessDeniedException {
    final ArgumentCaptor<AclModification[]> aclModificationArrayArgument = ArgumentCaptor
        .forClass(AclModification[].class);
    verify(accessControlManager, atLeastOnce()).setAcl(eq(Security.ZONE_CONTENT),
        anyString(), aclModificationArrayArgument.capture());
    // ensure we are applying the right deny Acls
    final List<AclModification> aclModifications = Arrays
        .asList(aclModificationArrayArgument.getValue());
    final AclModification denyAnonymous = new AclModification(
        AclModification.denyKey(User.ANON_USER), Permissions.ALL.getPermission(),
        Operation.OP_REPLACE);
    final AclModification denyEveryone = new AclModification(
        AclModification.denyKey(Group.EVERYONE), Permissions.ALL.getPermission(),
        Operation.OP_REPLACE);
    final AclModification denyCurrentUser = new AclModification(
        AclModification.denyKey(currentUserId), Permissions.ALL.getPermission(),
        Operation.OP_REPLACE);
    assertTrue(aclModifications.contains(denyAnonymous));
    assertTrue(aclModifications.contains(denyEveryone));
    assertTrue(aclModifications.contains(denyCurrentUser));
  }

}
