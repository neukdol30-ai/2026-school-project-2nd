package com.siyan1234.itproject2nd.mypage.support;

public final class MyPagePolicy {

    public static final String ADMIN_ROLE = "ADMIN";
    public static final String WITHDRAW_CONFIRM_TEXT = "회원탈퇴";

    private static final String PASSWORD_PATTERN = "(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,20}";
    private static final String EMAIL_PATTERN = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
    private static final String PHONE_PATTERN = "^010-[0-9]{4}-[0-9]{4}$";
    private static final String GENDER_MALE = "M";
    private static final String GENDER_FEMALE = "F";

    private MyPagePolicy() {
    }

    public static boolean isAdminRole(String role) {
        return ADMIN_ROLE.equalsIgnoreCase(role);
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.matches(PASSWORD_PATTERN);
    }

    public static boolean isValidEmail(String email) {
        return !isBlank(email) && email.matches(EMAIL_PATTERN);
    }

    public static boolean isValidPhone(String phone) {
        return !isBlank(phone) && phone.matches(PHONE_PATTERN);
    }

    public static boolean isValidGender(String gender) {
        return isBlank(gender)
                || GENDER_MALE.equalsIgnoreCase(gender)
                || GENDER_FEMALE.equalsIgnoreCase(gender);
    }

    public static boolean isWithdrawConfirmText(String confirmText) {
        return WITHDRAW_CONFIRM_TEXT.equals(confirmText);
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
