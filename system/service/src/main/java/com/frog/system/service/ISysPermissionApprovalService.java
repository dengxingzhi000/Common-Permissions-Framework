package com.frog.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.frog.common.dto.approval.ApprovalDTO;
import com.frog.common.dto.approval.ApprovalProcessDTO;
import com.frog.system.domain.entity.SysPermissionApproval;

import java.util.UUID;

/**
 * <p>
 * 权限申请审批表 服务类
 * </p>
 *
 * @author author
 * @since 2025-10-30
 */
public interface ISysPermissionApprovalService extends IService<SysPermissionApproval> {

    UUID submitApproval(ApprovalDTO dto);

    void processApproval(UUID approvalId, ApprovalProcessDTO dto);

    void withdrawApproval(UUID approvalId);

    Page<ApprovalDTO> getMyApplications(Integer pageNum, Integer pageSize);

    ApprovalDTO getApprovalDetail(UUID approvalId);

    Page<ApprovalDTO> getPendingApprovals(Integer pageNum, Integer pageSize);
}
