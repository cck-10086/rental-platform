-- =====================================================
-- 增量升级脚本（2026-10）：规则引擎 + 风险偏好配置
-- 适用于已按旧版 schema.sql 建库的环境，执行一次即可。
-- 注意：库开启 ANSI_QUOTES 模式，字符串一律使用单引号。
-- =====================================================
USE rental_platform;

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
