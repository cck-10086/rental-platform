-- =====================================================
-- 增量升级脚本（2026-10）：规则引擎 + 风险偏好配置 + 审查缓存
-- 适用于已按旧版 schema.sql 建库的环境，执行一次即可。
-- 注意：库开启 ANSI_QUOTES 模式，字符串一律使用单引号。
-- =====================================================
USE rental_platform;

-- 合同表：文件指纹（重复上传复用已有审查结果，不重复调用 AI）
ALTER TABLE contract ADD COLUMN file_hash VARCHAR(64) DEFAULT NULL COMMENT '文件SHA-256指纹(重复上传复用审查结果)';
ALTER TABLE contract ADD INDEX idx_file_hash (file_hash);

-- 审查记录表：规则引擎修正标记（可空，向后兼容）
ALTER TABLE review_record ADD COLUMN corrected_by VARCHAR(32) DEFAULT NULL COMMENT '定级修正来源: rule_engine-规则引擎修正';
ALTER TABLE review_record ADD COLUMN rule_reason VARCHAR(255) DEFAULT NULL COMMENT '规则引擎修正原因';

-- 风险偏好配置表（全局单行）
CREATE TABLE IF NOT EXISTS risk_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '配置ID',
    preference VARCHAR(16) NOT NULL DEFAULT 'standard' COMMENT '风险偏好: lenient-宽松/standard-标准/strict-严格',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险偏好配置表';

-- 写入默认配置行（表为空时）
INSERT INTO risk_config (preference)
SELECT 'standard' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM risk_config);

-- 审查结果反馈表（误报反馈闭环：用户对每条审查记录可标记 有用/误报）
CREATE TABLE IF NOT EXISTS review_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '反馈ID',
    record_id BIGINT NOT NULL COMMENT '审查记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    feedback VARCHAR(16) NOT NULL COMMENT '反馈类型: useful-有用/false_positive-误报',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '反馈时间',
    UNIQUE KEY uk_record_user (record_id, user_id),
    KEY idx_record_id (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审查结果反馈表';
