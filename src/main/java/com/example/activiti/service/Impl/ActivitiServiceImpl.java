package com.example.activiti.service.Impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.activiti.query.ActivitiTaskQuery;
import com.example.activiti.service.ActivitiService;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.EndEvent;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

/**
 * @Author syl
 * @Date 2022/7/29 9:19
 * @Description
 */
public class ActivitiServiceImpl implements ActivitiService {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Override
    public Page<Task> list(ActivitiTaskQuery query) {
        Page<Task> list = new Page<>();
        list.setCurrent(query.getPageNum().longValue());
        list.setSize(query.getPageSize().longValue());
        // 获取总数
        Long count = taskService.createTaskQuery()
                .processDefinitionKey(query.getKey())
                .taskAssignee(query.getId().toString())
                .count();
        list.setTotal(count);
        List<Task> tasks = taskService.createTaskQuery()
                .processDefinitionKey(query.getKey())
                .taskAssignee(query.getId().toString())
                .orderByTaskCreateTime()
                .desc()
                .listPage((query.getPageNum()-1)* query.getPageSize(),query.getPageSize());
        list.setRecords(tasks);
        return list;
    }

    @Override
    public Page<ProcessDefinition> listActiviti(Integer pageNum, Integer pageSize) {
        Page<ProcessDefinition> list = new Page<>();
        list.setCurrent(pageNum.longValue());
        list.setSize(pageSize.longValue());
        // 获取总数
        Long count = repositoryService.createProcessDefinitionQuery().count();
        List<ProcessDefinition> processDefinitionList = repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .listPage((pageNum-1)*pageSize,pageSize);
        list.setTotal(count);
        list.setRecords(processDefinitionList);
        return list;
    }

    @Override
    public void createByString(String bpmnName, String bpmn) {
        Deployment deployment = repositoryService.
                createDeployment()
                .addString(bpmnName + ".bpmn",bpmn)
                .deploy();
        System.out.println("部署ID" + deployment.getId());
        System.out.println("部署名称" + deployment.getName());
        System.out.println("部署时间：" + deployment.getDeploymentTime());
    }

    @Override
    public void createByUrl(String bpmnName, String bpmnUrl) {
        Deployment deployment = repositoryService
                .createDeployment()
                .name(bpmnName)
                .addClasspathResource(bpmnUrl)
                .deploy();//完成部署
        System.out.println("部署ID" + deployment.getId());
        System.out.println("部署名称" + deployment.getName());
        System.out.println("部署时间：" + deployment.getDeploymentTime());
    }

