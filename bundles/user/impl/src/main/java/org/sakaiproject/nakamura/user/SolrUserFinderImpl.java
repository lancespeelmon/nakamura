package org.sakaiproject.nakamura.user;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.search.SearchUtil;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.solr.SolrServerService;
import org.sakaiproject.nakamura.api.user.UserFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

@Component(label = "SolrUserFinder", description = "Find users using the Solr index")
@Service
public class SolrUserFinderImpl implements UserFinder {

  @Reference
  protected SolrServerService solrSearchService;
  
  @Reference
  protected Repository repository;

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrUserFinderImpl.class);

  /**
   * do a case insensitive solr search for user's name which is indexed as a
   * case-insensitive solr text field in AuthorizableIndexingHandler see KERN-2211
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.user.UserFinder#findUsersByName(java.lang.String)
   */
  public Set<String> findUsersByName(String name) throws Exception {
    return findUsersByField("name", name);
  }

  /**
   * do a case insensitive solr search for user's email which is indexed as a
   * case-insensitive solr text field in AuthorizableIndexingHandler
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.user.UserFinder#findUsersByEmail(java.lang.String)
   */
  public Set<String> findUsersByEmail(String email) throws Exception {
    return findUsersByField("email", email);
  }

  /**
   * using a case insensitive solr search, determine whether one or more users of this
   * name exist {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.user.UserFinder#userExists(java.lang.String)
   */
  public boolean userExists(String name) throws Exception {
    boolean userExists = false;
    Set<String> userIds = findUsersByName(name);
    if (!userIds.isEmpty()) {
      userExists = true;
    }
    LOGGER.debug("user with name " + name + " exists: " + userExists);
    return userExists;
  }
  
  protected Set<String> findUsersByField(String fieldName, String fieldValue) throws Exception {
    Set<String> userIds = new HashSet<String>();
    SolrServer solrServer = solrSearchService.getServer();
    String queryString = "resourceType:authorizable AND type:u AND " + fieldName + ":" + fieldValue;
    SolrQuery solrQuery = new SolrQuery(queryString);
    QueryResponse queryResponse = solrServer.query(solrQuery);
    SolrDocumentList results = queryResponse.getResults();
    for (SolrDocument solrDocument : results) {
      if (solrDocument.containsKey("id")) {
        userIds.add((String) solrDocument.getFieldValue("id"));
      }
    }
    LOGGER.debug("found these users by " + fieldName + ": " + userIds);
    return userIds;
  }

  protected Set<String> findUsersByFields(String[] fieldNames, String fieldValue) throws Exception {
    Set<String> userIds = new HashSet<String>();
    SolrServer solrServer = solrSearchService.getServer();
    StringBuilder queryString = new StringBuilder("resourceType:authorizable AND type:u AND (");
    
    boolean first = true;
    for(String fieldName : fieldNames) {
      if (!first) {
        queryString.append(" OR ");
      }
      
      queryString.append(fieldName + ":" + fieldValue);
      first = false;
    }
    
    queryString.append(")");
    
    SolrQuery solrQuery = new SolrQuery(queryString.toString());
    QueryResponse queryResponse = solrServer.query(solrQuery);
    SolrDocumentList results = queryResponse.getResults();
    for (SolrDocument solrDocument : results) {
      if (solrDocument.containsKey("id")) {
        userIds.add((String) solrDocument.getFieldValue("id"));
      }
    }
    LOGGER.debug("found these users by " + fieldNames + ": " + userIds);
    return userIds;
  }

  public boolean userWithEmailExists(String email) throws Exception {
    Set<String> emailUsers = findUsersByEmail(email);
    Set<String> newEmailUsers = findUsersByField("newemail", email);
    
    if (emailUsers.isEmpty() && newEmailUsers.isEmpty()) {
      return false;
    } else {
      return true;
    }
  }

  public Set<String> allUsers() throws Exception {
    Set<String> userIds = new HashSet<String>();
    SolrServer solrServer = solrSearchService.getServer();
    StringBuilder queryString = new StringBuilder("resourceType:authorizable AND type:u");
    
    SolrQuery solrQuery = new SolrQuery(queryString.toString());
    QueryResponse queryResponse = solrServer.query(solrQuery);
    SolrDocumentList results = queryResponse.getResults();
    for (SolrDocument solrDocument : results) {
      if (solrDocument.containsKey("id")) {
        userIds.add((String) solrDocument.getFieldValue("id"));
      }
    }
    
    return userIds;
  }

}
