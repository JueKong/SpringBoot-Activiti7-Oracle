package com.example.activiti.config.listener;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.springframework.stereotype.Component;

/**
 * @Description 流程结束监听器
 * @Date 22:44 2019/11/27
 * @Param
 * @return
 **/
@Component
public class ActivitiProcessCompleteListener implements ActivitiEventListener {

    @Override
    public void onEvent(ActivitiEvent activitiEvent) {
        System.out.println("部署流程ID：" + activitiEvent.getProcessDefinitionId());
        System.out.println("当前流程ID：" + activitiEvent.getProcessInstanceId());
        System.out.println("执行ID：" + activitiEvent.getExecutionId());
        System.out.println(activitiEvent);
        System.out.println("成功！");
        //todo
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }
}
