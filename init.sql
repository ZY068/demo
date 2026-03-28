-- Active: 1773397840703@@127.0.0.1@3306@smart_expense_db
USE smart_expense_db;

-- 删除表的顺序（注意外键依赖）
DROP TABLE IF EXISTS `approval_record`;
DROP TABLE IF EXISTS `approval_flow`;
DROP TABLE IF EXISTS `expense_claim_detail`;
DROP TABLE IF EXISTS `audit_report`;
DROP TABLE IF EXISTS `department_budget`;
DROP TABLE IF EXISTS `risk_alert`;
DROP TABLE IF EXISTS `invoice_archive`;
DROP TABLE IF EXISTS `invoice`;
DROP TABLE IF EXISTS `policy_rule_condition`;
DROP TABLE IF EXISTS `expense_policy_rule`;
DROP TABLE IF EXISTS `expense_category`;
DROP TABLE IF EXISTS `audit_log`;
DROP TABLE IF EXISTS `expense_claim`;
DROP TABLE IF EXISTS `sys_user`;
DROP TABLE IF EXISTS `department`;

-- ============================================
-- 核心基础表
-- ============================================

-- 1. 部门表（最先创建，因为用户表依赖它）
CREATE TABLE `department` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `dept_code` VARCHAR(32) NOT NULL UNIQUE COMMENT '部门编码',
  `dept_name` VARCHAR(128) NOT NULL COMMENT '部门名称',
  `parent_id` INT DEFAULT NULL COMMENT '父级部门ID',
  `manager_id` INT DEFAULT NULL COMMENT '部门经理ID（关联sys_user）',
  `status` TINYINT(1) DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_parent_id` (`parent_id`),
  INDEX `idx_manager_id` (`manager_id`)
) ENGINE=InnoDB COMMENT='部门表';

-- 2. 系统用户表（核心表）
CREATE TABLE `sys_user` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
  `password` VARCHAR(128) NOT NULL COMMENT '密码(BCrypt加密)',
  `real_name` VARCHAR(64) NOT NULL COMMENT '真实姓名',
  `role` TINYINT NOT NULL DEFAULT 0 COMMENT '角色：0-普通员工 1-部门经理 2-财务内审 9-IT管理员',
  `dept_id` INT DEFAULT NULL COMMENT '所属部门ID',
  `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  `phone` VARCHAR(32) DEFAULT NULL COMMENT '电话',
  `status` TINYINT(1) DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `last_login_at` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(64) DEFAULT NULL COMMENT '最后登录IP',
  `token` VARCHAR(256) DEFAULT NULL COMMENT '登录Token（单点登录用）',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_username` (`username`),
  INDEX `idx_dept_id` (`dept_id`),
  INDEX `idx_role` (`role`),
  FOREIGN KEY (`dept_id`) REFERENCES `department`(`id`)
) ENGINE=InnoDB COMMENT='系统用户表';

-- 3. 报销单主表
CREATE TABLE `expense_claim` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `claim_no` VARCHAR(32) NOT NULL UNIQUE COMMENT '报销单编号',
  `user_id` INT NOT NULL COMMENT '申请人ID',
  `dept_id` INT DEFAULT NULL COMMENT '部门ID',
  `title` VARCHAR(100) NOT NULL COMMENT '报销事由',
  `description` VARCHAR(1024) DEFAULT NULL COMMENT '报销事由详细说明',
  `amount` BIGINT NOT NULL COMMENT '金额(分)',
  `expense_date` DATE NOT NULL COMMENT '消费日期',
  `status` INT DEFAULT 0 COMMENT '状态：0草稿 1待部门经理审批 2待财务审计 3部门经理驳回 4已入账 5财务驳回',
  `audit_comment` VARCHAR(512) DEFAULT NULL COMMENT '审计意见',
  `risk_score` DECIMAL(5,2) DEFAULT NULL COMMENT '风险评分 0-100',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_dept_id` (`dept_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_expense_date` (`expense_date`),
  FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`),
  FOREIGN KEY (`dept_id`) REFERENCES `department`(`id`)
) ENGINE=InnoDB COMMENT='报销单主表';

-- 4. 审计日志表（核心：记录所有操作痕迹）
CREATE TABLE `audit_log` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `claim_id` INT DEFAULT NULL COMMENT '关联报销单ID',
  `operator_id` INT NOT NULL COMMENT '操作人ID',
  `action_type` VARCHAR(32) NOT NULL COMMENT '动作类型：SUBMIT/APPROVE/REJECT/AUDIT/MODIFY/DELETE',
  `action` VARCHAR(500) NOT NULL COMMENT '操作描述',
  `ip_address` VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
  `device_info` VARCHAR(256) DEFAULT NULL COMMENT '设备信息(User-Agent)',
  `before_value` TEXT DEFAULT NULL COMMENT '变更前值(JSON)',
  `after_value` TEXT DEFAULT NULL COMMENT '变更后值(JSON)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_claim_id` (`claim_id`),
  INDEX `idx_operator_id` (`operator_id`),
  INDEX `idx_action_type` (`action_type`),
  INDEX `idx_created_at` (`created_at`),
  FOREIGN KEY (`operator_id`) REFERENCES `sys_user`(`id`)
) ENGINE=InnoDB COMMENT='审计日志表';

