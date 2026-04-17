package com.finance.demo.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.demo.common.Result;
import com.finance.demo.entity.SysUser;
import com.finance.demo.mapper.SysUserMapper;
import com.finance.demo.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private SysUserMapper sysUserMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行OPTIONS请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");

        // 没有Token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(response, "未登录或Token已过期");
            return false;
        }

        String token = authHeader.substring(7);

        // 验证并解析Token（parseToken 内部已校验签名和过期时间）
        Claims claims = JwtUtil.parseToken(token);
        if (claims == null) {
            sendUnauthorized(response, "Token无效或已过期");
            return false;
        }

        // 查询用户是否存在
        Integer userId = claims.get("userId", Integer.class);
        SysUser user = sysUserMapper.selectById(userId);

        if (user == null) {
            sendUnauthorized(response, "用户不存在");
            return false;
        }

        // 将用户ID存入请求属性，供后续使用
        request.setAttribute("currentUserId", userId);
        request.setAttribute("currentUsername", user.getUsername());

        return true;
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Result<?> result = Result.error(message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
