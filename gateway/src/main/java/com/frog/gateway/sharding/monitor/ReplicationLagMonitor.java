package com.frog.gateway.sharding.monitor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 延迟监控
 *
 * @author Deng
 * createData 2025/11/11 16:00
 * @version 1.0
 */
@Component
@Slf4j
public class ReplicationLagMonitor {
    private final DataSource slave0;

    public ReplicationLagMonitor(@Qualifier("slave0-0") DataSource slave0) {
        this.slave0 = slave0;
    }

    @Scheduled(fixedRate = 10000)
    public void checkReplicationLag() {
        try (Connection conn = slave0.getConnection();
             PreparedStatement ps = conn.prepareStatement("SHOW SLAVE STATUS");
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                long lag = rs.getLong("Seconds_Behind_Master");
                if (lag > 10) {
                    log.warn("[ReplicationLag] slave0 延迟 {} 秒", lag);
                    // TODO: 集成告警系统，如 Prometheus Alert / Feishu Webhook
                } else {
                    log.debug("[ReplicationLag] slave0 延迟 {} 秒", lag);
                }
            }
        } catch (SQLException e) {
            log.error("[ReplicationLag] 检查复制延迟失败", e);
        }
    }
}
