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
package org.sakaiproject.nakamura.resource.lite.servlet.post;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet prevents POST operations against a Principal that does not exist yet.
 * i.e.: KERN-2879 updates to nonexistent users create documents under
 * /system/userManager/user rather than errors.
 */
@Component(immediate = true)
@SlingServlet(methods = "POST", resourceTypes = { "sling/nonexisting" }, generateComponent = false)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Cannot perform POST operations against a non-existing Principal.") })
public class UserGroupManagementOptingServlet extends SlingAllMethodsServlet implements
    OptingServlet {
  private static final long serialVersionUID = -711343928913796879L;
  private static final Logger LOG = LoggerFactory
      .getLogger(UserGroupManagementOptingServlet.class);

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(final SlingHttpServletRequest request,
      final SlingHttpServletResponse response) throws ServletException, IOException {
    LOG.debug("doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)");
    response.sendError(HttpServletResponse.SC_NOT_FOUND,
        "Cannot perform POST operations against a Principal that does not exist!");
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.OptingServlet#accepts(org.apache.sling.api.SlingHttpServletRequest)
   */
  @Override
  public boolean accepts(final SlingHttpServletRequest request) {
    LOG.debug("accepts(SlingHttpServletRequest request)");
    // is this for a non-existing user or group management request?
    final String path = request.getResource().getPath();
    if (path != null
        && (path.contains("/system/userManager/user") || path
            .contains("/system/userManager/group"))) {
      return true;
    }

    // default to returning false as it seems we shouldn't handle this resource.
    return false;
  }

}
