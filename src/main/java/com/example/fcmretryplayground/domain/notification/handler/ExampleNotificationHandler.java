package com.example.fcmretryplayground.domain.notification.handler;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ExampleNotificationHandler implements NotificationHandler<ExamplePayload> {

    private final List<NotificationType> types = List.of(NotificationType.EXAMPLE_ALARM);

    @Override
    public boolean supports(NotificationType type, Object payload) {
        if (types.contains(type) && payload instanceof ExamplePayload) {
            return true;
        }
        return false;
    }

    @Override
    public NotificationCommand handle(NotificationType type, ExamplePayload payload) {
        return new NotificationCommand(payload.sender(), payload.recipients(), type);
    }
}
