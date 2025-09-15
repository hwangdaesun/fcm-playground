package com.example.fcmretryplayground.domain.notification;

import com.google.firebase.messaging.MessagingErrorCode;

public record RecordNotificationLogCommand(Long notificationLogId, MessagingErrorCode code) {
}
