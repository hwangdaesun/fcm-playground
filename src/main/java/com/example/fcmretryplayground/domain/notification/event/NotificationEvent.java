package com.example.fcmretryplayground.domain.notification.event;

import com.example.fcmretryplayground.domain.notification.handler.NotificationCommand;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NotificationEvent extends ApplicationEvent {
    private final NotificationCommand notificationCommand;

    public NotificationEvent(Object source, NotificationCommand notificationCommand) {
        super(source);
        this.notificationCommand = notificationCommand;
    }
}
