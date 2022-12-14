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
        // ????????????
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
        // ????????????
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
        System.out.println("??????ID" + deployment.getId());
        System.out.println("????????????" + deployment.getName());
        System.out.println("???????????????" + deployment.getDeploymentTime());
    }

    @Override
    public void createByUrl(String bpmnName, String bpmnUrl) {
        Deployment deployment = repositoryService
                .createDeployment()
                .name(bpmnName)
                .addClasspathResource(bpmnUrl)
                .deploy();//????????????
        System.out.println("??????ID" + deployment.getId());
        System.out.println("????????????" + deployment.getName());
        System.out.println("???????????????" + deployment.getDeploymentTime());
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
            System.out.println("??????ID" + deployment.getId());
            System.out.println("????????????" + deployment.getName());
            System.out.println("???????????????" + deployment.getDeploymentTime());
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
                    .deploy();//????????????
            System.out.println("??????ID" + deployment.getId());
            System.out.println("????????????" + deployment.getName());
            System.out.println("???????????????" + deployment.getDeploymentTime());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void start(String key, String businessKey, String userid, Map<String, Object> variables) {
        Authentication.setAuthenticatedUserId(userid);
        ProcessInstance instance = runtimeService
                .startProcessInstanceByKey(key, businessKey, variables);
        System.out.println("BusinessKey???" + instance.getBusinessKey());
    }

    @Override
    public void auditTask(String[] taskids, Map<String, Object> variables) {
        for (String taskid : taskids) {
            Task task = taskService.createTaskQuery().taskId(taskid).singleResult();
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult();
            variables = taskService.getVariables(taskid);
            // ??????
            taskService.setVariables(taskid,variables);
            // ??????
            taskService.setVariable(taskid,"key","value");
            System.out.println("BusinessKey???" + processInstance.getBusinessKey());
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
        //  ???????????????????????????????????????????????????
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
            throw new Exception("??????????????????ID?????????");
        }
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        String processInstanceId = task.getProcessInstanceId();

        //  ???????????????????????????????????????????????????
        HistoricTaskInstance targetTask = historyService.createHistoricTaskInstanceQuery()
                .taskId(backProcessTaskId)
                .singleResult();

        HistoricTaskInstance currentTask = historyService.createHistoricTaskInstanceQuery()
                .taskId(taskId)
                .singleResult();

        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());

        //  ???????????????????????????
        FlowNode startFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(targetTask.getTaskDefinitionKey());
        //  ????????????????????????
        FlowNode currentFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(currentTask.getTaskDefinitionKey());

        //  ???????????????????????????????????????
        List<SequenceFlow> originalSequenceFlowList = new ArrayList<>();
        originalSequenceFlowList.addAll(currentFlowNode.getOutgoingFlows());
        //  ??????????????????
        currentFlowNode.getOutgoingFlows().clear();

        //  ???????????????
        SequenceFlow newSequenceFlow = new SequenceFlow();
        newSequenceFlow.setId("newSequenceFlowId");
        newSequenceFlow.setSourceFlowElement(currentFlowNode);
        newSequenceFlow.setTargetFlowElement(startFlowNode);
        List<SequenceFlow> newSequenceFlowList = new ArrayList<>();
        newSequenceFlowList.add(newSequenceFlow);
        //  ??????????????????????????????
        currentFlowNode.setOutgoingFlows(newSequenceFlowList);

        //  ??????????????????
        taskService.complete(task.getId());

        //  ????????????????????????
        Task nextTask = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        if (null != nextTask) {
            taskService.setAssignee(nextTask.getId(), targetTask.getAssignee());
        }

        //  ??????????????????
        currentFlowNode.setOutgoingFlows(originalSequenceFlowList);
    }


    /**
     * ????????????
     * @param taskId    ????????????ID
     */
    @Override
    public void endTask(String taskId) {
        //  ????????????
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        List endEventList = bpmnModel.getMainProcess().findFlowElementsOfType(EndEvent.class);
        FlowNode endFlowNode = (FlowNode) endEventList.get(0);
        FlowNode currentFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(task.getTaskDefinitionKey());

        //  ???????????????????????????????????????
        List originalSequenceFlowList = new ArrayList<>();
        originalSequenceFlowList.addAll(currentFlowNode.getOutgoingFlows());
        //  ??????????????????
        currentFlowNode.getOutgoingFlows().clear();

        //  ???????????????
        SequenceFlow newSequenceFlow = new SequenceFlow();
        newSequenceFlow.setId("newSequenceFlowId");
        newSequenceFlow.setSourceFlowElement(currentFlowNode);
        newSequenceFlow.setTargetFlowElement(endFlowNode);
        List newSequenceFlowList = new ArrayList<>();
        newSequenceFlowList.add(newSequenceFlow);
        //  ??????????????????????????????
        currentFlowNode.setOutgoingFlows(newSequenceFlowList);

        //  ??????????????????
        taskService.complete(task.getId());

        //  ?????????????????????????????????????????????????????????
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
     * ???????????????
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
                // ??????????????? ????????????
                return generator.generateDiagram(model, "??????","??????","??????");
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
            // ????????????????????????
            HistoricProcessInstance historicProcessInstance = historyService
                    .createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId).singleResult();
            // ?????????????????????????????????????????????????????????????????????
            List<HistoricActivityInstance> historicActivityInstances = historyService
                    .createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .orderByHistoricActivityInstanceId()
                    .asc().list();
            // ??????????????????????????????ID??????
            List<String> highLightedActivitiIds = new ArrayList<>();
            for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
                // ???????????????
                highLightedActivitiIds.add(historicActivityInstance.getActivityId());
            }

            List<String> currIds = historicActivityInstances.stream()
                    .filter(item -> StringUtils.isEmpty(item.getEndTime()))
                    .map(HistoricActivityInstance::getActivityId).collect(Collectors.toList());

            // ????????????????????????
            ProcessEngineConfiguration processEngineConfiguration = processEngine.getProcessEngineConfiguration();

            BpmnModel bpmnModel = repositoryService
                    .getBpmnModel(historicProcessInstance.getProcessDefinitionId());
            // ?????????????????????????????????id??????
            List<String> highLightedFlowIds = getHighLightedFlows(bpmnModel, historicActivityInstances);

            imageStream = new DefaultProcessDiagramGenerator().generateDiagram(
                    bpmnModel,
                    "png",
                    //??????????????????????????????????????????????????????????????????
                    highLightedActivitiIds,
                    //?????????????????????????????????
                    currIds,
                    //???????????????
                    highLightedFlowIds,
                    "??????",
                    "??????",
                    "??????",
                    processEngineConfiguration.getClassLoader(),
                    1.0);
            // ???????????????????????????????????????????????????????????????Base64????????????
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
     *  ????????????????????????
     *  @param bpmnModel
     * @param historicActivityInstances
     * @return
     */
    private static List<String> getHighLightedFlows(BpmnModel bpmnModel, List<HistoricActivityInstance> historicActivityInstances) {
        // ?????????????????????????????????id??????
        List<String> highLightedFlowIds = new ArrayList<>();
        // ??????????????????
        List<FlowNode> historicActivityNodes = new ArrayList<>();
        // ??????????????????????????????
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
        // ???????????????????????????????????????????????????outgoingFlows?????????????????????
        for (HistoricActivityInstance currentActivityInstance : finishedActivityInstances) {
            // ??????????????????????????????????????????outgoingFlows??????
            currentFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(currentActivityInstance.getActivityId(), true);
            List<SequenceFlow> sequenceFlows = currentFlowNode.getOutgoingFlows();

            /**
             * ??????outgoingFlows???????????????????????? ???????????????????????????????????????
             * 1.??????????????????????????????????????????????????????outgoingFlows????????????????????????????????????????????????????????????
             * 2.???????????????????????????????????????????????????outgoingFlows?????????????????????????????????????????????????????????
             */
            if ("parallelGateway".equals(currentActivityInstance.getActivityType())
                    || "inclusiveGateway".equals(currentActivityInstance.getActivityType())) {
                // ????????????????????????????????????????????????????????????
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
                    // ?????????????????????????????????????????????????????????
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