    @Override
    public void createByZipBar(MultipartFile file) throws IOException {
        InputStream in = file.getInputStream();
        ZipInputStream zipInputStream = new ZipInputStream(in);
        try {
            Deployment deployment = repositoryService
                    .createDeployment()
                    .addZipInputStream(zipInputStream)
                    .deploy();
            System.out.println("部署ID" + deployment.getId());
            System.out.println("部署名称" + deployment.getName());
            System.out.println("部署时间：" + deployment.getDeploymentTime());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void createByFile(MultipartFile file, String bpmnName) throws Exception {
        try {
            Deployment deployment = repositoryService
                    .createDeployment()
                    .name(bpmnName)
                    .addInputStream(bpmnName + ".bpmn", file.getInputStream())
                    .deploy();//完成部署
            System.out.println("部署ID" + deployment.getId());
            System.out.println("部署名称" + deployment.getName());
            System.out.println("部署时间：" + deployment.getDeploymentTime());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void start(String key, String businessKey, String userid, Map<String, Object> variables) {
        Authentication.setAuthenticatedUserId(userid);
        ProcessInstance instance = runtimeService
                .startProcessInstanceByKey(key, businessKey, variables);
        System.out.println("BusinessKey：" + instance.getBusinessKey());
    }

    @Override
    public void auditTask(String[] taskids, Map<String, Object> variables) {
        for (String taskid : taskids) {
            Task task = taskService.createTaskQuery().taskId(taskid).singleResult();
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult();
            variables = taskService.getVariables(taskid);
            // 批量
            taskService.setVariables(taskid,variables);
            // 单独
            taskService.setVariable(taskid,"key","value");
            System.out.println("BusinessKey：" + processInstance.getBusinessKey());
            taskService.complete(taskid);
        }
    }

    @Override
    public void transferAssignee(String taskId, String transferorId) {
        taskService.setAssignee(taskId, transferorId);
    }

    @Override
    public List<HistoricTaskInstance> history(String taskid) {
        Task task = taskService.createTaskQuery().taskId(taskid).singleResult();
        String processInstanceId = task.getProcessInstanceId();
        //  获取所有历史任务（按创建时间升序）
        List<HistoricTaskInstance> hisTaskList = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByTaskCreateTime()
                .asc()
                .list();
        if (CollectionUtils.isEmpty(hisTaskList) || hisTaskList.size() < 2) {
            return null;
        }
        return hisTaskList;
    }

    @Override
    public void backProcess(String taskId, String backProcessTaskId) throws Exception {
        if (backProcessTaskId==null) {
            throw new Exception("驳回目标节点ID为空！");
        }
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        String processInstanceId = task.getProcessInstanceId();

        //  获取所有历史任务（按创建时间升序）
        HistoricTaskInstance targetTask = historyService.createHistoricTaskInstanceQuery()
                .taskId(backProcessTaskId)
                .singleResult();

        HistoricTaskInstance currentTask = historyService.createHistoricTaskInstanceQuery()
                .taskId(taskId)
                .singleResult();

        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());

        //  获取第目标活动节点
        FlowNode startFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(targetTask.getTaskDefinitionKey());
        //  获取当前活动节点
        FlowNode currentFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(currentTask.getTaskDefinitionKey());

        //  临时保存当前活动的原始方向
        List<SequenceFlow> originalSequenceFlowList = new ArrayList<>();
        originalSequenceFlowList.addAll(currentFlowNode.getOutgoingFlows());
        //  清理活动方向
        currentFlowNode.getOutgoingFlows().clear();

        //  建立新方向
        SequenceFlow newSequenceFlow = new SequenceFlow();
        newSequenceFlow.setId("newSequenceFlowId");
        newSequenceFlow.setSourceFlowElement(currentFlowNode);
        newSequenceFlow.setTargetFlowElement(startFlowNode);
        List<SequenceFlow> newSequenceFlowList = new ArrayList<>();
        newSequenceFlowList.add(newSequenceFlow);
        //  当前节点指向新的方向
        currentFlowNode.setOutgoingFlows(newSequenceFlowList);

        //  完成当前任务
        taskService.complete(task.getId());

        //  重新查询当前任务
        Task nextTask = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        if (null != nextTask) {
            taskService.setAssignee(nextTask.getId(), targetTask.getAssignee());
        }

        //  恢复原始方向
        currentFlowNode.setOutgoingFlows(originalSequenceFlowList);
    }


    /**
     * 结束任务
     * @param taskId    当前任务ID
     */
    @Override
    public void endTask(String taskId) {
        //  当前任务
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        List endEventList = bpmnModel.getMainProcess().findFlowElementsOfType(EndEvent.class);
        FlowNode endFlowNode = (FlowNode) endEventList.get(0);
        FlowNode currentFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(task.getTaskDefinitionKey());

        //  临时保存当前活动的原始方向
        List originalSequenceFlowList = new ArrayList<>();
        originalSequenceFlowList.addAll(currentFlowNode.getOutgoingFlows());
        //  清理活动方向
        currentFlowNode.getOutgoingFlows().clear();

        //  建立新方向
        SequenceFlow newSequenceFlow = new SequenceFlow();
        newSequenceFlow.setId("newSequenceFlowId");
        newSequenceFlow.setSourceFlowElement(currentFlowNode);
        newSequenceFlow.setTargetFlowElement(endFlowNode);
        List newSequenceFlowList = new ArrayList<>();
        newSequenceFlowList.add(newSequenceFlow);
        //  当前节点指向新的方向
        currentFlowNode.setOutgoingFlows(newSequenceFlowList);

        //  完成当前任务
        taskService.complete(task.getId());

        //  可以不用恢复原始方向，不影响其它的流程
        //  currentFlowNode.setOutgoingFlows(originalSequenceFlowList);
    }
}
