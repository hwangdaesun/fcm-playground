package com.example.fcmretryplayground.application.notification;

import com.example.fcmretryplayground.common.RetryableAlarmException;
import com.example.fcmretryplayground.domain.notification.DeviceFcmToken;
import com.example.fcmretryplayground.domain.notification.DeviceFcmTokenRepository;
import com.example.fcmretryplayground.domain.notification.FcmTokenStatus;
import com.example.fcmretryplayground.domain.notification.NotificationLog;
import com.example.fcmretryplayground.domain.notification.NotificationLogRepository;
import com.example.fcmretryplayground.domain.notification.handler.NotificationCommand;
import com.example.fcmretryplayground.domain.notification.handler.Recipient;
import com.example.fcmretryplayground.domain.user.User;
import com.example.fcmretryplayground.domain.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final ObjectMapper mapper;
    private final NotificationLogRepository notificationLogRepository;
    private final DeviceFcmTokenRepository deviceFcmTokenRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(NotificationCommand command) {
        List<Long> recipientIds = command.recipients().stream().map(Recipient::getId).toList();
        List<User> users = userRepository.findAllById(recipientIds);
        for (User user : users) {
            List<DeviceFcmToken> activeTokens = deviceFcmTokenRepository.findAllByUserAndStatus(user,
                    FcmTokenStatus.ACTIVE);
            for (DeviceFcmToken deviceFcmToken : activeTokens) {
                Message message = buildMessage(deviceFcmToken.getFcmToken(), command.type().getTitle(),
                        command.type().getMessage());
                sendMessage(deviceFcmToken, message);
            }
        }
    }

    @Retryable(
            value = RetryableAlarmException.class,
            maxAttempts = 4,
            backoff = @Backoff(delay = 5000, multiplier = 2.0)
    )
    public void sendMessage(DeviceFcmToken deviceFcmToken, Message message) {
        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Send Notification Success: {}", response);
        } catch (FirebaseMessagingException e) {
            MessagingErrorCode code = e.getMessagingErrorCode();
            switch (code) {
                case INTERNAL, UNAVAILABLE, QUOTA_EXCEEDED -> {
                    throw new RetryableAlarmException(code);
                }
                case UNREGISTERED -> {
                    recordNotificationLog(deviceFcmToken, message, code);
                }
                default -> recordNotificationLog(deviceFcmToken, message, code);
            }
            log.error("메시지 전송 실패 : {}", e.getMessagingErrorCode().name());
        }
    }

    @Recover
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recover(
            RetryableAlarmException ex,
            DeviceFcmToken deviceFcmToken, Message message) {
        recordNotificationLog(deviceFcmToken, message, ex.getCode());
    }

    private void recordNotificationLog(DeviceFcmToken deviceFcmToken, Message message, MessagingErrorCode code) {
        try {
            deviceFcmToken.markInvalid();
            NotificationLog failLog = NotificationLog.record(
                    deviceFcmToken.getId(), mapper.writeValueAsString(message), code);
            notificationLogRepository.save(failLog);
            log.info("Fail Notification Log 기록 성공: {}", failLog);
        } catch (Exception e) {
            log.error("Fail Notification Log 기록 실패 : {}", e.getMessage());
        }
    }

    private Message buildMessage(String fcmToken, String title, String body) {
        return Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .build();
    }
}
