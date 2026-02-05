package com.banking.util;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TransactionRefGenerator {

    private static final SecureRandom random = new SecureRandom();
    private static final String PREFIX = "TXN";

    public static String generate() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        int randomNum = 100 + random.nextInt(900);
        return PREFIX + timestamp + randomNum;
    }
}
