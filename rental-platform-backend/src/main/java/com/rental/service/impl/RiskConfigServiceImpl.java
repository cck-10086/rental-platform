package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rental.engine.RiskRuleEngine;
import com.rental.entity.RiskConfig;
import com.rental.mapper.RiskConfigMapper;
import com.rental.service.RiskConfigService;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class RiskConfigServiceImpl implements RiskConfigService {

    /** 合法档位集合 */
    private static final Set<String> VALID_PREFERENCES =
            Set.of(RiskRuleEngine.PREF_LENIENT, RiskRuleEngine.PREF_STANDARD, RiskRuleEngine.PREF_STRICT);

    private final RiskConfigMapper riskConfigMapper;

    public RiskConfigServiceImpl(RiskConfigMapper riskConfigMapper) {
        this.riskConfigMapper = riskConfigMapper;
    }

    @Override
    public String getPreference() {
        RiskConfig config = riskConfigMapper.selectOne(new LambdaQueryWrapper<RiskConfig>().last("LIMIT 1"));
        if (config == null || config.getPreference() == null || config.getPreference().isBlank()) {
            return RiskRuleEngine.PREF_STANDARD;
        }
        return config.getPreference();
    }

    @Override
    public void updatePreference(String preference) {
        if (preference == null || !VALID_PREFERENCES.contains(preference)) {
            throw new RuntimeException("无效的风险偏好档位，仅支持 lenient/standard/strict");
        }
        RiskConfig config = riskConfigMapper.selectOne(new LambdaQueryWrapper<RiskConfig>().last("LIMIT 1"));
        if (config == null) {
            config = new RiskConfig();
            config.setPreference(preference);
            riskConfigMapper.insert(config);
        } else {
            config.setPreference(preference);
            riskConfigMapper.updateById(config);
        }
    }
}
