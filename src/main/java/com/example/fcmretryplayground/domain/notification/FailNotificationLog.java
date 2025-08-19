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
@Table(name = "FAIL_ALARM_LOG")
public class FailNotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FAIL_ALARM_LOG_ID", nullable = false)
    private Long id;

    @Column(name = "USER_ALARM_TOKEN_ID", nullable = false)
    private Long userAlarmTokenId;

    @Column(name = "MESSAGE")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "ERROR_CODE")
    private MessagingErrorCode errorCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "FAIL_NOTIFICATION_STATUS")
    private FailNotificationStatus failNotificationStatus;

    @Column(name = "COUNT")
    private Integer count;

    @Builder(access = AccessLevel.PRIVATE)
    private FailNotificationLog(
            Long userAlarmTokenId, String message, MessagingErrorCode errorCode,
            FailNotificationStatus failNotificationStatus, Integer count) {
        this.userAlarmTokenId = userAlarmTokenId;
        this.message = message;
        this.errorCode = errorCode;
        this.failNotificationStatus = failNotificationStatus;
        this.count = count;
    }

    public static FailNotificationLog record(Long userAlarmTokenId, String message, MessagingErrorCode errorCode) {
        return FailNotificationLog.builder()
                .userAlarmTokenId(userAlarmTokenId)
                .message(message)
                .errorCode(errorCode)
                .failNotificationStatus(FailNotificationStatus.FAIL)
                .count(1)
                .build();
    }

    public void markSuccess() {
        this.failNotificationStatus = FailNotificationStatus.SUCCESS;
    }

    public void markFail() {
        this.failNotificationStatus = FailNotificationStatus.FAIL;
        this.count++;
    }
}
