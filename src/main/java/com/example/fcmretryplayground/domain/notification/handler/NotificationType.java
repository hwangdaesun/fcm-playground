package com.example.fcmretryplayground.domain.notification.handler;

public enum NotificationType {
    EXAMPLE_ALARM("제목 예시", "메시지 예시");

    private final String title;
    private final String message;

    NotificationType(String title, String message) {
        this.title = title;
        this.message = message;
    }
}
