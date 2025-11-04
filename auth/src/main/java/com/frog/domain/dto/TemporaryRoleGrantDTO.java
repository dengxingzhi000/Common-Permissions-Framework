package com.frog.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 临时角色授予DTO
 *
 * @author Deng
 * createData 2025/11/3 15:57
 * @version 1.0
 */
@Data
@Schema(description = "临时角色授予DTO")
public class TemporaryRoleGrantDTO {

    @Schema(description = "角色ID列表")
    @NotNull(message = "角色列表不能为空")
    private List<UUID> roleIds;

    @Schema(description = "生效时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime effectiveTime;

    @Schema(description = "过期时间")
    @NotNull(message = "过期时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime expireTime;

    @Schema(description = "授予原因")
    private String reason;
}
