package com.auth;

import com.frog.AuthApplication;
import com.frog.controller.SysAuthController;
import com.frog.controller.SysUserController;
import com.frog.domain.dto.LoginRequest;
import com.frog.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * 测试类
 *
 * @author Deng
 * createData 2025/10/15 9:25
 * @version 1.0
 */
@SpringBootTest(classes = AuthApplication.class)
@SpringJUnitConfig
public class authTest {

    @Test
    @WithMockUser(authorities = {"system:user:add", "system:user:list"})
    void uuidv7Test() {
        // Mock HttpServletRequest
//        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
//        mockRequest.setRemoteAddr("127.0.0.1");
//        mockRequest.addHeader("User-Agent", "Test-Agent");
//
//        System.out.println("sysUserMapper.findByUsername(\"dengxingzhiTest\") = " + sysUserMapper.findByUsername("dengxingzhiTest"));
//        // 创建登录请求
//        LoginRequest loginRequest = new LoginRequest();
//        loginRequest.setUsername("dengxingzhiTest");
//        loginRequest.setPassword("123456");
//
//        sysAuthController.login(loginRequest, mockRequest);
        System.out.println("hello+++++++++++++++++++");


//        // 创建用户DTO用于测试
//        UserDTO userDTO = new UserDTO();
//        userDTO.setUsername("dengxingzhiTest");
//        userDTO.setRealName("邓");
//        userDTO.setPassword("123456");
//
//        // 执行需要权限的接口调用
//        sysUserController.add(userDTO);
        
//        sysUserController.list(1, 10, "testuser", 0);
    }
}
