package com.frog.system.service.Impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frog.common.exception.BusinessException;
import com.frog.common.util.UUIDv7Util;
import com.frog.common.dto.approval.ApprovalDTO;
import com.frog.common.dto.approval.ApprovalProcessDTO;
import com.frog.common.web.util.SecurityUtils;
import com.frog.system.domain.entity.SysPermissionApproval;
import com.frog.system.domain.entity.SysUser;
import com.frog.system.mapper.SysPermissionApprovalMapper;
import com.frog.system.mapper.SysUserMapper;
import com.frog.system.service.ISysPermissionApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限申请审批服务实现
 *
 * @author Deng
 * @since 2025-11-03
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SysPermissionApprovalServiceImpl
        extends ServiceImpl<SysPermissionApprovalMapper, SysPermissionApproval>
        implements ISysPermissionApprovalService {
    private final SysPermissionApprovalMapper approvalMapper;
    private final SysUserMapper userMapper;

    /**
     * 提交权限申请
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UUID submitApproval(ApprovalDTO dto) {
        UUID currentUserId = SecurityUtils.getCurrentUserUuid().orElse(null);

        // 1. 检查是否有重复的待审批申请
        if (approvalMapper.existsPendingApplication(
                currentUserId, dto.getTargetUserId(), dto.getApprovalType())) {
            throw new BusinessException("您已有相同的申请正在审批中");
        }

        // 2. 构建审批实体
        SysPermissionApproval approval = SysPermissionApproval.builder()
                .id(UUIDv7Util.generate())
                .applicantId(currentUserId)
                .approvalType(dto.getApprovalType())
                .targetUserId(dto.getTargetUserId())
                .roleIds(dto.getRoleIds() != null ?
                        dto.getRoleIds().stream()
                                .map(UUID::toString)
                                .collect(Collectors.joining(",")) : null)
                .permissionIds(dto.getPermissionIds() != null ?
                        dto.getPermissionIds().stream()
                                .map(UUID::toString)
                                .collect(Collectors.joining(",")) : null)
                .effectiveTime(dto.getEffectiveTime())
                .expireTime(dto.getExpireTime())
                .applyReason(dto.getApplyReason())
                .businessJustification(dto.getBusinessJustification())
                .approvalStatus(0) // 待审批
                .build();

        // 3. 构建审批链
        List<UUID> approvalChain = buildApprovalChain(dto.getApprovalType(), currentUserId);
        approval.setApprovalChain(JSON.toJSONString(approvalChain));

        // 4. 设置第一个审批人
        if (!approvalChain.isEmpty()) {
            approval.setCurrentApproverId(approvalChain.getFirst());
        }

        // 5. 保存申请
        approvalMapper.insert(approval);

        log.info("Permission approval submitted: id={}, applicant={}, type={}",
                approval.getId(), currentUserId, dto.getApprovalType());

        // 6. 发送通知给第一个审批人
        sendApprovalNotification(approval);

        return approval.getId();
    }

    /**
     * 审批处理
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processApproval(UUID approvalId, ApprovalProcessDTO dto) {
        UUID currentUserId = SecurityUtils.getCurrentUserUuid().orElse(null);

        // 1. 查询审批记录
        SysPermissionApproval approval = approvalMapper.selectById(approvalId);
        if (approval == null) {
            throw new BusinessException("审批记录不存在");
        }

        // 2. 验证审批权限
        if (!Objects.equals(currentUserId, approval.getCurrentApproverId())) {
            throw new BusinessException("您不是当前审批人");
        }

        // 3. 验证审批状态
        if (approval.getApprovalStatus() != 0 && approval.getApprovalStatus() != 1) {
            throw new BusinessException("该申请已被处理");
        }

        // 4. 处理审批
        if (dto.getApproved()) {
            handleApprove(approval, currentUserId);
        } else {
            handleReject(approval, dto, currentUserId);
        }
    }

    /**
     * 处理通过
     */
    private void handleApprove(SysPermissionApproval approval, UUID approverId) {
        // 1. 获取审批链
        List<UUID> approvalChain = JSON.parseArray(
                approval.getApprovalChain(), UUID.class);

        int currentIndex = approvalChain.indexOf(approverId);

        // 2. 判断是否还有下一级审批人
        if (currentIndex < approvalChain.size() - 1) {
            // 还有下一级，转给下一个审批人
            UUID nextApprover = approvalChain.get(currentIndex + 1);
            approvalMapper.updateCurrentApprover(approval.getId(), nextApprover);

            log.info("Approval forwarded to next approver: id={}, next={}",
                    approval.getId(), nextApprover);

            // 发送通知给下一个审批人
            sendApprovalNotification(approval);
        } else {
            // 最后一级审批人，批准通过
            approvalMapper.updateApprovalStatus(
                    approval.getId(), 2, approverId, null);

            log.info("Approval granted: id={}, approver={}",
                    approval.getId(), approverId);

            // 执行权限授予
            grantPermissions(approval);

            // 通知申请人
            sendResultNotification(approval, true);
        }
    }

    /**
     * 处理拒绝
     */
    private void handleReject(SysPermissionApproval approval,
                              ApprovalProcessDTO dto, UUID approverId) {
        approvalMapper.updateApprovalStatus(
                approval.getId(), 3, approverId, dto.getRejectReason());

        log.info("Approval rejected: id={}, approver={}, reason={}",
                approval.getId(), approverId, dto.getRejectReason());

        // 通知申请人
        sendResultNotification(approval, false);
    }

    /**
     * 执行权限授予
     */
    private void grantPermissions(SysPermissionApproval approval) {
        UUID targetUserId = approval.getTargetUserId();

        switch (approval.getApprovalType()) {
            case 1 -> // 角色申请
                    Optional.ofNullable(approval.getRoleIds())
                            .ifPresent(roleIdsStr -> {
                                List<UUID> roleIds = Arrays.stream(roleIdsStr.split(","))
                                        .map(UUID::fromString)
                                        .toList();
                                userMapper.batchInsertUserRoles(targetUserId, roleIds,
                                        SecurityUtils.getCurrentUserUuid().orElse(null));
                            });

            case 2 -> // 权限申请
                    // TODO: 实现直接权限授予逻辑（如果需要）
                    log.debug("Direct permission grant not implemented yet");

            case 3 -> // 临时授权
                    // 临时授权在 sys_user_role 中设置过期时间
                    Optional.ofNullable(approval.getRoleIds())
                            .ifPresent(roleIdsStr -> {
                                List<UUID> roleIds = Arrays.stream(roleIdsStr.split(","))
                                        .map(UUID::fromString)
                                        .toList();
                                // TODO: 实现带过期时间的角色授予
                                log.debug("Temporary role grant with expiration not implemented yet");
                            });

            default -> throw new BusinessException("未知的审批类型: " + approval.getApprovalType());
        }
    }

    /**
     * 撤回申请
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawApproval(UUID approvalId) {
        UUID currentUserId = SecurityUtils.getCurrentUserUuid().orElse(null);

        SysPermissionApproval approval = approvalMapper.selectById(approvalId);
        if (approval == null) {
            throw new BusinessException("审批记录不存在");
        }

        // 验证权限：只有申请人可以撤回
        if (!Objects.equals(currentUserId, approval.getApplicantId())) {
            throw new BusinessException("您无权撤回该申请");
        }

        // 验证状态：只有待审批的可以撤回
        if (approval.getApprovalStatus() != 0 && approval.getApprovalStatus() != 1) {
            throw new BusinessException("该申请已被处理，无法撤回");
        }

        // 更新状态为已撤回
        approvalMapper.updateApprovalStatus(approvalId, 4, currentUserId, "申请人撤回");

        log.info("Approval withdrawn: id={}, applicant={}", approvalId, currentUserId);
    }

    /**
     * 查询待我审批的列表
     */
    @Override
    public Page<ApprovalDTO> getPendingApprovals(Integer pageNum, Integer pageSize) {
        UUID currentUserId = SecurityUtils.getCurrentUserUuid().orElse(null);
        Page<SysPermissionApproval> page = new Page<>(pageNum, pageSize);

        Page<SysPermissionApproval> result = approvalMapper.selectPendingApprovals(
                page, currentUserId);

        return convertToDTO(result);
    }

    /**
     * 查询我的申请历史
     */
    @Override
    public Page<ApprovalDTO> getMyApplications(Integer pageNum, Integer pageSize) {
        UUID currentUserId = SecurityUtils.getCurrentUserUuid().orElse(null);
        Page<SysPermissionApproval> page = new Page<>(pageNum, pageSize);

        Page<SysPermissionApproval> result = approvalMapper.selectUserApplyHistory(
                page, currentUserId);

        return convertToDTO(result);
    }

    /**
     * 构建审批链
     * 根据申请类型和申请人确定审批链路
     */
    private List<UUID> buildApprovalChain(Integer approvalType, UUID applicantId) {
        List<UUID> chain = new ArrayList<>();

        // 查询申请人信息
        SysUser applicant = userMapper.selectById(applicantId);
        if (applicant == null) {
            return chain;
        }

        // TODO: 根据实际业务规则构建审批链
        // 示例：部门经理 -> 系统管理员

        // 1. 添加部门经理
        UUID deptManager = getDeptManager(applicant.getDeptId());
        if (deptManager != null) {
            chain.add(deptManager);
        }

        // 2. 添加系统管理员
        UUID sysAdmin = getSystemAdmin();
        if (sysAdmin != null) {
            chain.add(sysAdmin);
        }

        return chain;
    }

    /**
     * 获取部门经理
     */
    private UUID getDeptManager(UUID deptId) {
        // TODO: 从 sys_dept 表查询部门负责人
        return null;
    }

    /**
     * 获取系统管理员
     */
    private UUID getSystemAdmin() {
        // TODO: 查询系统管理员用户
        return null;
    }

    /**
     * 发送审批通知
     */
    private void sendApprovalNotification(SysPermissionApproval approval) {
        // TODO: 集成消息通知服务
        log.info("Approval notification sent: id={}, approver={}",
                approval.getId(), approval.getCurrentApproverId());
    }

    /**
     * 发送审批结果通知
     */
    private void sendResultNotification(SysPermissionApproval approval, boolean approved) {
        // TODO: 通知申请人审批结果
        log.info("Approval result notification sent: id={}, result={}",
                approval.getId(), approved ? "approved" : "rejected");
    }

    /**
     * 查询审批详情
     */
    @Override
    public ApprovalDTO getApprovalDetail(UUID approvalId) {
        SysPermissionApproval approval = approvalMapper.selectById(approvalId);
        if (approval == null) {
            throw new BusinessException("审批记录不存在");
        }

        return convertToDTO(approval);
    }

    /**
     * 转换为DTO
     */
    private Page<ApprovalDTO> convertToDTO(Page<SysPermissionApproval> page) {
        Page<ApprovalDTO> dtoPage = new Page<>(
                page.getCurrent(), page.getSize(), page.getTotal());

        List<ApprovalDTO> records = page.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        dtoPage.setRecords(records);
        return dtoPage;
    }

    private ApprovalDTO convertToDTO(SysPermissionApproval entity) {
        ApprovalDTO approvalDTO = ApprovalDTO.builder()
                .id(entity.getId())
                .applicantId(entity.getApplicantId())
                .approvalType(entity.getApprovalType())
                .targetUserId(entity.getTargetUserId())
                .applyReason(entity.getApplyReason())
                .businessJustification(entity.getBusinessJustification())
                .approvalStatus(entity.getApprovalStatus())
                .effectiveTime(entity.getEffectiveTime())
                .expireTime(entity.getExpireTime())
                .approvedTime(entity.getApprovedTime())
                .rejectReason(entity.getRejectReason())
                .build();

        // 解析角色和权限ID
        if (entity.getRoleIds() != null) {
            approvalDTO.setRoleIds(Arrays.stream(entity.getRoleIds().split(","))
                    .map(UUID::fromString)
                    .toList());
        }

        if (entity.getPermissionIds() != null) {
            approvalDTO.setPermissionIds(Arrays.stream(entity.getPermissionIds().split(","))
                    .map(UUID::fromString)
                    .toList());
        }

        return approvalDTO;
    }
}