package com.frog.common.dto.role;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 *
 *
 * @author Deng
 * createData 2025/10/15 11:43
 * @version 1.0
 */
@Data
public class RoleDTO {
    private UUID id;

    @NotBlank(message = "角色编码不能为空")
    @Pattern(regexp = "^ROLE_[A-Z_]+$", message = "角色编码必须以ROLE_开头，只能包含大写字母和下划线")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称长度不能超过64位")
    private String roleName;

    @Size(max = 255, message = "角色描述长度不能超过255位")
    private String roleDesc;

    @NotNull(message = "角色级别不能为空")
    @Min(value = 0, message = "角色级别不能小于0")
    @Max(value = 999, message = "角色级别不能大于999")
    private Integer roleLevel;

    @NotNull(message = "数据权限不能为空")
    private Integer dataScope;

    @DecimalMin(value = "0.00", message = "最大审批金额不能为负数")
    private BigDecimal maxApprovalAmount;

    private String businessScope;

    private Integer status;

    private Integer sortOrder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    // 关联数据
    private List<UUID> permissionIds;
    private List<UUID> deptIds;
    private Integer userCount; // 拥有该角色的用户数
}
