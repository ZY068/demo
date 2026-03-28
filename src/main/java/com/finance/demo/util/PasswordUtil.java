package com.finance.demo.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码工具类
 * 用于生成 BCrypt 密码哈希值
 */
public class PasswordUtil {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "123456";
        String encodedPassword = encoder.encode(rawPassword);
        System.out.println("原始密码: " + rawPassword);
        System.out.println("BCrypt 哈希值: " + encodedPassword);
        System.out.println("验证结果: " + encoder.matches(rawPassword, encodedPassword));

        // 验证 init.sql 中的哈希值是否正确
        String sqlHash = "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";
        System.out.println("\n验证 init.sql 中的哈希值:");
        System.out.println("验证结果: " + encoder.matches(rawPassword, sqlHash));
    }
}