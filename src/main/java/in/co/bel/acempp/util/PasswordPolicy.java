package in.co.bel.acempp.util;

import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared password generation and strength rules.
 * Standard: min 8 chars, first uppercase, at least one lowercase, digit, and special (@#$%^&+=!).
 * Default / reset temp: CapitalizedUserId@ddmmyyyy
 */
public final class PasswordPolicy {

    public static final int HISTORY_LIMIT = 3;
    public static final String SPECIAL_CHARS = "@#$%^&+=!";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("ddMMyyyy");

    private PasswordPolicy() {}

    public static String generateDefaultPassword(String userId) {
        return generateDefaultPassword(userId, LocalDate.now());
    }

    public static String generateDefaultPassword(String userId, LocalDate date) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("UserId cannot be empty");
        }
        String cleanUserId = userId.trim();
        int maxUserIdLength = 11;
        if (cleanUserId.length() > maxUserIdLength) {
            cleanUserId = cleanUserId.substring(0, maxUserIdLength);
        }

        String base = cleanUserId.toLowerCase();
        String formatted = Character.toUpperCase(base.charAt(0)) + base.substring(1);

        return formatted + "@" + date.format(DATE_FMT);
    }

    public static void validateStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "Password must be at least 8 characters long");
        }
        if (!Character.isUpperCase(password.charAt(0))) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "Password must start with an uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "Password must contain at least one lowercase letter");
        }
        if (!password.matches(".*\\d.*")) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "Password must contain at least one digit");
        }
        if (!password.matches(".*[@#$%^&+=!].*")) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "Password must contain at least one special character (@#$%^&+=!)");
        }
    }

    /**
     * Push current hash into history (max {@link #HISTORY_LIMIT}), return updated list.
     */
    public static List<String> pushHistory(List<String> existing, String currentHash) {
        List<String> history = existing != null ? new ArrayList<>(existing) : new ArrayList<>();
        if (currentHash != null && !currentHash.isBlank()) {
            history.add(0, currentHash);
        }
        if (history.size() > HISTORY_LIMIT) {
            return new ArrayList<>(history.subList(0, HISTORY_LIMIT));
        }
        return history;
    }

    public static boolean matchesAnyHistory(PasswordEncoder encoder, String rawPassword,
                                            String currentHash, List<String> history) {
        if (currentHash != null && encoder.matches(rawPassword, currentHash)) {
            return true;
        }
        if (history == null) {
            return false;
        }
        for (String hash : history) {
            if (hash != null && encoder.matches(rawPassword, hash)) {
                return true;
            }
        }
        return false;
    }
}
