package com.example.fcmretryplayground;

import com.example.fcmretryplayground.domain.notification.DeviceFcmToken;
import com.example.fcmretryplayground.domain.notification.DeviceFcmTokenRepository;
import com.example.fcmretryplayground.domain.notification.FcmTokenStatus;
import com.example.fcmretryplayground.domain.user.User;
import com.example.fcmretryplayground.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SetupMockData {
    private final UserRepository userRepository;
    private final DeviceFcmTokenRepository deviceFcmTokenRepository;

    @Transactional
    public void execute() {
        User user = userRepository.save(User.create("sonny123@test.com"));
        deviceFcmTokenRepository.save(DeviceFcmToken.create(user, true, "mockFcmToken", FcmTokenStatus.ACTIVE));
    }

}
