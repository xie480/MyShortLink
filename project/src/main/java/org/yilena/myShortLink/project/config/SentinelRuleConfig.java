

package org.yilena.myShortLink.project.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
public class SentinelRuleConfig implements InitializingBean {

    /*
        这里我们就学习如何使用就好，拿并发量最高的转发路由接口为例
     */

    @Override
    public void afterPropertiesSet() throws Exception {
        List<FlowRule> rules = new ArrayList<>();
        FlowRule createOrderRule = new FlowRule();
        // 配置资源名称
        createOrderRule.setResource("restore_short-link");
        // 配置流控模式：根据每秒的请求数
        createOrderRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        // 每秒最多允许1次请求，这是为了测试方便，一般的话应该是根据机器配置来判断，均为10 - 1000不等
        createOrderRule.setCount(1);
        rules.add(createOrderRule);
        FlowRuleManager.loadRules(rules);
    }
}
