package com.frog.common.log.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.frog.common.log.entity.SysAuditLog;

import java.util.UUID;

/**
 * <p>
 * 操作审计日志表 服务类
 * </p>
 *
 * @author author
 * @since 2025-10-14
 */
public interface ISysAuditLogService extends IService<SysAuditLog> {
    void recordLogin(UUID userId, String username, String ipAddress,
                     boolean success, String remark);

    void recordLoginFailure(String username, String ipAddress, String reason);

    void recordLogout(UUID userId, String remark);
}
