package com.rsmart.oae.system.api.upgrades;

import org.sakaiproject.nakamura.api.lite.Session;

public interface UpgradeUnit {
  
  void runUpgrade(Session sparseSession) throws Exception;

}
