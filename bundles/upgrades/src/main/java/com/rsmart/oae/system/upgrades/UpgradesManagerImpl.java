package com.rsmart.oae.system.upgrades;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import com.rsmart.oae.system.api.upgrades.UpgradeUnit;
import com.rsmart.oae.system.api.upgrades.UpgradesManager;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Component(immediate = true, metatype=true)
public class UpgradesManagerImpl implements UpgradesManager {

  protected static final String UPGRADES_MANAGER_PATH = "/var/system/upgrades/";

  private static final String SLING_RESOURCE_TYPE = "sling:resourceType";

  private static final Logger
    LOG = LoggerFactory.getLogger(UpgradesManagerImpl.class);
  
  @Reference
  protected Repository repository;
  
  public boolean checkRunVersion(String componentName, String upgradeName) {
    Session adminSession = null;
    try { 
      adminSession = repository.loginAdministrative(User.ADMIN_USER);
      return checkRunVersion(componentName, upgradeName, adminSession);
    } catch (StorageClientException e) {
      throw new RuntimeException(e);
    } catch (AccessDeniedException e) {
      throw new RuntimeException(e);
    }
    finally {
      try { 
        adminSession.logout(); 
      } catch ( Exception e) {
        LOG.warn("Failed to logout of administrative session {} ",e.getMessage());
      }
    }
  }

  public void setRunVersion(String componentName, String upgradeName, boolean success,
      Throwable exception) {
    Session adminSession = null;
    try { 
      adminSession = repository.loginAdministrative(User.ADMIN_USER);

      setRunVersion(componentName, upgradeName, success, exception, adminSession);
    } catch (StorageClientException e) {
      throw new RuntimeException(e);
    } catch (AccessDeniedException e) {
      throw new RuntimeException(e);
    }
    finally {
      try { 
        adminSession.logout(); 
      } catch ( Exception e) {
        LOG.warn("Failed to logout of administrative session {} ",e.getMessage());
      }
    }
  }

  protected boolean checkRunVersion(String componentName, String upgradeName, Session session) throws StorageClientException, AccessDeniedException {
    Content componentFile = getComponentFile(componentName, session);
    
    if (new Boolean(true).equals(componentFile.getProperty(upgradeName + ".run"))) {
      return true;
    }
    
    return false;
  }

  protected void setRunVersion(String componentName, String upgradeName, boolean success,
      Throwable exception, Session session) throws StorageClientException, AccessDeniedException {
    Content componentFile = getComponentFile(componentName, session);

    componentFile.setProperty(upgradeName + ".run", success);
    componentFile.setProperty(upgradeName + ".updated", Calendar.getInstance());
    
    if (!success && exception != null) {
      StringWriter sw = new StringWriter();
      exception.printStackTrace(new PrintWriter(sw));
      
      componentFile.setProperty(upgradeName + ".exception", sw.toString());
    }
    
    session.getContentManager().update(componentFile);
  }
    
  protected Content getComponentFile(String componentName, Session session) throws StorageClientException, AccessDeniedException {
    String path = UPGRADES_MANAGER_PATH + componentName;
    
    ContentManager contentManager = session.getContentManager();
    
    Content ret = contentManager.get(path);
    
    if (ret == null) {
      System.out.println("failed to find file -- creating it");
      String resourceType = UpgradesManager.UPGRADES_MANAGER_FILE_TYPE;
      Map<String, Object> additionalProperties = new HashMap<String, Object>();
      
      additionalProperties.put("create", Calendar.getInstance());

      Builder<String, Object> propertyBuilder = ImmutableMap.builder();
      propertyBuilder.put(SLING_RESOURCE_TYPE, resourceType);
      if (additionalProperties != null) {
        propertyBuilder.putAll(additionalProperties);
      }

      ret = new Content(path, propertyBuilder.build());
      contentManager.update(ret);

      ret = contentManager.get(path);
      
      setupAcl(path, session.getAccessControlManager());
    }
    
    return ret;
  }

  protected void setupAcl(String path, AccessControlManager accessControlManager) throws StorageClientException, AccessDeniedException {
    List<AclModification> aclModifications = new ArrayList<AclModification>();
    AclModification.addAcl(false, Permissions.CAN_ANYTHING, User.ANON_USER,
        aclModifications);
    AclModification.addAcl(false, Permissions.CAN_ANYTHING, Group.EVERYONE,
        aclModifications);
    AclModification.addAcl(true, Permissions.CAN_ANYTHING, User.ADMIN_USER,
        aclModifications);

    AclModification[] aclMods = aclModifications
        .toArray(new AclModification[aclModifications.size()]);
    accessControlManager.setAcl(Security.ZONE_CONTENT, path, aclMods);
  }
  
  public void runUpgradeCode(String componentName, String upgradeName, UpgradeUnit upgrade) {
    Session adminSession = null;
    try { 
      adminSession = repository.loginAdministrative(User.ADMIN_USER);

      if (!checkRunVersion(componentName, upgradeName, adminSession)) {
        try {
          upgrade.runUpgrade(adminSession);
          setRunVersion(componentName, upgradeName, true, null, adminSession);
        }
        catch (Throwable e) {
          setRunVersion(componentName, upgradeName, false, e, adminSession);
        }
      }
    } catch (StorageClientException e) {
      LOG.error("failed to run upgrade code", e);
    } catch (AccessDeniedException e) {
      LOG.error("failed to run upgrade code", e);
    }
    finally {
      try { 
        adminSession.logout(); 
      } catch ( Exception e) {
        LOG.warn("Failed to logout of administrative session {} ",e.getMessage());
      }
    }
  }

}
