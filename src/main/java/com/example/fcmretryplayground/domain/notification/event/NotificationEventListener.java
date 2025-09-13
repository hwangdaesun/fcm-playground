package com.example.fcmretryplayground.domain.notification.event;

import com.example.fcmretryplayground.application.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async
    @EventListener
    public void execute(NotificationEvent event) {
        notificationService.send(event.getNotificationCommand());
    }

}
