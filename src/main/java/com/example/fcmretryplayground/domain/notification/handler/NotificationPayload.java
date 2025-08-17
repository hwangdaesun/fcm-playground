package com.example.fcmretryplayground.domain.notification.handler;

import java.util.List;

public interface NotificationPayload {
    Sender sender();
    List<Recipient> recipients();
}
