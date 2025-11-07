package com.frog.common.dto.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 *
 *
 * @author Deng
 * createData 2025/10/15 11:41
 * @version 1.0
 */
@Data
public class UserDTO {
    private UUID id;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 32, message = "用户名长度4-32位")
    @Pattern(
            regexp = "^[a-zA-Z0-9_]+$",
            message = "用户名只能包含字母、数字和下划线"
    )
    private String username;

    @Size(min = 6, max = 20, message = "密码长度6-20位")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 64, message = "姓名长度不能超过64位")
    private String realName;

    @Pattern(
            regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$",
            message = "身份证号格式不正确"
    )
    private String idCard;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(
            regexp = "^1[3-9]\\d{9}$",
            message = "手机号格式不正确"
    )
    private String phone;

    private String avatar;

    private Integer status;

    private UUID deptId;

    private String deptName;

    private Integer userLevel;

    private Integer accountType;

    private Integer loginAttempts;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lockedUntil;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime passwordExpireTime;

    private Boolean forceChangePassword;

    private Boolean twoFactorEnabled;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastLoginTime;

    private String lastLoginIp;

    private String remark;

    // 关联数据
    private List<UUID> roleIds;
    private List<String> roleNames;
    private List<String> permissions;
}

