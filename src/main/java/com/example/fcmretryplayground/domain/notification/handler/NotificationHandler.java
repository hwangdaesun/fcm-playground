package com.example.fcmretryplayground.domain.notification.handler;

public interface NotificationHandler<P extends NotificationPayload> {
    boolean supports(NotificationType type, Object payload);
    NotificationCommand handle(NotificationType type, P payload);
}
