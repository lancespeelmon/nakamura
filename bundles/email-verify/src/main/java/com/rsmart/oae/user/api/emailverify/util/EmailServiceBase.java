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
package com.rsmart.oae.user.api.emailverify.util;

import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.jcr.api.SlingRepository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.message.LiteMessageRouterManager;
import org.sakaiproject.nakamura.api.message.LiteMessageTransport;
import org.sakaiproject.nakamura.api.message.LiteMessagingService;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessageRoutes;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.parameters.ParameterMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

@Service
public abstract class EmailServiceBase {

  private static final String PARAM_LANGUAGE = "l";

  private static final Logger baseLogger = LoggerFactory
      .getLogger(EmailServiceBase.class);

  public void sendEmail(Session session, User user, String email, String template,
      Map<String, String> templateParams, String localizedSubject)
      throws PathNotFoundException, RepositoryException, IOException {

    if (email == null) {
      email = (String) user.getProperty(UserConstants.USER_EMAIL_PROPERTY);
    }

    Map<String, Object> mapProperties = new HashMap<String, Object>();
    mapProperties.put(MessageConstants.PROP_SAKAI_TYPE, "smtp");
    mapProperties.put(MessageConstants.PROP_SAKAI_SENDSTATE, "pending");
    mapProperties.put(MessageConstants.PROP_SAKAI_TO, "smtp:" + email);
    mapProperties.put(MessageConstants.PROP_SAKAI_BODY, "blank");
    mapProperties.put(MessageConstants.PROP_SAKAI_FROM, user.getId());
    mapProperties.put(MessageConstants.PROP_SAKAI_SUBJECT, localizedSubject);
    mapProperties.put("sakai:category", "message");

    mapProperties.put(MessageConstants.PROP_SAKAI_MESSAGEBOX, "pending");
    mapProperties.put(MessageConstants.PROP_TEMPLATE_PATH, template);

    templateParams.put("sender", user.getId());

    StringBuilder sb = new StringBuilder();

    boolean first = true;

    for (final Entry<String, String> entry : templateParams.entrySet()) {
      if (!first) {
        sb.append("|");
      }
      first = false;

      sb.append(entry.getKey());
      sb.append("=");
      sb.append(entry.getValue());
    }

    mapProperties.put(MessageConstants.PROP_TEMPLATE_PARAMS, sb.toString());

    Content message = getLiteMessagingService().create(session, mapProperties);

    MessageRoutes routes = getLiteMessageRouterManager().getMessageRouting(message);

    for (LiteMessageTransport transport : getTransports().values()) {
      if (transport != null) {
        transport.send(routes, null, message);
      }
    }
  }

  public String getLocaleString(final Map<String, Object[]> parameters,
      final String subjectKey, final SlingRepository slingRepository)
      throws PathNotFoundException, RepositoryException, IOException {
    String locale = null;
    javax.jcr.Session jcrSession = null;
    try {
      jcrSession = slingRepository.loginAdministrative(null);
      locale = getLocaleString(getLocale(parameters), jcrSession, subjectKey);
    } finally {
      if (jcrSession != null) {
        jcrSession.logout();
      }
    }
    return locale;
  }

  protected abstract String getBundleName();

  protected String getLocaleString(Locale locale, javax.jcr.Session session,
      String subjectKey) throws PathNotFoundException, RepositoryException, IOException {
    Node bundlesNode = session.getNode(getBundleName());

    // load the language bundle
    Properties bndLang = getLangBundle(bundlesNode, locale.toString(), false);

    // load the default bundle
    Properties bndLangDefault = getLangBundle(bundlesNode, "default", true);

    if (bndLang != null && bndLang.containsKey(subjectKey)) {
      return bndLang.getProperty(subjectKey);
    } else if (bndLangDefault.containsKey(subjectKey)) {
      return bndLangDefault.getProperty(subjectKey);
    } else {
      String msg = "[MESSAGE KEY NOT FOUND '" + subjectKey + "']";
      getLogger().warn(msg);
      return subjectKey;
    }
  }

  public Locale getLocale(final Map<String, Object[]> params) {
    Locale l = null;
    final String lang = (String) getFirstValueFromArray(PARAM_LANGUAGE, params);

    if (lang != null) {
      String[] parts = lang.split("_");
      l = new Locale(parts[0], parts[1]);
    } else {
      l = (Locale) getFirstValueFromArray("SlingHttpServletRequest.locale", params);
    }

    return l;
  }

  private Properties getLangBundle(Node bundlesNode, String name, boolean throwNotFound)
      throws IOException, PathNotFoundException, RepositoryException {
    Node langNode;
    try {
      langNode = bundlesNode.getNode(name + ".properties");
    } catch (PathNotFoundException e) {
      if (throwNotFound) {
        throw e;
      } else {
        return null;
      }
    } catch (RepositoryException e) {
      if (throwNotFound) {
        throw e;
      } else {
        return null;
      }
    }

    Node content = langNode.getNode("jcr:content");
    Properties props = new Properties();
    InputStream in = content.getProperty("jcr:data").getBinary().getStream();
    props.load(in);
    in.close();
    return props;
  }

  protected String getBaseUrl(SlingHttpServletRequest request) {
    if ((request.getServerPort() == 80) || (request.getServerPort() == 443))
      return request.getScheme() + "://" + request.getServerName()
          + request.getContextPath();
    else
      return request.getScheme() + "://" + request.getServerName() + ":"
          + request.getServerPort() + request.getContextPath();
  }

  protected Logger getLogger() {
    return baseLogger;
  }

  protected abstract LiteMessageRouterManager getLiteMessageRouterManager();

  protected abstract LiteMessagingService getLiteMessagingService();

  protected abstract Map<LiteMessageTransport, LiteMessageTransport> getTransports();

  /**
   * @see ParameterMap#extractParameters(SlingHttpServletRequest)
   * @param key
   * @param params
   * @return null if cannot be found
   */
  protected Object getFirstValueFromArray(final String key,
      final Map<String, Object[]> params) {
    baseLogger.debug(
        "getSingleValue(final String {}, final Map<String, Object[]> params)", key);
    if (key == null || params == null) {
      baseLogger.debug("(key == null || params == null)");
      return null;
    }
    Object value = null;
    if (params.containsKey(key)) {
      final Object[] objArray = params.get(key);
      if (objArray != null && objArray.length > 0) {
        value = objArray[0];
      }
    }
    baseLogger.debug("return {};", value);
    return value;
  }
}
