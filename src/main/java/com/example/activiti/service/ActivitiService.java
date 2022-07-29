package com.example.activiti.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.activiti.query.ActivitiTaskQuery;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.Task;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @Author syl
 * @Date 2022/7/29 9:20
 * @Description
 */
public interface ActivitiService {

    /**
     * 个人代办
     * @param query
     * @return
     */
    Page<Task> list(ActivitiTaskQuery query);

    /**
     * 流程列表
     * @param pageNum
     * @param pageSize
     * @return
     */
    Page<ProcessDefinition> listActiviti(Integer pageNum, Integer pageSize);

    /**
     * 新建流程根据字符串
     * @param bpmnName
     * @param bpmn
     */
    void createByString(String bpmnName, String bpmn);

    /**
     * 新建流程根据pathUrl
     * @param bpmnName
     * @param bpmnUrl
     */
    void createByUrl(String bpmnName, String bpmnUrl);

    /**
     * 新建流程根据文件(Zip/Bar)
     * @param file
     * @throws IOException
     */
    void createByZipBar(MultipartFile file) throws IOException;

    /**
     * 新建流程根据文件(BPMN/BPMN20.XML)
     * @param file
     * @param bpmnName
     * @throws Exception
     */
    void createByFile(MultipartFile file, String bpmnName) throws Exception;

    /**
     * 开始流程
     * @param key
     * @param businessKey
     * @param userid
     * @param variables
     */
    void start(String key, String businessKey, String userid, Map<String, Object> variables);

    /**
     * 通过流程 todo 后期修改成各种类型通用
     * @param taskids
     * @param variables
     */
    void auditTask(String[] taskids, Map<String, Object> variables);

    /**
     * 转让受理人
     * @param taskId
     * @param transferorId
     */
    void transferAssignee(String taskId, String transferorId);

    /**
     * 流程历史
     * @param taskid
     * @return
     */
    List<HistoricTaskInstance> history(String taskid);

    /**
     * 回退流程
     * @param taskId
     * @param backProcessTaskId
     * @throws Exception
     */
    void backProcess(String taskId, String backProcessTaskId) throws Exception;

    void endTask(String taskId);
}
