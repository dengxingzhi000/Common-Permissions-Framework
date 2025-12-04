package com.frog.system.notification;

/**
 * 通知服务接口（邮件/站内信等）
 */
public interface NotificationService {
    void sendEmail(String to, String subject, String body);

    void sendSystemMessage(String username, String message);
}

