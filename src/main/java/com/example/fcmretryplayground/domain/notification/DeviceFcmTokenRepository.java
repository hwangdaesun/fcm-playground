package com.example.fcmretryplayground.domain.notification;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceFcmTokenRepository extends JpaRepository<DeviceFcmToken, Long> {
}
