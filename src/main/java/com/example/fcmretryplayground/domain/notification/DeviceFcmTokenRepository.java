package com.example.fcmretryplayground.domain.notification;

import com.example.fcmretryplayground.domain.user.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceFcmTokenRepository extends JpaRepository<DeviceFcmToken, Long> {

    List<DeviceFcmToken> findAllByUserAndStatus(User user, FcmTokenStatus status);
}
