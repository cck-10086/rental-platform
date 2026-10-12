package com.rental.service;

/**
 * 风险偏好配置服务：读写全局单行配置。
 */
public interface RiskConfigService {

    /** 获取当前风险偏好档位（无配置时返回 standard） */
    String getPreference();

    /** 更新风险偏好档位，仅接受 lenient/standard/strict */
    void updatePreference(String preference);
}
