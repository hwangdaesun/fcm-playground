package com.example.fcmretryplayground.common;

import com.google.firebase.messaging.MessagingErrorCode;
import lombok.Getter;

@Getter
public class RetryableAlarmException extends RuntimeException {

    private final MessagingErrorCode code;

    public RetryableAlarmException(MessagingErrorCode code) {
        this.code = code;
    }
}

