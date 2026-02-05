package com.banking.util;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PaymentRefGenerator {

    private static final SecureRandom random = new SecureRandom();
    private static final String PREFIX = "PAY";

    public static String generate() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomNum = 1000 + random.nextInt(9000);
        return PREFIX + timestamp + randomNum;
    }
}
