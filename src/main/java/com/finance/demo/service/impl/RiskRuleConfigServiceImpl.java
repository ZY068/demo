package com.finance.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.finance.demo.entity.RiskRuleConfig;
import com.finance.demo.mapper.RiskRuleConfigMapper;
import com.finance.demo.service.RiskRuleConfigService;
import org.springframework.stereotype.Service;

@Service
public class RiskRuleConfigServiceImpl extends ServiceImpl<RiskRuleConfigMapper, RiskRuleConfig> implements RiskRuleConfigService {
}
