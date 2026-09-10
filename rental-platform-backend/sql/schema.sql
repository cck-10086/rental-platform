-- =====================================================
-- 租房辅助平台 - 数据库建表脚本
-- 引擎: InnoDB  字符集: utf8mb4
-- =====================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS rental_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE rental_platform;

-- ========================
-- 用户表
-- ========================
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色: user/admin',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1正常 0禁用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删除 1已删除',
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ========================
-- 合同表
-- ========================
CREATE TABLE IF NOT EXISTS `contract` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '合同ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `title` VARCHAR(200) DEFAULT NULL COMMENT '合同标题',
    `file_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT 'MinIO存储路径',
    `file_type` VARCHAR(20) DEFAULT NULL COMMENT '文件类型: pdf/docx/txt',
    `content` LONGTEXT DEFAULT NULL COMMENT '合同文本内容',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/processing/completed/failed',
    `risk_level` VARCHAR(20) DEFAULT NULL COMMENT '风险等级: 高风险/中风险/低风险',
    `risk_count` INT DEFAULT 0 COMMENT '风险条款数量',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同表';

-- ========================
-- 审查记录表
-- ========================
CREATE TABLE IF NOT EXISTS `review_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    `contract_id` BIGINT NOT NULL COMMENT '合同ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `clause_content` TEXT COMMENT '条款原文',
    `risk_type` VARCHAR(50) DEFAULT NULL COMMENT '风险类型',
    `risk_level` VARCHAR(20) DEFAULT NULL COMMENT '风险等级: 高风险/中风险/低风险',
    `risk_explanation` TEXT COMMENT '风险解读(大白话)',
    `suggestion` TEXT COMMENT '建议',
    `is_high_risk` TINYINT NOT NULL DEFAULT 0 COMMENT '是否高风险: 0否 1是',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY `idx_contract_id` (`contract_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审查记录表';

-- ========================
-- 房源表
-- ========================
CREATE TABLE IF NOT EXISTS `house` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '房源ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `title` VARCHAR(200) NOT NULL COMMENT '房源标题/名称',
    `address` VARCHAR(500) NOT NULL COMMENT '地址',
    `rent_price` DECIMAL(10,2) DEFAULT NULL COMMENT '月租金',
    `deposit` DECIMAL(10,2) DEFAULT NULL COMMENT '押金',
    `area` DOUBLE DEFAULT NULL COMMENT '面积(m²)',
    `rooms` INT DEFAULT NULL COMMENT '户型(室数)',
    `floor` VARCHAR(50) DEFAULT NULL COMMENT '楼层',
    `contact_name` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
    `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `images` TEXT COMMENT '图片URL(JSON数组)',
    `notes` TEXT COMMENT '备注',
    `status` VARCHAR(20) NOT NULL DEFAULT 'viewing' COMMENT '状态: viewing/signed/cancelled',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房源表';

-- ========================
-- 看房记录表
-- ========================
CREATE TABLE IF NOT EXISTS `house_viewing` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `house_id` BIGINT NOT NULL COMMENT '房源ID',
    `viewing_time` DATETIME DEFAULT NULL COMMENT '看房时间',
    `impression` TEXT COMMENT '总体印象',
    `pros` TEXT COMMENT '优点',
    `cons` TEXT COMMENT '缺点',
    `score` INT DEFAULT NULL COMMENT '评分(1-5)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_user_id` (`user_id`),
    KEY `idx_house_id` (`house_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='看房记录表';

-- ========================
-- 费用记录表
-- ========================
CREATE TABLE IF NOT EXISTS `expense` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `contract_id` BIGINT DEFAULT NULL,
    `expense_type` VARCHAR(20) NOT NULL COMMENT '费用类型: 水/电/燃气/物业/网络/其他',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '金额',
    `bill_month` VARCHAR(10) DEFAULT NULL COMMENT '账单月份(YYYY-MM)',
    `due_date` DATE DEFAULT NULL COMMENT '缴费截止日期',
    `is_paid` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已缴: 0未缴 1已缴',
    `paid_date` DATETIME DEFAULT NULL COMMENT '缴费时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    KEY `idx_user_id` (`user_id`),
    KEY `idx_contract_id` (`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费用记录表';

-- ========================
-- 维修报备表
-- ========================
CREATE TABLE IF NOT EXISTS `repair_request` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `contract_id` BIGINT DEFAULT NULL,
    `title` VARCHAR(200) NOT NULL COMMENT '维修标题',
    `description` TEXT COMMENT '问题描述',
    `images` TEXT COMMENT '图片URL(JSON数组)',
    `urgency` VARCHAR(10) NOT NULL DEFAULT '普通' COMMENT '紧急程度: 普通/紧急',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/processing/completed',
    `handler_note` TEXT COMMENT '处理备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='维修报备表';

-- ========================
-- 退租清单表
-- ========================
CREATE TABLE IF NOT EXISTS `move_out_checklist` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `contract_id` BIGINT DEFAULT NULL,
    `move_out_date` DATE DEFAULT NULL COMMENT '退租日期',
    `items` TEXT COMMENT '清单项目(JSON)',
    `total_deposit` DECIMAL(10,2) DEFAULT NULL COMMENT '押金总额',
    `deduction_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '扣除金额',
    `refund_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '应退金额',
    `notes` TEXT COMMENT '备注',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退租清单表';

-- ========================
-- AI对话记录表
-- ========================
CREATE TABLE IF NOT EXISTS `ai_conversation` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `question` TEXT NOT NULL COMMENT '用户问题',
    `answer` TEXT COMMENT 'AI回答',
    `conversation_id` VARCHAR(50) DEFAULT NULL COMMENT '会话ID(多轮对话)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_user_id` (`user_id`),
    KEY `idx_conversation_id` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话记录表';
