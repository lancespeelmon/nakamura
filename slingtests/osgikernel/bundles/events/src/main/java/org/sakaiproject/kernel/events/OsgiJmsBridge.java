/*
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
package org.sakaiproject.kernel.events;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.MessageProducer;
import javax.jms.Session;

/**
 * Bridge to pipe OSGi events into a JMS topic.
 * 
 * @scr.component label="%bridge.name" description="%bridge.description"
 * @scr.service
 */
public class OsgiJmsBridge implements EventHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(OsgiJmsBridge.class);

  /** @scr.property value="*" private="true" */
  static final String TOPICS = EventConstants.EVENT_TOPIC;

  /** @scr.property value="tcp://localhost:61616" */
  static final String BROKER_URL = "bridge.brokerUrl";

  /** @scr.property value="sakai.event.bridge" */
  static final String CONNECTION_CLIENT_ID = "bridge.connectionClientId";

  /** @scr.property value="sakai.event.bridge" */
  static final String EVENT_JMS_TOPIC = "bridge.eventJmsTopic";

  /** @scr.property value="false" options true="true" false="false" */
  static final String SESSION_TRANSACTED = "bridge.sessionTransacted";

  /**
   * @scr.property valueRef="javax.jms.Session.AUTO_ACKNOWLEDGE" options
   *               1="Auto Acknowledge" 2="Client Acknowledge"
   *               3="Lazy Acknowledge"
   */
  static final String ACKNOWLEDGE_MODE = "bridge.acknowledgeMode";

  private ConnectionFactory connFactory;
  private String brokerUrl;
  private String eventTopicName;
  private boolean transacted;
  private String connectionClientId;
  private int acknowledgeMode;

  @SuppressWarnings("unchecked")
  protected void activate(ComponentContext ctx) {
    Dictionary props = ctx.getProperties();

    eventTopicName = (String) props.get(EVENT_JMS_TOPIC);
    transacted = Boolean.parseBoolean((String) props.get(SESSION_TRANSACTED));
    acknowledgeMode = (Integer) props.get(ACKNOWLEDGE_MODE);
    connectionClientId = (String) props.get(CONNECTION_CLIENT_ID);

    String _brokerUrl = (String) props.get(BROKER_URL);
    if (diff(brokerUrl, _brokerUrl)) {
      brokerUrl = _brokerUrl;
      LOGGER.info("Creating a new ActiveMQ Connection Factory");
      //connFactory = new ActiveMQConnectionFactory(brokerUrl);
    }
  }

  public void handleEvent(Event event) {
    LOGGER.debug("Receiving event {}", event);
	/*
    Connection conn = null;
    try {
      // post to JMS
      conn = connFactory.createConnection();
      conn.setClientID(connectionClientId);
      Session clientSession = conn.createSession(transacted, acknowledgeMode);
      Destination emailTopic = clientSession.createTopic(eventTopicName);
      MessageProducer client = clientSession.createProducer(emailTopic);
      MapMessage msg = clientSession.createMapMessage();
      msg.setJMSType(event.getTopic());
      for (String name : event.getPropertyNames()) {
        Object obj = event.getProperty(name);
        msg.setObject(name, obj);
      }
      client.send(msg);
    } catch (JMSException e) {
      LOGGER.error(e.getMessage(), e);
    } finally {
      if (conn != null) {
        try {
          conn.close();
        } catch (JMSException e) {
          LOGGER.warn(e.getMessage(), e);
        }
      }
    }
	*/
  }

  private boolean diff(Object obj1, Object obj2) {
    boolean diff = true;

    boolean bothNull = obj1 == null && obj2 == null;
    boolean neitherNull = obj1 != null && obj2 != null;

    if (bothNull || (neitherNull && obj1.equals(obj2))) {
      diff = false;
    }

    return diff;
  }
}
