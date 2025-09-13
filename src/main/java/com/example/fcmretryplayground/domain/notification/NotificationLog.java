package com.example.fcmretryplayground.domain.notification;

import com.google.firebase.messaging.MessagingErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(name = "NOTIFICATION_LOG")
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NOTIFICATION_LOG_ID", nullable = false)
    private Long id;

    @Column(name = "USER_NOTIFICATION_TOKEN_ID", nullable = false)
    private Long userNotificationTokenId;

    @Column(name = "MESSAGE")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "ERROR_CODE")
    private MessagingErrorCode errorCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "NOTIFICATION_STATUS")
    private NotificationStatus notificationStatus;

    @Column(name = "COUNT")
    private Integer count;

    @Builder(access = AccessLevel.PRIVATE)
    private NotificationLog(
            Long userNotificationTokenId, String message, MessagingErrorCode errorCode,
            NotificationStatus notificationStatus, Integer count) {
        this.userNotificationTokenId = userNotificationTokenId;
        this.message = message;
        this.errorCode = errorCode;
        this.notificationStatus = notificationStatus;
        this.count = count;
    }

    public static NotificationLog record(Long userNotificationTokenId, String message, MessagingErrorCode errorCode, NotificationStatus notificationStatus) {
        return NotificationLog.builder()
                .userNotificationTokenId(userNotificationTokenId)
                .message(message)
                .errorCode(errorCode)
                .notificationStatus(notificationStatus)
                .count(1)
                .build();
    }

    public void markSuccess() {
        this.notificationStatus = NotificationStatus.SUCCESS;
    }

    public void markFail() {
        this.notificationStatus = NotificationStatus.FAIL;
        this.count++;
    }
}
