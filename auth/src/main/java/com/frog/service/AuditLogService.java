package com.frog.service;

import com.frog.domain.entity.SysAuditLog;
import com.frog.mapper.SysAuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 审计日志服务
 *
 * @author Deng
 * createData 2025/10/14 15:01
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final SysAuditLogMapper sysAuditLogMapper;

    @Async
    public void recordLogin(UUID userId, String username, String ipAddress,
                            boolean success, String remark) {
        SysAuditLog log = new SysAuditLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setOperationType("LOGIN");
        log.setIpAddress(ipAddress);
        log.setStatus(success ? 1 : 0);
        log.setOperationDesc(remark);
        log.setCreateTime(LocalDateTime.now());
        sysAuditLogMapper.insert(log);
    }

    @Async
    public void recordLoginFailure(String username, String ipAddress, String reason) {
        SysAuditLog log = new SysAuditLog();
        log.setUsername(username);
        log.setOperationType("LOGIN_FAILURE");
        log.setIpAddress(ipAddress);
        log.setStatus(0);
        log.setErrorMsg(reason);
        log.setCreateTime(LocalDateTime.now());
        sysAuditLogMapper.insert(log);
    }

    @Async
    public void recordLogout(UUID userId, String remark) {
        SysAuditLog log = new SysAuditLog();
        log.setUserId(userId);
        log.setOperationType("LOGOUT");
        log.setStatus(1);
        log.setOperationDesc(remark);
        log.setCreateTime(LocalDateTime.now());
        sysAuditLogMapper.insert(log);
    }
}