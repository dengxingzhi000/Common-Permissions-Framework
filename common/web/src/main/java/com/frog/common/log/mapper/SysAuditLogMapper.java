package com.frog.common.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.frog.common.log.entity.SysAuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 操作审计日志表 Mapper 接口
 * </p>
 *
 * @author author
 * @since 2025-10-14
 */
@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {

}
