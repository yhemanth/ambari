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

package org.apache.ambari.server.controller;

import org.apache.ambari.server.alerts.AlertPublisherWebSocket;
import org.apache.ambari.server.notifications.DispatchFactory;
import org.apache.ambari.server.notifications.Notification;
import org.apache.ambari.server.notifications.dispatchers.NotificationWebSocketDispatcher;
import org.apache.ambari.server.state.alert.TargetType;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AlertPublisherWebSocketServlet extends WebSocketServlet implements WebSocketCloseListener {
  private static final Logger LOG = LoggerFactory.getLogger(AlertPublisherWebSocketServlet.class);

  private List<AlertPublisherWebSocket> connectedSockets = new ArrayList<>();

  public AlertPublisherWebSocketServlet() {
    LOG.warn("initializing websocket publisher servlet");
    NotificationWebSocketDispatcher dispatcher = (NotificationWebSocketDispatcher) DispatchFactory.getInstance().
        getDispatcher(TargetType.WEB_SOCKET.name());
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    executor.scheduleAtFixedRate(new NotificationSender(dispatcher), 10, 10, TimeUnit.SECONDS);
  }

  @Override
  public String getInitParameter(String name) {
    if ("maxIdleTime".equals(name)) {
      return String.valueOf(Integer.MAX_VALUE);
    }
    return super.getInitParameter(name);
  }

  @Override
  public WebSocket doWebSocketConnect(HttpServletRequest httpServletRequest, String origin) {
    LOG.warn("Received a connect request from: " + origin);
    AlertPublisherWebSocket alertPublisherWebSocket = new AlertPublisherWebSocket(this);
    connectedSockets.add(alertPublisherWebSocket);
    return alertPublisherWebSocket;
  }

  @Override
  public void socketClosed(AlertPublisherWebSocket socket) {
    connectedSockets.remove(socket);
  }

  private class NotificationSender implements Runnable {

    private NotificationWebSocketDispatcher dispatcher;

    public NotificationSender(NotificationWebSocketDispatcher dispatcher) {
      this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
      for (AlertPublisherWebSocket webSocket : connectedSockets) {
        List<Notification> notifications = dispatcher.getNotifications();
        try {
          if (notifications.size() > 0) {
            for (Notification n : notifications) {
              StringBuilder stringBuilder = new StringBuilder();
              String notificationText = stringBuilder.append(n.Subject).append(": ").append(n.Body).toString();
              webSocket.sendMessage(notificationText);
            }
          } else {
            LOG.warn("No notifications as of now.");
          }
        } catch (IOException e) {
          LOG.error("Could not send status", e);
        }
      }
    }
  }
}
