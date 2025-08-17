package com.example.fcmretryplayground.domain.notification.handler;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Recipient {
    private final Long id;
    private final String name;
}
