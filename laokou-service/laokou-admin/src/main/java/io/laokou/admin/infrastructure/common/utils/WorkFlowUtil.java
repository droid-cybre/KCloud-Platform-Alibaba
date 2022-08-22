package io.laokou.admin.infrastructure.common.utils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Sets;
import io.laokou.admin.application.service.SysMessageApplicationService;
import io.laokou.admin.interfaces.dto.MessageDTO;
import io.laokou.common.utils.HttpContextUtil;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.Execution;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
/**
 * @author Kou Shenhai
 * @version 1.0
 * @date 2022/8/19 0019 上午 9:23
 */
@Component
public class WorkFlowUtil {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private SysMessageApplicationService sysMessageApplicationService;

    public String getAuditUser(String definitionId,String executionId,String processInstanceId) {
        if (StringUtils.isBlank(executionId)) {
            final Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).active().singleResult();
            executionId = task.getExecutionId();
        }
        Execution execution = runtimeService.createExecutionQuery().executionId(executionId).singleResult();
        String activityId = execution.getActivityId();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(definitionId);
        FlowNode flowNode = (FlowNode) bpmnModel.getFlowElement(activityId);
        List<SequenceFlow> outFlows = flowNode.getOutgoingFlows();
        for (SequenceFlow sequenceFlow : outFlows) {
            FlowElement sourceFlowElement = sequenceFlow.getSourceFlowElement();
            final String json = JSON.toJSONString(sourceFlowElement);
            return JSONObject.parseObject(json).getString("assignee");
        }
        return null;
    }

    /**
     *
     * @param assignee
     * @param type
     * @param sendChannel
     */
    @Async
    public void sendAuditMsg(String assignee, Integer type, Integer sendChannel) {
        String title = "资源审批提醒";
        String content = String.format("编号为%s的资源需要您审批，请及时查看并处理","");
        Set set = Sets.newHashSet();
        set.add(assignee);
        MessageDTO dto = new MessageDTO();
        dto.setContent(content);
        dto.setTitle(title);
        dto.setSendChannel(sendChannel);
        dto.setReceiver(set);
        dto.setType(type);
        HttpServletRequest request = HttpContextUtil.getHttpServletRequest();
        sysMessageApplicationService.sendMessage(dto,request);
    }

}