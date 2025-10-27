package com.frog.domain.entity;

import java.io.Serial;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 角色表
 * </p>
 *
 * @author author
 * @since 2025-10-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_role")
@Tag(
        name="SysRole对象",
        description="角色表"
)
public class SysRole implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "角色ID")
    @TableId(value = "id", type = IdType.NONE)
    private UUID id;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色描述")
    private String roleDesc;

    @Schema(description = "角色级别(数字越大权限越高)")
    private Integer roleLevel;

    @Schema(description = "数据权限:1-全部,2-自定义,3-本部门,4-本部门及以下,5-仅本人")
    private Integer dataScope;

    @Schema(description = "最大审批金额")
    private BigDecimal maxApprovalAmount;

    @Schema(description = "业务范围(JSON)")
    private String businessScope;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "创建人")
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private UUID createBy;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "更新人")
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private UUID updateBy;

    @Schema(description = "逻辑删除")
    private Integer deleted;
}