-- ============================================
-- 业务扩展表
-- ============================================

-- 5. 费用类别表
CREATE TABLE `expense_category` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `code` VARCHAR(32) NOT NULL UNIQUE COMMENT '类别编码',
  `name` VARCHAR(64) NOT NULL COMMENT '类别名称',
  `parent_id` INT DEFAULT NULL COMMENT '父级ID',
  `max_amount_per_claim` DECIMAL(12,2) DEFAULT NULL COMMENT '单次报销上限',
  `require_invoice` TINYINT(1) DEFAULT 1 COMMENT '是否必须发票：0否 1是',
  `status` TINYINT(1) DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB COMMENT='费用类别表';

-- 6. 报销政策规则表
CREATE TABLE `expense_policy_rule` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `rule_code` VARCHAR(64) NOT NULL UNIQUE COMMENT '规则编码',
  `rule_name` VARCHAR(128) NOT NULL COMMENT '规则名称',
  `rule_type` VARCHAR(32) NOT NULL COMMENT '规则类型：AMOUNT_LIMIT/CATEGORY_LIMIT/TIME_LIMIT/BUDGET_CONTROL',
  `description` TEXT COMMENT '规则描述',
  `priority` INT DEFAULT 100 COMMENT '优先级，数字越小越优先',
  `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用：0禁用 1启用',
  `error_message` VARCHAR(256) DEFAULT NULL COMMENT '违规提示信息',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_rule_type` (`rule_type`),
  INDEX `idx_enabled` (`enabled`)
) ENGINE=InnoDB COMMENT='报销政策规则表';

-- 7. 规则条件表
CREATE TABLE `policy_rule_condition` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `rule_id` INT NOT NULL COMMENT '关联规则ID',
  `condition_type` VARCHAR(32) NOT NULL COMMENT '条件类型：AMOUNT_RANGE/CATEGORY/DEPARTMENT/TIME_RANGE',
  `operator` VARCHAR(16) NOT NULL COMMENT '操作符：EQ/NEQ/GT/LT/GTE/LTE/BETWEEN/IN',
  `field_name` VARCHAR(64) DEFAULT NULL COMMENT '字段名',
  `field_value` TEXT COMMENT '字段值(JSON数组或单值)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_rule_id` (`rule_id`),
  FOREIGN KEY (`rule_id`) REFERENCES `expense_policy_rule`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='规则条件表';

