package com.example.activiti.config;

import com.example.activiti.config.listener.ActivitiProcessCompleteListener;
import com.google.common.collect.Lists;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.activiti.spring.boot.ProcessEngineConfigurationConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author syl
 * @Date 2022/7/29 12:15
 * @Description
 */

@Configuration
public class ActivitiConfig implements ProcessEngineConfigurationConfigurer {

    @Autowired
    private ActivitiProcessCompleteListener activitiProcessCompleteListener;

    @Override
    public void configure(SpringProcessEngineConfiguration configuration) {
        Map<String, List<ActivitiEventListener>> activitiEventListenerMap = new HashMap<>();
        activitiEventListenerMap.put(ActivitiEventType.PROCESS_COMPLETED.name(), Lists.newArrayList(activitiProcessCompleteListener));
        configuration.setTypedEventListeners(activitiEventListenerMap);
    }
}
