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

public class LostPasswordException extends RuntimeException {

  private static final long serialVersionUID = 6804827709793216858L;

  private String redirectLink;

  public LostPasswordException() {
    super();
  }

  public LostPasswordException(String message, Throwable cause) {
    super(message, cause);
  }

  public LostPasswordException(String message) {
    super(message);
  }

  public LostPasswordException(Throwable cause) {
    super(cause);
  }

  public LostPasswordException(String message, Throwable cause, String redirectLink) {
    super(message, cause);
    this.redirectLink = redirectLink;
  }

  public LostPasswordException(Throwable cause, String redirectLink) {
    super(cause);
    this.redirectLink = redirectLink;
  }

  public LostPasswordException(String arg0, String redirectLink) {
    super(arg0);
    this.redirectLink = redirectLink;
  }

  public String getRedirectLink() {
    return redirectLink;
  }

  public void setRedirectLink(String redirectLink) {
    this.redirectLink = redirectLink;
  }

}