-- 8. 发票表
CREATE TABLE `invoice` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `invoice_number` VARCHAR(64) NOT NULL UNIQUE COMMENT '发票号码',
  `invoice_code` VARCHAR(64) NOT NULL COMMENT '发票代码',
  `invoice_type` VARCHAR(32) NOT NULL COMMENT '发票类型：VAT_NORMAL/VAT_SPECIAL/RECEIPT/OTHER',
  `amount` DECIMAL(12,2) NOT NULL COMMENT '发票金额',
  `tax_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '税额',
  `total_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '价税合计',
  `issue_date` DATE NOT NULL COMMENT '开票日期',
  `seller_name` VARCHAR(256) DEFAULT NULL COMMENT '销售方名称',
  `seller_tax_no` VARCHAR(64) DEFAULT NULL COMMENT '销售方税号',
  `buyer_name` VARCHAR(256) DEFAULT NULL COMMENT '购买方名称',
  `buyer_tax_no` VARCHAR(64) DEFAULT NULL COMMENT '购买方税号',
  `ocr_raw_text` TEXT COMMENT 'OCR原始识别文本',
  `ocr_confidence` DECIMAL(5,4) DEFAULT NULL COMMENT 'OCR识别置信度',
  `ocr_status` TINYINT(1) DEFAULT 0 COMMENT 'OCR状态：0待识别 1识别中 2成功 3失败',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_invoice_number` (`invoice_number`),
  INDEX `idx_invoice_code` (`invoice_code`),
  INDEX `idx_issue_date` (`issue_date`)
) ENGINE=InnoDB COMMENT='发票表';

-- 9. 发票电子存档表
CREATE TABLE `invoice_archive` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `invoice_id` INT NOT NULL COMMENT '关联发票ID',
  `file_path` VARCHAR(512) NOT NULL COMMENT '文件存储路径',
  `file_hash` VARCHAR(64) NOT NULL COMMENT '文件哈希(SHA-256)',
  `file_size` BIGINT NOT NULL COMMENT '文件大小(字节)',
  `mime_type` VARCHAR(64) NOT NULL COMMENT 'MIME类型',
  `archived_by` INT NOT NULL COMMENT '存档操作人',
  `archived_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_invoice_id` (`invoice_id`),
  FOREIGN KEY (`invoice_id`) REFERENCES `invoice`(`id`),
  FOREIGN KEY (`archived_by`) REFERENCES `sys_user`(`id`)
) ENGINE=InnoDB COMMENT='发票电子存档表';

-- 10. 风险预警表
CREATE TABLE `risk_alert` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `claim_id` INT NOT NULL COMMENT '关联报销单ID',
  `risk_type` VARCHAR(64) NOT NULL COMMENT '风险类型',
  `risk_level` TINYINT NOT NULL COMMENT '风险等级：1低 2中 3高 4严重',
  `risk_score` DECIMAL(5,2) DEFAULT NULL COMMENT '风险评分',
  `alert_message` VARCHAR(512) NOT NULL COMMENT '预警信息',
  `trigger_rules` TEXT COMMENT '触发的规则(JSON)',
  `handling_status` TINYINT(1) DEFAULT 0 COMMENT '处理状态：0待处理 1处理中 2已处理 3误报',
  `handled_by` INT DEFAULT NULL COMMENT '处理人',
  `handled_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  `handled_comment` VARCHAR(512) DEFAULT NULL COMMENT '处理备注',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_claim_id` (`claim_id`),
  INDEX `idx_risk_level` (`risk_level`),
  INDEX `idx_handling_status` (`handling_status`),
  FOREIGN KEY (`claim_id`) REFERENCES `expense_claim`(`id`),
  FOREIGN KEY (`handled_by`) REFERENCES `sys_user`(`id`)
) ENGINE=InnoDB COMMENT='风险预警表';

-- 11. 风险规则配置表
CREATE TABLE `risk_rule_config` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `rule_code` VARCHAR(64) NOT NULL UNIQUE COMMENT '规则编码',
  `rule_name` VARCHAR(128) NOT NULL COMMENT '规则名称',
  `risk_type` VARCHAR(64) NOT NULL COMMENT '风险类型',
  `risk_level` TINYINT NOT NULL COMMENT '风险等级',
  `weight` DECIMAL(5,2) DEFAULT 1.00 COMMENT '权重',
  `condition_expression` TEXT NOT NULL COMMENT '条件表达式(SpEL)',
  `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用：0禁用 1启用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_risk_type` (`risk_type`),
  INDEX `idx_enabled` (`enabled`)
) ENGINE=InnoDB COMMENT='风险规则配置表';

-- 12. 部门预算表
CREATE TABLE `department_budget` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `dept_id` INT NOT NULL COMMENT '部门ID',
  `budget_year` INT NOT NULL COMMENT '预算年度',
  `budget_amount` DECIMAL(14,2) NOT NULL COMMENT '预算总额',
  `used_amount` DECIMAL(14,2) DEFAULT 0 COMMENT '已使用金额',
  `frozen_amount` DECIMAL(14,2) DEFAULT 0 COMMENT '冻结金额',
  `version` INT DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_dept_year` (`dept_id`, `budget_year`),
  INDEX `idx_budget_year` (`budget_year`),
  FOREIGN KEY (`dept_id`) REFERENCES `department`(`id`)
) ENGINE=InnoDB COMMENT='部门预算表';

