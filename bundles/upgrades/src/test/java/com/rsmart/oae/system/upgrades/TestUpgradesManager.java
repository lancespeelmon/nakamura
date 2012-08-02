package com.rsmart.oae.system.upgrades;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.rsmart.oae.system.api.upgrades.UpgradeUnit;
import com.rsmart.oae.system.api.upgrades.UpgradesManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.lite.BaseMemoryRepository;

import java.io.IOException;


public class TestUpgradesManager {

  private UpgradesManager upgradesManager;
    
  private Repository repository = null;

  public TestUpgradesManager() {
    System.out.println("constructing test");
    upgradesManager = new UpgradesManagerImpl();
    
    try {
      repository = new BaseMemoryRepository().getRepository();
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

    ((UpgradesManagerImpl)upgradesManager).repository = repository;
  }

  @Before
  public void before()
      throws Exception {
    run = false;
  }
  
  @After
  public void after()
      throws Exception {
  }

  private boolean run;
  
  
  @Test
  public void testDoSuccessfulUpgrades()
      throws Exception {
    
    // make sure it starts out false
    assertEquals(false, upgradesManager.checkRunVersion("upgradesTest", "testDoSuccessfulUpgrades"));

    upgradesManager.runUpgradeCode("upgradesTest", "testDoSuccessfulUpgrades", new UpgradeUnit() {
      public void runUpgrade(Session sparseSession) throws Exception {
        run = true;
      }
    });
    
    // make sure it did run...
    assertEquals(true, run);
    
    System.out.println("about to check if it ran");
    // make sure it is now set to true
    assertEquals(true, upgradesManager.checkRunVersion("upgradesTest", "testDoSuccessfulUpgrades"));
    System.out.println("checked if it ran");

    upgradesManager.runUpgradeCode("upgradesTest", "testDoSuccessfulUpgrades", new UpgradeUnit() {
      public void runUpgrade(Session sparseSession) throws Exception {
        // make sure it didn't run again
        fail();
      }
    });
    
    // test that no one else can read or write to the file
    Session session = repository.loginAdministrative(User.ANON_USER);
    
    ContentManager contentManager = session.getContentManager();

    try {
      Content content = contentManager.get(UpgradesManagerImpl.UPGRADES_MANAGER_PATH + "upgradesTest");
      assertNull(content);
      fail("AccessDeniedException SHOULD be thrown");
    }
    catch (AccessDeniedException e) {
      // this should happen
      assertNotNull("AccessDeniedException SHOULD be thrown", e);
    }
  }
  
  @Test
  public void testDoFailedUpgrades() throws Exception {
    // make sure it starts out false
    assertEquals(false, upgradesManager.checkRunVersion("upgradesTest", "testDoFailedUpgrades"));

    upgradesManager.runUpgradeCode("upgradesTest", "testDoFailedUpgrades", new UpgradeUnit() {
      public void runUpgrade(Session sparseSession) throws Exception {
        run = true;
        throw new Exception("this failed on purpose");
      }
    });
    
    // make sure it did TRY to run...
    assertEquals(run, true);
    
    run = false;
    
    // make sure it is still false (cause of the exception)
    assertEquals(false, upgradesManager.checkRunVersion("upgradesTest", "testDoFailedUpgrades"));
    
    upgradesManager.runUpgradeCode("upgradesTest", "testDoFailedUpgrades", new UpgradeUnit() {
      public void runUpgrade(Session sparseSession) throws Exception {
        run = true;
      }
    });
    
    // make sure it does run again
    assertEquals(run, true);
    
  }

}
