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
package com.rsmart.oae.user.api.lostpwd;

public class InvalidLinkLostPasswordException extends LostPasswordException {

  /**
   * 
   */
  private static final long serialVersionUID = 3096808578406138180L;

  public InvalidLinkLostPasswordException() {
    super();
  }

  public InvalidLinkLostPasswordException(String arg0, String redirectLink) {
    super(arg0, redirectLink);
  }

  public InvalidLinkLostPasswordException(String message, Throwable cause,
      String redirectLink) {
    super(message, cause, redirectLink);
  }

  public InvalidLinkLostPasswordException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidLinkLostPasswordException(String message) {
    super(message);
  }

  public InvalidLinkLostPasswordException(Throwable cause, String redirectLink) {
    super(cause, redirectLink);
  }

  public InvalidLinkLostPasswordException(Throwable cause) {
    super(cause);
  }

}
