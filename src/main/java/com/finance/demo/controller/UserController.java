package com.finance.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.finance.demo.common.Result;
import com.finance.demo.entity.SysUser;
import com.finance.demo.mapper.SysUserMapper;
import com.finance.demo.mapper.DepartmentMapper;
import com.finance.demo.entity.Department;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器
 * 仅限 IT管理员 使用
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin
public class UserController {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 获取用户列表
     */
    @GetMapping
    public Result<List<SysUser>> list() {
        List<SysUser> users = userMapper.selectList(new QueryWrapper<SysUser>().orderByDesc("created_at"));
        // 清除密码，不返回给前端
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }

    /**
     * 获取单个用户
     */
    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable Integer id) {
        SysUser user = userMapper.selectById(id);
        if (user != null) {
            user.setPassword(null);
            return Result.success(user);
        }
        return Result.error("用户不存在");
    }

    /**
     * 创建用户
     */
    @PostMapping
    public Result<SysUser> create(@RequestBody SysUser user) {
        // 检查用户名是否已存在
        SysUser existing = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", user.getUsername()));
        if (existing != null) {
            return Result.error("用户名已存在");
        }

        // 密码加密
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            user.setPassword("123456"); // 默认密码
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userMapper.insert(user);
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public Result<SysUser> update(@PathVariable Integer id, @RequestBody SysUser user) {
        SysUser existing = userMapper.selectById(id);
        if (existing == null) {
            return Result.error("用户不存在");
        }

        // 不允许修改密码（需走专门的修改密码接口）
        user.setPassword(null);
        user.setId(id);

        userMapper.updateById(user);
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        SysUser existing = userMapper.selectById(id);
        if (existing == null) {
            return Result.error("用户不存在");
        }

        userMapper.deleteById(id);
        return Result.success(null);
    }

    /**
     * 重置密码
     */
    @PostMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Integer id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 重置为默认密码 123456
        user.setPassword(passwordEncoder.encode("123456"));
        userMapper.updateById(user);

        return Result.success(null);
    }
}