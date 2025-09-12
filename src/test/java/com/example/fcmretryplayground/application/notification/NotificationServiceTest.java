package com.example.fcmretryplayground.application.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.fcmretryplayground.common.RetryableAlarmException;
import com.example.fcmretryplayground.domain.notification.DeviceFcmToken;
import com.example.fcmretryplayground.domain.notification.DeviceFcmTokenRepository;
import com.example.fcmretryplayground.domain.notification.NotificationLogRepository;
import com.example.fcmretryplayground.domain.user.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration()
class NotificationServiceTest {


    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    @Description("첫 시도 실패 후 재시도 성공")
    void retryable_then_success() throws FirebaseMessagingException {
        //given
        String mockFcmToken = "mockFcmToken";
        DeviceFcmToken mockDeviceFcmToken = getDeviceFcmToken(1L, mockFcmToken);
        Message mockMessage = getMessage(mockFcmToken);

        FirebaseMessaging mockFirebaseMessaging = mock(FirebaseMessaging.class);
        FirebaseMessagingException mockMessagingException = getException(MessagingErrorCode.INTERNAL);

        // when
        try (MockedStatic<FirebaseMessaging> firebaseMessagingMockedStatic = Mockito.mockStatic(
                FirebaseMessaging.class)) {
            firebaseMessagingMockedStatic.when(FirebaseMessaging::getInstance).thenReturn(mockFirebaseMessaging);

            when(mockFirebaseMessaging.send(any(Message.class)))
                    .thenThrow(mockMessagingException)
                    .thenReturn("ok");

            notificationService.sendMessage(mockDeviceFcmToken, mockMessage);

            //then
            verify(mockFirebaseMessaging, times(2)).send(any(Message.class));
        }
    }

    @Test
    @Description("첫 시도, 재시도 모두 실패 후 복구 메서드 수행")
    void retry_exhausted_then_recover() throws FirebaseMessagingException, JsonProcessingException {
        //given
        String mockFcmToken = "mockFcmToken";
        DeviceFcmToken mockDeviceFcmToken = getDeviceFcmToken(1L, mockFcmToken);
        Message mockMessage = getMessage(mockFcmToken);

        FirebaseMessagingException mockMessagingException = getException(MessagingErrorCode.INTERNAL);
        FirebaseMessaging mockFirebaseMessaging = mock(FirebaseMessaging.class);

        //when
        try(MockedStatic<FirebaseMessaging> firebaseMessagingMockedStatic = Mockito.mockStatic(
                FirebaseMessaging.class)) {
            firebaseMessagingMockedStatic.when(FirebaseMessaging::getInstance).thenReturn(mockFirebaseMessaging);

            when(mockFirebaseMessaging.send(any(Message.class)))
                    .thenThrow(mockMessagingException)
                    .thenThrow(mockMessagingException)
                    .thenThrow(mockMessagingException)
                    .thenThrow(mockMessagingException);

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            //then
            notificationService.sendMessage(mockDeviceFcmToken, mockMessage);

            verify(mockFirebaseMessaging, times(4)).send(any(Message.class));
            verify(notificationLogRepository, times(1)).save(any());
        }
    }

    // 유틸 메서드
    private FirebaseMessagingException getException(MessagingErrorCode code) {
        FirebaseMessagingException mockException = Mockito.mock(FirebaseMessagingException.class);
        when(mockException.getMessagingErrorCode()).thenReturn(code);
        return mockException;
    }

    private DeviceFcmToken getDeviceFcmToken(long id, String token) {
        DeviceFcmToken deviceFcmToken = Mockito.mock(DeviceFcmToken.class);
        when(deviceFcmToken.getId()).thenReturn(id);
        when(deviceFcmToken.getFcmToken()).thenReturn(token);
        return deviceFcmToken;
    }

    private Message getMessage(String token) {
        return Message.builder().setToken(token).putAllData(Map.of("k", "v")).build();
    }

    @Configuration
    @EnableRetry
    static class TestConfig {
        @Bean
        public ObjectMapper getMapper() {
            return mock(ObjectMapper.class);
        }

        @Bean
        public NotificationLogRepository getFailNotificationLogRepository() {
            return mock(NotificationLogRepository.class);
        }

        @Bean
        public DeviceFcmTokenRepository getDeviceFcmTokenRepository() {
            return mock(DeviceFcmTokenRepository.class);
        }

        @Bean
        public UserRepository getUserRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        public NotificationService getNotificationService() {
            return new NotificationService(getMapper(), getFailNotificationLogRepository(),
                    getDeviceFcmTokenRepository(), getUserRepository());
        }

    }

    static class TestNotificationService extends NotificationService {
        public TestNotificationService(ObjectMapper mapper,
                                       NotificationLogRepository notificationLogRepository,
                                       DeviceFcmTokenRepository deviceFcmTokenRepository,
                                       UserRepository userRepository) {
            super(mapper, notificationLogRepository, deviceFcmTokenRepository, userRepository);
        }

        @Override
        @org.springframework.retry.annotation.Retryable(
                value = RetryableAlarmException.class,
                maxAttempts = 4,
                backoff = @org.springframework.retry.annotation.Backoff(delay = 10, multiplier = 2.0, maxDelay = 50)
        )
        public void sendMessage(DeviceFcmToken deviceFcmToken, Message message) {
            super.sendMessage(deviceFcmToken, message);
        }

    }
}
