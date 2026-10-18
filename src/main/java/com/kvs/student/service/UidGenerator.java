package com.kvs.student.service;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

public class UidGenerator {

    private static final AtomicInteger COUNTER = new AtomicInteger(1000);

    public static String generateStudentUid() {
        int year = LocalDate.now().getYear();
        return String.format("STU-%d-%05d", year, COUNTER.incrementAndGet());
    }

    public static String generateCertificateNumber(String prefix) {
        int year = LocalDate.now().getYear();
        return String.format("%s-%d-%05d", prefix, year, COUNTER.incrementAndGet());
    }
}
