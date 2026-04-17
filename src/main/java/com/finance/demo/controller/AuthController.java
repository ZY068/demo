package com.finance.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.finance.demo.common.Result;
import com.finance.demo.entity.SysUser;
import com.finance.demo.mapper.SysUserMapper;
import com.finance.demo.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 安全登录：使用 BCrypt 密码验证
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 用户登录
     * 使用 BCrypt 验证密码
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody SysUser loginRequest) {
        // 1. 根据用户名查询用户
        SysUser user = userMapper.selectOne(new QueryWrapper<SysUser>()
                .eq("username", loginRequest.getUsername()));

        if (user == null) {
            return Result.error("用户名或密码错误");
        }

        // 2. 检查用户状态
        if (user.getStatus() != null && user.getStatus() != 1) {
            return Result.error("账户已被禁用");
        }

        // 3. 使用 BCrypt 验证密码
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return Result.error("用户名或密码错误");
        }

        // 4. 生成 JWT Token
        String token = JwtUtil.generateToken(user.getId(), user.getUsername());

        // 5. 构建返回数据（不包含密码）
        user.setPassword(null);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("role", user.getRole());
        data.put("deptId", user.getDeptId());

        return Result.success(data);
    }

    /**
     * 修改密码（可选功能）
     */
    @PostMapping("/change-password")
    public Result<?> changePassword(@RequestBody ChangePasswordRequest request, HttpServletRequest httpRequest) {
        Integer currentUserId = (Integer) httpRequest.getAttribute("currentUserId");
        if (currentUserId == null) {
            return Result.error("请先登录");
        }

        SysUser user = userMapper.selectById(currentUserId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return Result.error("原密码错误");
        }

        // 设置新密码（加密）
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);

        return Result.success(null);
    }

    /**
     * 密码修改请求体
     */
    public static class ChangePasswordRequest {
        private Integer userId;
        private String oldPassword;
        private String newPassword;

        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }
        public String getOldPassword() { return oldPassword; }
        public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}