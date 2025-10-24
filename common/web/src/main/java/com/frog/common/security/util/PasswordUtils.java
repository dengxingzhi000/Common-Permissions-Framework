package com.frog.common.security.util;

import java.security.SecureRandom;
import java.util.regex.Pattern;

/**
 * 密码工具类
 *
 * @author Deng
 * createData 2025/10/15 14:41
 * @version 1.0
 */
public class PasswordUtils {
    private static final String UPPER_CASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER_CASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL_CHARS = "@#$%^&+=!";

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$");

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 生成随机密码
     */
    public static String generateRandomPassword(int length) {
        if (length < 8) {
            length = 8;
        }

        StringBuilder password = new StringBuilder(length);
        String allChars = UPPER_CASE + LOWER_CASE + DIGITS + SPECIAL_CHARS;

        // 确保至少包含每种类型的字符
        password.append(UPPER_CASE.charAt(RANDOM.nextInt(UPPER_CASE.length())));
        password.append(LOWER_CASE.charAt(RANDOM.nextInt(LOWER_CASE.length())));
        password.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        password.append(SPECIAL_CHARS.charAt(RANDOM.nextInt(SPECIAL_CHARS.length())));

        // 填充剩余长度
        for (int i = 4; i < length; i++) {
            password.append(allChars.charAt(RANDOM.nextInt(allChars.length())));
        }

        // 打乱顺序
        return shuffleString(password.toString());
    }

    /**
     * 验证密码强度
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * 获取密码强度等级 (0-4)
     * 0: 很弱
     * 1: 弱
     * 2: 中等
     * 3: 强
     * 4: 很强
     */
    public static int getPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }

        int strength = 0;

        // 长度检查
        if (password.length() >= 8) strength++;
        if (password.length() >= 12) strength++;

        // 包含小写字母
        if (password.matches(".*[a-z].*")) strength++;

        // 包含大写字母
        if (password.matches(".*[A-Z].*")) strength++;

        // 包含数字
        if (password.matches(".*\\d.*")) strength++;

        // 包含特殊字符
        if (password.matches(".*[@#$%^&+=!].*")) strength++;

        return Math.min(strength / 2, 4);
    }

    private static String shuffleString(String string) {
        char[] chars = string.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }
}

