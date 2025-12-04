package com.frog.common.security.util;

/**
 * 数据脱敏工具类
 *
 * @author Deng
 * createData 2025/10/15 14:42
 * @version 1.0
 */
public class DesensitizeUtils {

    /**
     * 手机号脱敏
     * 138****1234
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 身份证脱敏
     * 110101********1234
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 18) {
            return idCard;
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(14);
    }

    /**
     * 邮箱脱敏
     * abc****@example.com
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String username = parts[0];
        if (username.length() <= 3) {
            return email;
        }
        return username.substring(0, 3) + "****@" + parts[1];
    }

    /**
     * 银行卡脱敏
     * 6222 **** **** 1234
     */
    public static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 16) {
            return bankCard;
        }
        return bankCard.substring(0, 4) + " **** **** " + bankCard.substring(bankCard.length() - 4);
    }

    /**
     * 姓名脱敏
     * 张**
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() == 1) {
            return "*";
        }
        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }
        return name.charAt(0) + "**";
    }

    /**
     * 地址脱敏
     * 保留省市，详细地址脱敏
     */
    public static String maskAddress(String address) {
        if (address == null || address.length() <= 6) {
            return address;
        }
        return address.substring(0, 6) + "****";
    }

    /**
     * 敏感信息脱敏
     */
    public static String desensitize(String content) {
        if (content == null) return null;

        // 脱敏密码字段
        content = content.replaceAll("(\"password\"\\s*:\\s*\")([^\"]+)(\")", "$1******$3");
        // 脱敏身份证
        content = content.replaceAll("(\"idCard\"\\s*:\\s*\")([^\"]+)(\")", "$1****$3");
        // 脱敏手机号
        content = content.replaceAll("(\"phone\"\\s*:\\s*\")([^\"]+)(\")", "$1****$3");

        return content;
    }
}

