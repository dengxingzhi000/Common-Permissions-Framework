package com.frog.common.security.filter;

import com.frog.common.response.ApiResponse;
import com.frog.common.security.util.IpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
/**
 *
 *
 * @author Deng
 * createData 2025/10/24 16:11
 * @version 1.0
 */
@Component
@Slf4j
public class SqlInjectionFilter implements Filter {

    // SQL注入关键字正则
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?:')|(?:--)|(/\\*(?:.|[\\n\\r])*?\\*/)|"
                    + "(\\b(select|update|union|and|or|delete|insert|trancate|char|into|substr|ascii|declare|exec|count|master|into|drop|execute)\\b)",
            Pattern.CASE_INSENSITIVE
    );

    // XSS攻击正则
    private static final Pattern XSS_PATTERN = Pattern.compile(
            "<script[^>]*?>.*?</script>|javascript:|<iframe.*?>|<img.*?onerror.*?>",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 检查所有参数
        for (String paramName : httpRequest.getParameterMap().keySet()) {
            String[] paramValues = httpRequest.getParameterValues(paramName);
            for (String paramValue : paramValues) {
                if (StrUtil.isNotBlank(paramValue)) {
                    // SQL注入检测
                    if (SQL_INJECTION_PATTERN.matcher(paramValue).find()) {
                        log.warn("SQL injection detected! Parameter: {}, Value: {}, IP: {}",
                                paramName, paramValue, IpUtils.getClientIp(httpRequest));
                        sendErrorResponse(httpResponse, "检测到非法SQL注入攻击");
                        return;
                    }

                    // XSS检测
                    if (XSS_PATTERN.matcher(paramValue).find()) {
                        log.warn("XSS attack detected! Parameter: {}, Value: {}, IP: {}",
                                paramName, paramValue, IpUtils.getClientIp(httpRequest));
                        sendErrorResponse(httpResponse, "检测到XSS攻击");
                        return;
                    }
                }
            }
        }

        chain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json;charset=UTF-8");

        ApiResponse<Void> result = ApiResponse.fail(400, message);
        response.getWriter().write(JSON.toJSONString(result));
    }
}
