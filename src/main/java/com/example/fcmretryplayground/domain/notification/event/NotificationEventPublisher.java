package com.example.fcmretryplayground.domain.notification.event;

import com.example.fcmretryplayground.domain.notification.handler.NotificationCommand;
import com.example.fcmretryplayground.domain.notification.handler.NotificationType;
import com.example.fcmretryplayground.domain.notification.handler.NotificationHandler;
import com.example.fcmretryplayground.domain.notification.handler.NotificationPayload;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {
    private final ApplicationEventPublisher eventPublisher;
    private final List<NotificationHandler<?>> handlers;

    public void publishNotification(NotificationType type, Object payload) {
        NotificationHandler<?> notificationHandler = handlers.stream()
                .filter(handler -> handler.supports(type, payload))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Not Found Handler type : " + type + ", payload: " + payload));

        @SuppressWarnings("unchecked")
        NotificationHandler<NotificationPayload> typedHandler = (NotificationHandler<NotificationPayload>) notificationHandler;

        NotificationCommand command = typedHandler.handle(type, (NotificationPayload) payload);
        eventPublisher.publishEvent(new NotificationEvent(this, command));
    }
}
