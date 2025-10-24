package com.frog.common.rabbitmq.event;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 *
 *
 * @author Deng
 * createData 2025/10/24 14:01
 * @version 1.0
 */
@Data
public class UserLoginEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String ipAddress;
    private String deviceId;
    private LocalDateTime loginTime;
    private String location;
}
