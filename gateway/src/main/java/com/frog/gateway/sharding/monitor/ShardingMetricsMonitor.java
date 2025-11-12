package com.frog.gateway.sharding.monitor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.driver.jdbc.core.datasource.ShardingSphereDataSource;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.mode.manager.ContextManager;
import org.apache.shardingsphere.mode.metadata.MetaDataContexts;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.util.Map;

/**
 *
 *
 * @author Deng
 * createData 2025/11/11 15:31
 * @version 1.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ShardingMetricsMonitor {
    private final DataSource dataSource;

    /**
     * 每分钟采集一次 ShardingSphere 内部元数据统计信息
     */
    @Scheduled(fixedRate = 60000)
    public void collectMetrics() {
        if (!(dataSource instanceof ShardingSphereDataSource shardingDS)) {
            log.warn("[ShardingMetrics] 当前数据源不是 ShardingSphereDataSource，跳过采集。");
            return;
        }

        try {
            //获取 ContextManager（通过反射）
            Field contextManagerField = ShardingSphereDataSource.class.getDeclaredField("contextManager");
            contextManagerField.setAccessible(true);
            ContextManager contextManager = (ContextManager) contextManagerField.get(shardingDS);

            if (contextManager == null) {
                log.warn("[ShardingMetrics] ContextManager 未初始化。");
                return;
            }

            // 获取 MetaDataContexts
            Field metaField = ContextManager.class.getDeclaredField("metaDataContexts");
            metaField.setAccessible(true);
            MetaDataContexts metaDataContexts = (MetaDataContexts) metaField.get(contextManager);

            if (metaDataContexts == null) {
                log.warn("[ShardingMetrics] MetaDataContexts 为空。");
                return;
            }

            // 3输出逻辑数据库信息
            Map<String, ShardingSphereDatabase> metaMap = metaDataContexts.getMetaData().getDatabases();
            metaMap.forEach((logicDbName, metaData) -> {
                var ruleCount = metaData.getRuleMetaData().getRules().size();
                var schemaNames = metaData.getSchemas().keySet();
                log.info("""
                        [ShardingMetrics]
                        ├─ 逻辑库名称: {}
                        ├─ 活动规则数: {}
                        ├─ 包含Schema: {}
                        """, logicDbName, ruleCount, schemaNames);
            });

        } catch (NoSuchFieldException e) {
            log.error("[ShardingMetrics] 反射字段未找到，可能与 ShardingSphere 版本不兼容 (5.4.1+)", e);
        } catch (Exception e) {
            log.error("[ShardingMetrics] 采集元数据时发生异常", e);
        }
    }
}

