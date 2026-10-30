package com.growth360.util;

import java.util.regex.Pattern;

/**
 * Validation utility for business inputs.
 */
public final class Validator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private Validator() {}

    public static boolean isNotEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }

    public static boolean isValidPositiveDouble(String text) {
        if (!isNotEmpty(text)) return false;
        try {
            double val = Double.parseDouble(text.trim());
            return val > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidNonNegativeDouble(String text) {
        if (!isNotEmpty(text)) return false;
        try {
            double val = Double.parseDouble(text.trim());
            return val >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidPositiveInt(String text) {
        if (!isNotEmpty(text)) return false;
        try {
            int val = Integer.parseInt(text.trim());
            return val > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidNonNegativeInt(String text) {
        if (!isNotEmpty(text)) return false;
        try {
            int val = Integer.parseInt(text.trim());
            return val >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidEmail(String email) {
        if (!isNotEmpty(email)) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
}
