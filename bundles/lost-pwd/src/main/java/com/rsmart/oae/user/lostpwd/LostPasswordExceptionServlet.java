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
package com.rsmart.oae.user.lostpwd;

import com.rsmart.oae.user.api.emailverify.util.ExceptionServletBase;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.servlets.ServletResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SlingServlet(methods = { "LostPasswordException" }, resourceTypes = { "sling/servlet/errorhandler" })
public class LostPasswordExceptionServlet extends ExceptionServletBase {

  @Reference
  protected ServletResolver resolver;

  private static final long serialVersionUID = 3716780477529017681L;

  private static final Logger LOGGER = LoggerFactory
      .getLogger(LostPasswordExceptionServlet.class);

  protected Logger getLogger() {
    return LOGGER;
  }

  protected String getBasePath() {
    return "/lostpwd/sling/servlet/errorhandler/";
  }

  @Override
  protected ServletResolver getResolver() {
    return resolver;
  }

}