-- 13. 审计报告表
CREATE TABLE `audit_report` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `report_code` VARCHAR(64) NOT NULL UNIQUE COMMENT '报告编号',
  `report_type` VARCHAR(32) NOT NULL COMMENT '报告类型：COMPLIANCE/PROCESS/DATA_ANALYSIS',
  `report_period_start` DATE NOT NULL COMMENT '报告周期开始',
  `report_period_end` DATE NOT NULL COMMENT '报告周期结束',
  `summary` TEXT COMMENT '报告摘要',
  `findings` TEXT COMMENT '审计发现(JSON)',
  `recommendations` TEXT COMMENT '建议(JSON)',
  `generated_by` INT NOT NULL COMMENT '生成人',
  `status` TINYINT(1) DEFAULT 0 COMMENT '状态：0草稿 1已发布',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_report_type` (`report_type`),
  INDEX `idx_period` (`report_period_start`, `report_period_end`),
  FOREIGN KEY (`generated_by`) REFERENCES `sys_user`(`id`)
) ENGINE=InnoDB COMMENT='审计报告表';

-- 14. 报销单明细表
CREATE TABLE `expense_claim_detail` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `claim_id` INT NOT NULL COMMENT '报销单ID',
  `category_id` INT NOT NULL COMMENT '费用类别ID',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '费用描述',
  `amount` DECIMAL(12,2) NOT NULL COMMENT '金额',
  `invoice_id` INT DEFAULT NULL COMMENT '关联发票ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_claim_id` (`claim_id`),
  INDEX `idx_category_id` (`category_id`),
  FOREIGN KEY (`claim_id`) REFERENCES `expense_claim`(`id`),
  FOREIGN KEY (`category_id`) REFERENCES `expense_category`(`id`)
) ENGINE=InnoDB COMMENT='报销单明细表';

-- 15. 审批流程配置表
CREATE TABLE `approval_flow` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `flow_code` VARCHAR(64) NOT NULL UNIQUE COMMENT '流程编码',
  `flow_name` VARCHAR(128) NOT NULL COMMENT '流程名称',
  `flow_type` VARCHAR(32) NOT NULL COMMENT '流程类型：EXPENSE/APPROVAL/REJECT',
  `condition_expression` TEXT COMMENT '触发条件表达式',
  `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用：0禁用 1启用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_flow_type` (`flow_type`),
  INDEX `idx_enabled` (`enabled`)
) ENGINE=InnoDB COMMENT='审批流程配置表';

