package com.meetclone.notification.presence;

import org.springframework.web.socket.handler.TextWebSocketHandler;

public class PresenceWebSocketHandler extends TextWebSocketHandler {
    // extends TextWebSocketHandler - pushes presence changes to subscribed clients
}
