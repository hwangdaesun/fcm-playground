package com.example.fcmretryplayground.domain.notification.handler;

import com.example.fcmretryplayground.domain.notification.handler.NotificationType;
import com.example.fcmretryplayground.domain.notification.handler.Recipient;
import com.example.fcmretryplayground.domain.notification.handler.Sender;
import java.util.List;

public record NotificationCommand(
        Sender sender,
        List<Recipient> recipients,
        NotificationType type
) {
}
