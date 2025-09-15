package com.example.fcmretryplayground.domain.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.Message;
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
    public void recordNotificationLog(RecordNotificationLogCommand command) {
        try {
            NotificationLog notificationLog = notificationLogRepository.findById(command.notificationLogId())
                    .orElseThrow(RuntimeException::new);
            notificationLog.markFail(command.code());
            log.info("Notification Log 기록 성공: {}", notificationLog.getId());
        } catch (Exception e) {
            log.error("Notification Log 기록 실패 : {}", e.getMessage());
        }
    }

    private String serializeMessage(Message message) throws JsonProcessingException {
        return mapper.writeValueAsString(message);
    }
}
