/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.notifications.dispatchers;

import org.apache.ambari.server.notifications.Notification;
import org.apache.ambari.server.notifications.NotificationDispatcher;
import org.apache.ambari.server.notifications.TargetConfigurationResult;
import org.apache.ambari.server.state.alert.TargetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NotificationWebSocketDispatcher implements NotificationDispatcher {

  private static final Logger LOG = LoggerFactory.getLogger(NotificationWebSocketDispatcher.class);
  private List<Notification> notifications = new ArrayList<>();

  @Override
  public String getType() {
    return TargetType.WEB_SOCKET.name();
  }

  @Override
  public void dispatch(Notification notification) {
    LOG.warn("Received notification: " + notification.toString());
    synchronized (notifications) {
      notifications.add(notification);
    }
  }

  @Override
  public boolean isDigestSupported() {
    return false;
  }

  @Override
  public TargetConfigurationResult validateTargetConfig(Map<String, Object> properties) {
    LOG.warn("Returning valid response for validation of target config");
    return TargetConfigurationResult.valid();
  }

  @Override
  public boolean isNotificationContentGenerationRequired() {
    return true;
  }

  public List<Notification> getNotifications() {
    ArrayList<Notification> unsentNotifications = new ArrayList<>();
    synchronized (notifications) {
      if (notifications.size() != 0) {
        unsentNotifications.addAll(notifications);
        notifications.clear();
      }
    }
    return unsentNotifications;
  }
}
