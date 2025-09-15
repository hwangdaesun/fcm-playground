## 푸시 알림 아키텍처

외부 시스템인 FCM과의 연동은 예측 불가능한 장애가 발생할 수 있어, 데이터의 정합성을 보장하고 높은 시스템 안정성을 유지하는 것이 중요합니다.

안정적인 푸시 알림 전송을 위해 모든 알림의 생명주기를 READY, SUCCESS, FAIL 상태로 추적하는 상태 기반(State-Driven) 아키텍처를 설계했습니다.

일시적인 오류는 Spring Retry(@Retryable, @Recover)를 통해 즉시 복구하고, 놓치거나 장기화된 장애는 복구 스케줄러가 주기적으로 처리하여 최종 일관성(Eventual
Consistency)을 보장합니다. 또한, Timeout을 명시하여 외부 시스템의 응답 지연이 전체 서비스의 장애로 확산되는 것을 차단했습니다.

### 엔티티 설계

- DeviceFcmToken 엔티티
    - 하나의 사용자는 여러 디바이스를 사용할 수 있다.
    - 각 기기별로 푸시 동의 여부가 다를 수 있다.
    - 각 기기와 FcmToken은 1 : 1 매칭이나 FcmToken은 갱신될 수 있다.
- NotificationLog 엔티티
    - MessagingErrorCode를 저장한다. (실패 이유)
    - NotificationLogStatus를 저장한다. (READY, SUCCESS, FAIL)

### Retry 및 Recover 전략

외부 시스템(FCM)과의 통신은 일시적인 네트워크 불안정이나 서버 오류로 인해 실패할 수 있습니다.
이러한 단발성 장애에 즉각적으로 대응하고 알림 성공률을 높이기 위해 Spring Retry 기반의 재시도 및 복구 전략을 도입했습니다.

### 알림 로그 상태별 복구 처리 전략

알림 로그 상태(NotificationLogStatus)에 따른 복구 처리 전략은 다음과 같습니다.

1. SUCCESS 상태 : 복구 처리 제외
    - 알림 발송과 NotificationLog가 SUCCESS로 모두 성공적으로 처리된 상태.
2. FAIL 상태 : 선택적 복구 (MessagingErrorCode)
    - 복구 스케줄러가 FAIL 상태인 알림 중 재시도 가능한 경우에 한해서 선별적으로 재시도 처리.
3. READY 상태 : 전체 복구
    - READY 상태는 알림 발송이 시도되지 않은 상태로, 전체 복구 대상에 포함.
    - 복구 시도 후 전송 실패 시 NotificationStatus Update 수행.

### 복구 스케줄러 운영

- 매일 오전 9시부터 오후 10시까지, 5분에 한 번씩 주기적으로 실행.
- FAIL 상태(재시도 가능 오류)와 READY 상태의 알림을 조회하여 재전송을 트리거함.

### Timeout 설정

- 스레드를 대기 상태로 두지 않도록, 타임 아웃 설정.

### MessagingErrorCode

- `INTERNAL` : FCM 서버 오류 / 일시 장애
- `UNAVAILABLE` : FCM 서비스 일시 불가
- `QUOTA_EXCEEDED` : 전송 한도 초과
- `INVALID_ARGUMENT` : 잘못된 토큰 포맷
- `THIRD_PARTY_AUTH_ERROR` : APNs/인증키 문제
- `SENDER_ID_MISMATCH` : 토큰의 프로젝트와 발신자 불일치
- `UNREGISTERED` : 앱 인스턴스가 FCM에서 등록 해제

## 코드 예시

```java

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
    notificationLogService.recordNotificationLog(notificationLog.getId(), ex.getCode());
}

public void handleSendFailure(DeviceFcmToken deviceFcmToken, Message message, FirebaseMessagingException e) {
    MessagingErrorCode code = e.getMessagingErrorCode();
    switch (code) {
        case INTERNAL, UNAVAILABLE, QUOTA_EXCEEDED -> throw new RetryableAlarmException(code);
        case UNREGISTERED -> {
            deviceFcmToken.markInvalid();
            log.error("Device FCM Token Unregistered: {}", deviceFcmToken.getId());
        }
        case INVALID_ARGUMENT -> {
            deviceFcmToken.markInvalid();
            log.error("Invalid Argument: {}", message);
        }
        case SENDER_ID_MISMATCH -> {

            log.error("Sender Id Mismatch");
        } // Firebase 프로젝트가 다를 경우 ex) 개발용 서버의 토큰을 사용해 운영 서버에 알림을 보내려고 할 때
        case THIRD_PARTY_AUTH_ERROR -> log.error("Third Party Auth Error");
    }
}

```
