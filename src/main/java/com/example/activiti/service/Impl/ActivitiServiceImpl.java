package com.example.activiti.service.Impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.activiti.generator.DefaultProcessDiagramGenerator;
import com.example.activiti.query.ActivitiTaskQuery;
import com.example.activiti.service.ActivitiService;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.EndEvent;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.image.ProcessDiagramGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

    @Autowired
    private ProcessEngine processEngine;

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

    @Override
    public void createActivitiImg(String processInstanceId, HttpServletResponse response) throws IOException {
        InputStream inputStream = getProcessDiagram(processInstanceId);
        response.setContentType("image/svg+xml;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + "a.svg");
        ServletOutputStream os = response.getOutputStream();
        int len;
        byte[] bytes = new byte[1024];
        while ((len = inputStream.read(bytes)) != -1) {
            os.write(bytes, 0, len);
        }
        os.flush();
        os.close();
    }


    /**
     * 获取流程图
     */
    public InputStream getProcessDiagram(String processInstanceId) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        // null check
        if (processInstance != null) {
            // get process model
            BpmnModel model = repositoryService.getBpmnModel(processInstance.getProcessDefinitionId());
            if (model != null && model.getLocationMap().size() > 0) {
                ProcessDiagramGenerator generator = new DefaultProcessDiagramGenerator();
                // 生成流程图 都不高亮
                return generator.generateDiagram(model, "宋体","宋体","宋体");
            }
        }
        return null;
    }

    @Override
    public void getFlowImgByInstanceId(String processInstanceId, HttpServletResponse response) throws IOException {
        InputStream imageStream = null;
        try {
            if (StringUtils.isEmpty(processInstanceId)) {
                return;
            }
            // 获取历史流程实例
            HistoricProcessInstance historicProcessInstance = historyService
                    .createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId).singleResult();
            // 获取流程中已经执行的节点，按照执行先后顺序排序
            List<HistoricActivityInstance> historicActivityInstances = historyService
                    .createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .orderByHistoricActivityInstanceId()
                    .asc().list();
            // 高亮已经执行流程节点ID集合
            List<String> highLightedActivitiIds = new ArrayList<>();
            for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
                // 用默认颜色
                highLightedActivitiIds.add(historicActivityInstance.getActivityId());
            }

            List<String> currIds = historicActivityInstances.stream()
                    .filter(item -> StringUtils.isEmpty(item.getEndTime()))
                    .map(HistoricActivityInstance::getActivityId).collect(Collectors.toList());

            // 获得流程引擎配置
            ProcessEngineConfiguration processEngineConfiguration = processEngine.getProcessEngineConfiguration();

            BpmnModel bpmnModel = repositoryService
                    .getBpmnModel(historicProcessInstance.getProcessDefinitionId());
            // 高亮流程已发生流转的线id集合
            List<String> highLightedFlowIds = getHighLightedFlows(bpmnModel, historicActivityInstances);

            imageStream = new DefaultProcessDiagramGenerator().generateDiagram(
                    bpmnModel,
                    "png",
                    //所有活动过的节点，包括当前在激活状态下的节点
                    highLightedActivitiIds,
                    //当前为激活状态下的节点
                    currIds,
                    //活动过的线
                    highLightedFlowIds,
                    "宋体",
                    "宋体",
                    "宋体",
                    processEngineConfiguration.getClassLoader(),
                    1.0);
            // 将图片文件转化为字节数组字符串，并对其进行Base64编码处理
            response.setContentType("image/svg+xml;charset=utf-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + "a.svg");
            ServletOutputStream os = response.getOutputStream();
            int len;
            byte[] bytes = new byte[1024];
            while ((len = imageStream.read(bytes)) != -1) {
                os.write(bytes, 0, len);
            }
            os.flush();
            os.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (imageStream != null) {
                try {
                    imageStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return;
    }

    /**
     *  获取已经流转的线
     *  @param bpmnModel
     * @param historicActivityInstances
     * @return
     */
    private static List<String> getHighLightedFlows(BpmnModel bpmnModel, List<HistoricActivityInstance> historicActivityInstances) {
        // 高亮流程已发生流转的线id集合
        List<String> highLightedFlowIds = new ArrayList<>();
        // 全部活动节点
        List<FlowNode> historicActivityNodes = new ArrayList<>();
        // 已完成的历史活动节点
        List<HistoricActivityInstance> finishedActivityInstances = new ArrayList<>();

        for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
            FlowNode flowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(historicActivityInstance.getActivityId(), true);
            historicActivityNodes.add(flowNode);
            if (historicActivityInstance.getEndTime() != null) {
                finishedActivityInstances.add(historicActivityInstance);
            }
        }

        FlowNode currentFlowNode = null;
        FlowNode targetFlowNode = null;
        // 遍历已完成的活动实例，从每个实例的outgoingFlows中找到已执行的
        for (HistoricActivityInstance currentActivityInstance : finishedActivityInstances) {
            // 获得当前活动对应的节点信息及outgoingFlows信息
            currentFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(currentActivityInstance.getActivityId(), true);
            List<SequenceFlow> sequenceFlows = currentFlowNode.getOutgoingFlows();

            /**
             * 遍历outgoingFlows并找到已已流转的 满足如下条件认为已已流转：
             * 1.当前节点是并行网关或兼容网关，则通过outgoingFlows能够在历史活动中找到的全部节点均为已流转
             * 2.当前节点是以上两种类型之外的，通过outgoingFlows查找到的时间最早的流转节点视为有效流转
             */
            if ("parallelGateway".equals(currentActivityInstance.getActivityType())
                    || "inclusiveGateway".equals(currentActivityInstance.getActivityType())) {
                // 遍历历史活动节点，找到匹配流程目标节点的
                for (SequenceFlow sequenceFlow : sequenceFlows) {
                    targetFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(sequenceFlow.getTargetRef(), true);
                    if (historicActivityNodes.contains(targetFlowNode)) {
                        highLightedFlowIds.add(sequenceFlow.getId());
                    }
                }
            } else {
                List<Map<String, Object>> tempMapList = new ArrayList<>();
                for (SequenceFlow sequenceFlow : sequenceFlows) {
                    for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
                        if (historicActivityInstance.getActivityId().equals(sequenceFlow.getTargetRef())) {
                            Map<String, Object> map = new HashMap<>();
                            map.put("highLightedFlowId", sequenceFlow.getId());
                            map.put("highLightedFlowStartTime", historicActivityInstance.getStartTime().getTime());
                            tempMapList.add(map);
                        }
                    }
                }

                if (!CollectionUtils.isEmpty(tempMapList)) {
                    // 遍历匹配的集合，取得开始时间最早的一个
                    long earliestStamp = 0L;
                    String highLightedFlowId = null;
                    for (Map<String, Object> map : tempMapList) {
                        long highLightedFlowStartTime = Long.valueOf(map.get("highLightedFlowStartTime").toString());
                        if (earliestStamp == 0 || earliestStamp == highLightedFlowStartTime) {
                            highLightedFlowId = map.get("highLightedFlowId").toString();
                            earliestStamp = highLightedFlowStartTime;
                        }
                    }

                    highLightedFlowIds.add(highLightedFlowId);
                }

            }

        }
        return highLightedFlowIds;
    }
}
