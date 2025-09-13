package com.example.fcmretryplayground.domain.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationLogService {

    private final NotificationLogRepository notificationLogRepository;
    private final ObjectMapper mapper;

    @SneakyThrows
    @Transactional
    public NotificationLog recordReadyNotificationLog(DeviceFcmToken deviceFcmToken, Message message){
        NotificationLog notificationLog = NotificationLog.record(
                deviceFcmToken.getId(), serializeMessage(message), null, NotificationStatus.READY);
        return notificationLogRepository.save(notificationLog);
    }

    @Transactional
    public void recordNotificationLog(DeviceFcmToken deviceFcmToken, Message message, MessagingErrorCode code) {
        try {
            deviceFcmToken.markInvalid();
            NotificationLog failLog = NotificationLog.record(
                    deviceFcmToken.getId(), serializeMessage(message), code);
            notificationLogRepository.save(failLog);
            log.info("Fail Notification Log 기록 성공: {}", failLog);
        } catch (Exception e) {
            log.error("Fail Notification Log 기록 실패 : {}", e.getMessage());
        }
    }

    private String serializeMessage(Message message) throws JsonProcessingException {
        return mapper.writeValueAsString(message);
    }
}
