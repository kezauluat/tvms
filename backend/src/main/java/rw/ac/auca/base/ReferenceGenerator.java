package rw.ac.auca.base;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Human-readable references printed on tickets and payment receipts. */
public final class ReferenceGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O or 1/I confusion
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyMMdd");

    private ReferenceGenerator() {}

    /** Example: TV261011-K7Q2M */
    public static String violationReference(LocalDate date) {
        return "TV" + date.format(DATE) + "-" + random(5);
    }

    /** Example: MOMO-8KD2PQ7X4M */
    public static String paymentReference() {
        return "MOMO-" + random(10);
    }

    private static String random(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
