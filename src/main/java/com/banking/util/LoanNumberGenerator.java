package com.banking.util;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoanNumberGenerator {

    private static final SecureRandom random = new SecureRandom();
    private static final String PREFIX = "LOAN";

    public static String generate() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomNum = 10000 + random.nextInt(90000);
        return PREFIX + timestamp + randomNum;
    }
}
