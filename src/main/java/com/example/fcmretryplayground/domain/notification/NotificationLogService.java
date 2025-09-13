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
    public void recordNotificationLog(Long notificationLogId, MessagingErrorCode code) {
        try {
            NotificationLog notificationLog = notificationLogRepository.findById(notificationLogId)
                    .orElseThrow(RuntimeException::new);
            notificationLog.markFail(code);
            log.info("Notification Log 기록 성공: {}", notificationLog.getId());
        } catch (Exception e) {
            log.error("Notification Log 기록 실패 : {}", e.getMessage());
        }
    }

    private String serializeMessage(Message message) throws JsonProcessingException {
        return mapper.writeValueAsString(message);
    }
}
