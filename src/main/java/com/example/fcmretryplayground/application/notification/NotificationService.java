package com.example.fcmretryplayground.application.notification;

import com.example.fcmretryplayground.common.RetryableAlarmException;
import com.example.fcmretryplayground.domain.notification.DeviceFcmToken;
import com.example.fcmretryplayground.domain.notification.DeviceFcmTokenRepository;
import com.example.fcmretryplayground.domain.notification.FcmTokenStatus;
import com.example.fcmretryplayground.domain.notification.NotificationLog;
import com.example.fcmretryplayground.domain.notification.NotificationLogService;
import com.example.fcmretryplayground.domain.notification.handler.NotificationCommand;
import com.example.fcmretryplayground.domain.notification.handler.Recipient;
import com.example.fcmretryplayground.domain.user.User;
import com.example.fcmretryplayground.domain.user.UserRepository;
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

    private final NotificationLogService notificationLogService;
    private final DeviceFcmTokenRepository deviceFcmTokenRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(NotificationCommand command) {
        List<User> users = userRepository.findAllById(
                command.recipients().stream().map(Recipient::getId).toList()
        );

        users.stream()
                .flatMap(user -> deviceFcmTokenRepository
                        .findAllByUserAndStatus(user, FcmTokenStatus.ACTIVE)
                        .stream())
                .forEach(deviceFcmToken -> {
                    Message message = buildMessage(
                            deviceFcmToken.getFcmToken(),
                            command.type().getTitle(),
                            command.type().getMessage()
                    );
                    NotificationLog notificationLog = notificationLogService.recordReadyNotificationLog(deviceFcmToken,
                            message);
                    sendMessage(deviceFcmToken, message, notificationLog);
                });
    }

    @Retryable(
            retryFor = RetryableAlarmException.class,
            maxAttempts = 2,
            backoff = @Backoff(delay = 5000, multiplier = 2.0)
    )
    public void sendMessage(DeviceFcmToken deviceFcmToken, Message message, NotificationLog notificationLog) {
        try {
            String response = FirebaseMessaging.getInstance().send(message);
            notificationLog.markSuccess();
            log.info("Send Notification Success: {}", response);
        } catch (FirebaseMessagingException e) {
            notificationLog.markFail(e.getMessagingErrorCode());
            handleSendFailure(deviceFcmToken, message, e);
        }
    }

    @Recover
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recover(
            RetryableAlarmException ex,
            DeviceFcmToken deviceFcmToken, Message message, NotificationLog notificationLog) {
        notificationLogService.recordNotificationLog(
                new com.example.fcmretryplayground.domain.notification.RecordNotificationLogCommand(
                        notificationLog.getId(), ex.getCode()
                )
        );
    }

    public void handleSendFailure(DeviceFcmToken deviceFcmToken, Message message, FirebaseMessagingException e) {
        MessagingErrorCode code = e.getMessagingErrorCode();
        switch (code) {
            case INTERNAL, UNAVAILABLE, QUOTA_EXCEEDED -> throw new RetryableAlarmException(code);
            case UNREGISTERED -> {
                deviceFcmToken.markInvalid();
                log.error("Device FCM Token Unregistered: {}", deviceFcmToken.getId());
            }
            case INVALID_ARGUMENT ->{
                deviceFcmToken.markInvalid();
                log.error("Invalid Argument: {}", message);
            }
            case SENDER_ID_MISMATCH ->{

                log.error("Sender Id Mismatch");
            } // Firebase 프로젝트가 다를 경우 ex) 개발용 서버의 토큰을 사용해 운영 서버에 알림을 보내려고 할 때
            case THIRD_PARTY_AUTH_ERROR -> log.error("Third Party Auth Error");
        }
    }

    private Message buildMessage(String fcmToken, String title, String body) {
        return Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .build();
    }
}
