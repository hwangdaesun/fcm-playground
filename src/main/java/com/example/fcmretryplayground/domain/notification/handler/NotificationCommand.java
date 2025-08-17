package com.example.fcmretryplayground.domain.notification.handler;

import java.util.List;

public record NotificationCommand(
        Sender sender,
        List<Recipient> recipients,
        NotificationType type
) {
}
