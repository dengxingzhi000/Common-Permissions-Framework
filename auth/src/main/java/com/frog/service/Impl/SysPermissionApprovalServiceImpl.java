package com.frog.service.Impl;

import com.frog.common.exception.BusinessException;
import com.frog.common.security.util.SecurityUtils;
import com.frog.domain.entity.SysPermissionApproval;
import com.frog.mapper.SysPermissionApprovalMapper;
import com.frog.mapper.SysUserMapper;
import com.frog.service.ISysPermissionApprovalService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * <p>
 * 权限申请审批表 服务实现类
 * </p>
 *
 * @author author
 * @since 2025-10-30
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SysPermissionApprovalServiceImpl extends ServiceImpl<SysPermissionApprovalMapper, SysPermissionApproval>
        implements ISysPermissionApprovalService {
    private final SysPermissionApprovalMapper approvalMapper;
    private final SysUserMapper userMapper;

    /**
     * 申请权限/角色
     */
    @Transactional(rollbackFor = Exception.class)
    public UUID applyPermission(UUID userId, UUID targetId, String targetType,
                                String reason, Integer expireDays) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        // 验证目标类型
        if (!"ROLE".equals(targetType) && !"PERMISSION".equals(targetType)) {
            throw new BusinessException("目标类型只能是ROLE或PERMISSION");
        }

        // 验证申请理由长度
        if (reason == null || reason.length() < 10) {
            throw new BusinessException("申请理由至少10个字符");
        }

        // 创建审批申请
        UUID approvalId = UUID.randomUUID();
        LocalDateTime expireTime = expireDays != null ? LocalDateTime.now().plusDays(expireDays) : null;

        approvalMapper.insertApproval(
                approvalId,
                currentUserId,
                userId,
                targetId,
                targetType,
                reason,
                expireTime,
                0, // 待审批
                LocalDateTime.now()
        );

        // TODO: 发送通知给审批人
        // notifyApprovers(approvalId, userId, targetType);

        log.info("Permission approval created: approvalId={}, applicant={}, userId={}, targetType={}",
                approvalId, currentUserId, userId, targetType);

        return approvalId;
    }

    /**
     * 审批通过
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(UUID approvalId, String comment) {
        UUID approverId = SecurityUtils.getCurrentUserId();

        // 验证审批人权限
        if (!cannotApprove(approverId)) {
            throw new BusinessException("您没有审批权限");
        }

        // 获取审批详情
        Map<String, Object> approval = approvalMapper.getApprovalDetail(approvalId);
        if (approval == null) {
            throw new BusinessException("审批申请不存在");
        }

        Integer status = (Integer) approval.get("status");
        if (status != 0) {
            throw new BusinessException("该申请已被处理");
        }

        // 更新审批状态
        approvalMapper.updateApprovalStatus(
                approvalId,
                1, // 已通过
                approverId,
                comment,
                LocalDateTime.now()
        );

        // 执行实际的权限授予
        grantPermissionAfterApproval(approval);

        log.info("Approval approved: approvalId={}, approverId={}", approvalId, approverId);
    }

    /**
     * 审批拒绝
     */
    @Transactional(rollbackFor = Exception.class)
    public void reject(UUID approvalId, String reason) {
        UUID approverId = SecurityUtils.getCurrentUserId();

        // 验证审批人权限
        if (!cannotApprove(approverId)) {
            throw new BusinessException("您没有审批权限");
        }

        // 验证审批状态
        Integer status = approvalMapper.getApprovalStatus(approvalId);
        if (status == null) {
            throw new BusinessException("审批申请不存在");
        }
        if (status != 0) {
            throw new BusinessException("该申请已被处理");
        }

        // 更新审批状态
        approvalMapper.updateApprovalStatus(
                approvalId,
                2, // 已拒绝
                approverId,
                reason,
                LocalDateTime.now()
        );

        log.info("Approval rejected: approvalId={}, approverId={}, reason={}",
                approvalId, approverId, reason);
    }

    /**
     * 撤销申请
     */
    @Transactional(rollbackFor = Exception.class)
    public void revoke(UUID approvalId) {
        UUID userId = SecurityUtils.getCurrentUserId();

        // 验证是否为申请人
        UUID applicantId = approvalMapper.getApplicantId(approvalId);
        if (applicantId == null) {
            throw new BusinessException("审批申请不存在");
        }
        if (!Objects.equals(userId, applicantId)) {
            throw new BusinessException("只能撤销自己的申请");
        }

        // 验证状态是否为待审批
        Integer status = approvalMapper.getApprovalStatus(approvalId);
        if (status != 0) {
            throw new BusinessException("只能撤销待审批的申请");
        }

        approvalMapper.updateApprovalStatus(
                approvalId,
                3, // 已撤销
                userId,
                "申请人主动撤销",
                LocalDateTime.now()
        );

        log.info("Approval revoked: approvalId={}, userId={}", approvalId, userId);
    }

    /**
     * 验证是否可以审批
     * 这里简化为检查是否有特定角色
     */
    private boolean cannotApprove(UUID userId) {
        var roles = userMapper.findRolesByUserId(userId);
        return !(
                roles.contains("ROLE_SUPER_ADMIN") ||
                        roles.contains("ROLE_ADMIN") ||
                        roles.contains("ROLE_DEPT_MANAGER")
        );
    }

    /**
     * 审批通过后授予权限
     */
    private void grantPermissionAfterApproval(Map<String, Object> approval) {
        String targetType = (String) approval.get("target_type");
        UUID userId = (UUID) approval.get("user_id");
        UUID targetId = (UUID) approval.get("target_id");
        LocalDateTime expireTime = (LocalDateTime) approval.get("expire_time");

        // 根据类型授予权限
        if ("ROLE".equals(targetType)) {
            approvalMapper.grantRole(userId, targetId, expireTime, 1);
            log.info("Role granted: userId={}, roleId={}", userId, targetId);
        } else if ("PERMISSION".equals(targetType)) {
            approvalMapper.grantPermission(userId, targetId, expireTime, 1);
            log.info("Permission granted: userId={}, permissionId={}", userId, targetId);
        }
    }

    /**
     * 查询待审批列表
     */
    public Object getPendingApprovals(UUID approverId, Integer page, Integer size) {
        int offset = (page - 1) * size;
        return approvalMapper.findPendingApprovals(approverId, offset, size);
    }

    /**
     * 查询我的申请列表
     */
    public Object getMyApplications(UUID applicantId, Integer page, Integer size) {
        int offset = (page - 1) * size;
        return approvalMapper.findApplicationsByUser(applicantId, offset, size);
    }
}
