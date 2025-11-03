package com.frog.mapper;

import com.frog.domain.entity.SysPermissionApproval;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <p>
 * 权限申请审批表 Mapper 接口
 * </p>
 *
 * @author author
 * @since 2025-10-30
 */
@Mapper
public interface SysPermissionApprovalMapper extends BaseMapper<SysPermissionApproval> {
    /**
     * 插入审批申请
     */
    @Insert("""
        INSERT INTO sys_approval (
            id, applicant_id, user_id, target_id, target_type,
            apply_reason, expire_time, status, create_time
        ) VALUES (
            #{id}, #{applicantId}, #{userId}, #{targetId}, #{targetType},
            #{reason}, #{expireTime}, #{status}, #{createTime}
        )
    """)
    void insertApproval(
            @Param("id") UUID id,
            @Param("applicantId") UUID applicantId,
            @Param("userId") UUID userId,
            @Param("targetId") UUID targetId,
            @Param("targetType") String targetType,
            @Param("reason") String reason,
            @Param("expireTime") LocalDateTime expireTime,
            @Param("status") Integer status,
            @Param("createTime") LocalDateTime createTime
    );

    /**
     * 更新审批状态
     */
    @Update("""
        UPDATE sys_approval SET
            status = #{status},
            approver_id = #{approverId},
            approve_comment = #{comment},
            approve_time = #{approveTime}
        WHERE id = #{id}
    """)
    void updateApprovalStatus(
            @Param("id") UUID id,
            @Param("status") Integer status,
            @Param("approverId") UUID approverId,
            @Param("comment") String comment,
            @Param("approveTime") LocalDateTime approveTime
    );

    /**
     * 获取申请人ID
     */
    @Select("SELECT applicant_id FROM sys_approval WHERE id = #{id}")
    UUID getApplicantId(@Param("id") UUID id);

    /**
     * 获取审批状态
     */
    @Select("SELECT status FROM sys_approval WHERE id = #{id}")
    Integer getApprovalStatus(@Param("id") UUID id);

    /**
     * 获取审批详情
     */
    @Select("""
        SELECT * FROM sys_approval WHERE id = #{id}
    """)
    Map<String, Object> getApprovalDetail(@Param("id") UUID id);

    /**
     * 授予角色
     */
    @Insert("""
        INSERT INTO sys_user_role (
            user_id, role_id, expire_time, approval_status, create_time
        ) VALUES (
            #{userId}, #{roleId}, #{expireTime}, #{approvalStatus}, NOW()
        )
    """)
    void grantRole(
            @Param("userId") UUID userId,
            @Param("roleId") UUID roleId,
            @Param("expireTime") LocalDateTime expireTime,
            @Param("approvalStatus") Integer approvalStatus
    );

    /**
     * 授予权限
     */
    @Insert("""
        INSERT INTO sys_user_permission (
            user_id, permission_id, expire_time, approval_status, create_time
        ) VALUES (
            #{userId}, #{permissionId}, #{expireTime}, #{approvalStatus}, NOW()
        )
    """)
    void grantPermission(
            @Param("userId") UUID userId,
            @Param("permissionId") UUID permissionId,
            @Param("expireTime") LocalDateTime expireTime,
            @Param("approvalStatus") Integer approvalStatus
    );

    /**
     * 查询待审批列表
     */
    @Select("""
        <script>
        SELECT a.*, 
               u1.username as applicant_name,
               u2.username as user_name,
               CASE 
                   WHEN a.target_type = 'ROLE' THEN r.role_name
                   WHEN a.target_type = 'PERMISSION' THEN p.permission_name
               END as target_name
        FROM sys_approval a
        LEFT JOIN sys_user u1 ON a.applicant_id = u1.id
        LEFT JOIN sys_user u2 ON a.user_id = u2.id
        LEFT JOIN sys_role r ON a.target_type = 'ROLE' AND a.target_id = r.id
        LEFT JOIN sys_permission p ON a.target_type = 'PERMISSION' AND a.target_id = p.id
        WHERE a.status = 0
        ORDER BY a.create_time DESC
        LIMIT #{size} OFFSET #{offset}
        </script>
    """)
    List<Map<String, Object>> findPendingApprovals(
            @Param("approverId") UUID approverId,
            @Param("page") Integer page,
            @Param("size") Integer size
    );

    /**
     * 查询用户的申请列表
     */
    @Select("""
        <script>
        SELECT a.*,
               u.username as user_name,
               CASE 
                   WHEN a.target_type = 'ROLE' THEN r.role_name
                   WHEN a.target_type = 'PERMISSION' THEN p.permission_name
               END as target_name,
               approver.username as approver_name
        FROM sys_approval a
        LEFT JOIN sys_user u ON a.user_id = u.id
        LEFT JOIN sys_role r ON a.target_type = 'ROLE' AND a.target_id = r.id
        LEFT JOIN sys_permission p ON a.target_type = 'PERMISSION' AND a.target_id = p.id
        LEFT JOIN sys_user approver ON a.approver_id = approver.id
        WHERE a.applicant_id = #{applicantId}
        ORDER BY a.create_time DESC
        LIMIT #{size} OFFSET #{offset}
        </script>
    """)
    List<Map<String, Object>> findApplicationsByUser(
            @Param("applicantId") UUID applicantId,
            @Param("page") Integer page,
            @Param("size") Integer size
    );
}
