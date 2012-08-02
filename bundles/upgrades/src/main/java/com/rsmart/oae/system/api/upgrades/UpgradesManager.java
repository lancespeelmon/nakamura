package com.rsmart.oae.system.api.upgrades;

public interface UpgradesManager {
  
  
  static final String UPGRADES_MANAGER_FILE_TYPE = "upgradesFile";

  /**
   * used to check if a component has run the passed in upgrade yet
   * @param componentName name of the component in question
   * @param upgradeName name of the upgrade to run
   * @return true if the component has already run this upgrade
   */
  boolean checkRunVersion(String componentName, String upgradeName);
  
  /**
   * tell the UpgradesManager that this upgrade has been successfully run
   * @param componentName name of the component in question
   * @param upgradeName name of the upgrade that was run
   * @param success true if there was a successful run
   * @param exception any exception that might have occurred during the running of a failed upgrade
   */
  void setRunVersion(String componentName, String upgradeName, boolean success, Throwable exception);
  
  /**
   * convenience method that will execute the runnable if {@link #checkRunVersion(String, String)} 
   * returns true, then calls {@link #setRunVersion(String, String, boolean, Throwable)} with
   * the results
   * @param componentName name of the component in question
   * @param upgradeName name of the upgrade to run
   * @param upgrade {@link UpgradeUnit} that gets executed synchronously
   */
  void runUpgradeCode(String componentName, String upgradeName, UpgradeUnit upgrade);

}
