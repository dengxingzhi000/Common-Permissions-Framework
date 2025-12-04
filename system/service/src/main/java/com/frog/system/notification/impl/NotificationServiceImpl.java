package com.frog.system.notification.impl;

import com.frog.system.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_RETRIES = 3;

    @Override
    public void sendEmail(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            log.debug("Skip email: empty recipient, subject={}", subject);
            return;
        }
        runWithRetry(() -> {
            // TODO: integrate actual email client
            log.info("[Email] to={}, subject={}, body={}", to, subject, body);
        });
    }

    @Override
    public void sendSystemMessage(String username, String message) {
        if (username == null || username.isBlank()) {
            log.debug("Skip system message: empty username");
            return;
        }
        runWithRetry(() -> {
            // TODO: integrate in-app messaging/notification bus
            log.info("[SystemMessage] user={}, message={}", username, message);
        });
    }

    private void runWithRetry(Runnable task) {
        long[] backoff = new long[]{200, 500, 1000};
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                task.run();
                return;
            } catch (Exception ex) {
                if (attempt == MAX_RETRIES) {
                    log.error("Notification send failed after {} attempts", MAX_RETRIES, ex);
                    return;
                }
                long sleep = backoff[Math.min(attempt - 1, backoff.length - 1)];
                log.warn("Notification send failed (attempt {}), retry in {}ms", attempt, sleep);
                try {
                    TimeUnit.MILLISECONDS.sleep(sleep);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}