-- 16. 审批记录表（核心：记录审批痕迹）
CREATE TABLE `approval_record` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `claim_id` INT NOT NULL COMMENT '报销单ID',
  `flow_id` INT DEFAULT NULL COMMENT '流程ID',
  `approver_id` INT NOT NULL COMMENT '审批人ID',
  `approval_stage` TINYINT NOT NULL COMMENT '审批阶段：1-部门经理 2-财务审计',
  `before_status` INT DEFAULT NULL COMMENT '审批前状态',
  `after_status` INT DEFAULT NULL COMMENT '审批后状态',
  `action` VARCHAR(32) NOT NULL COMMENT '动作：APPROVE/REJECT/TRANSFER',
  `comment` TEXT COMMENT '审批意见',
  `ip_address` VARCHAR(64) DEFAULT NULL COMMENT '审批IP',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_claim_id` (`claim_id`),
  INDEX `idx_approver_id` (`approver_id`),
  INDEX `idx_approval_stage` (`approval_stage`),
  FOREIGN KEY (`claim_id`) REFERENCES `expense_claim`(`id`),
  FOREIGN KEY (`approver_id`) REFERENCES `sys_user`(`id`)
) ENGINE=InnoDB COMMENT='审批记录表';

-- ============================================
-- 初始化数据
-- ============================================

-- 初始化部门数据
INSERT INTO `department` (`dept_code`, `dept_name`) VALUES
('DEPT001', '研发部'),
('DEPT002', '市场部'),
('DEPT003', '财务部'),
('DEPT004', '人力资源部'),
('DEPT005', '行政部');

-- 初始化用户数据
-- 密码使用 BCrypt 加密，原始密码都是 123456
-- BCrypt hash generated by PasswordUtil
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `dept_id`) VALUES
('emp001', '$2a$10$WYj0ztt9hJF7zB16RlfdfeHgiQ0.HdZR0XAAbh9fc8rtJjZvYPRSK', '张三', 0, 1),
('emp002', '$2a$10$WYj0ztt9hJF7zB16RlfdfeHgiQ0.HdZR0XAAbh9fc8rtJjZvYPRSK', '小明', 0, 1),
('emp003', '$2a$10$WYj0ztt9hJF7zB16RlfdfeHgiQ0.HdZR0XAAbh9fc8rtJjZvYPRSK', '小红', 0, 2),
('mgr001', '$2a$10$WYj0ztt9hJF7zB16RlfdfeHgiQ0.HdZR0XAAbh9fc8rtJjZvYPRSK', '李四', 1, 1),
('mgr002', '$2a$10$WYj0ztt9hJF7zB16RlfdfeHgiQ0.HdZR0XAAbh9fc8rtJjZvYPRSK', '王经理', 1, 2),
('fin001', '$2a$10$WYj0ztt9hJF7zB16RlfdfeHgiQ0.HdZR0XAAbh9fc8rtJjZvYPRSK', '王五', 2, 3),
('fin002', '$2a$10$WYj0ztt9hJF7zB16RlfdfeHgiQ0.HdZR0XAAbh9fc8rtJjZvYPRSK', '赵财务', 2, 3),
('admin',  '$2a$10$WYj0ztt9hJF7zB16RlfdfeHgiQ0.HdZR0XAAbh9fc8rtJjZvYPRSK', '系统管理员', 9, NULL);

-- 更新部门经理ID
UPDATE `department` SET `manager_id` = 4 WHERE `dept_code` = 'DEPT001';  -- 李四是研发部经理
UPDATE `department` SET `manager_id` = 5 WHERE `dept_code` = 'DEPT002';  -- 王经理是市场部经理

-- 初始化费用类别数据
INSERT INTO `expense_category` (`code`, `name`, `max_amount_per_claim`, `require_invoice`) VALUES
('TRAVEL', '差旅费', 50000.00, 1),
('MEAL', '餐饮费', 5000.00, 1),
('OFFICE', '办公用品', 10000.00, 1),
('COMMUNICATION', '通讯费', 2000.00, 1),
('ENTERTAINMENT', '招待费', 10000.00, 1),
('TRAINING', '培训费', 50000.00, 1),
('OTHER', '其他费用', 5000.00, 1);

-- 初始化风险规则配置
INSERT INTO `risk_rule_config` (`rule_code`, `rule_name`, `risk_type`, `risk_level`, `weight`, `condition_expression`, `enabled`) VALUES
('RISK_WEEKEND', '周末报销异常', 'WEEKEND_CLAIM', 1, 1.0, 'dayOfWeek in [6,7]', 1),
('RISK_HIGH_AMOUNT', '高金额异常', 'HIGH_AMOUNT', 3, 1.5, 'amount > 50000', 1),
('RISK_FREQUENCY', '高频报销异常', 'HIGH_FREQUENCY', 2, 1.0, 'claimCountInWeek > 5', 1),
('RISK_BUDGET_EXCEED', '预算超支', 'BUDGET_EXCEED', 3, 1.5, 'usedAmount >= budgetAmount * 0.9', 1);

-- 初始化部门预算
INSERT INTO `department_budget` (`dept_id`, `budget_year`, `budget_amount`) VALUES
(1, 2026, 500000.00),
(2, 2026, 300000.00),
(3, 2026, 200000.00),
(4, 2026, 150000.00),
(5, 2026, 100000.00);

UPDATE risk_rule_config SET risk_level = 3, weight = 3.0 WHERE risk_type = 'HIGH_AMOUNT';
