package com.example.fcmretryplayground.domain.notification.handler;

import java.util.List;

public record ExamplePayload(Sender sender, List<Recipient> recipients) implements NotificationPayload {
}
