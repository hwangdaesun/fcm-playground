package com.example.fcmretryplayground.domain.notification;

import com.example.fcmretryplayground.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "DEVICE_FCM_TOKEN")
@Getter
@NoArgsConstructor
public class DeviceFcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    private String deviceId;

    // 푸시 알림 동의 여부
    @Column(nullable = false)
    private Boolean notificationOptIn;

    @Column(nullable = false)
    private String fcmToken;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private FcmTokenStatus status;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private DeviceType deviceType;

    @Builder(access = AccessLevel.PRIVATE)
    private DeviceFcmToken(User user, String deviceId, Boolean notificationOptIn, String fcmToken,
                           FcmTokenStatus status, DeviceType deviceType) {
        this.user = user;
        this.deviceId = deviceId;
        this.notificationOptIn = notificationOptIn;
        this.fcmToken = fcmToken;
        this.status = status;
        this.deviceType = deviceType;
    }

    public static DeviceFcmToken create(User user, Boolean notificationOptIn, String fcmToken,
                                        FcmTokenStatus status, DeviceType deviceType) {
        return DeviceFcmToken.builder()
                .user(user)
                .deviceId(UUID.randomUUID().toString())
                .notificationOptIn(notificationOptIn)
                .fcmToken(fcmToken)
                .status(status)
                .deviceType(deviceType)
                .build();
    }

    public void markInvalid() {
        this.status = FcmTokenStatus.INVALID;
    }

}
