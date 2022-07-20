package io.laokou.admin.application.service.impl;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import io.laokou.admin.application.service.SysMessageApplicationService;
import io.laokou.admin.domain.sys.entity.SysMessageDO;
import io.laokou.admin.domain.sys.entity.SysMessageDetailDO;
import io.laokou.admin.domain.sys.repository.service.SysMessageDetailService;
import io.laokou.admin.domain.sys.repository.service.SysMessageService;
import io.laokou.admin.infrastructure.component.event.SaveMessageEvent;
import io.laokou.admin.infrastructure.component.pipeline.ProcessController;
import io.laokou.admin.infrastructure.component.run.Task;
import io.laokou.admin.infrastructure.component.run.TaskPendingHolder;
import io.laokou.admin.infrastructure.config.WebsocketStompServer;
import io.laokou.admin.interfaces.dto.MessageDTO;
import io.laokou.admin.interfaces.qo.MessageQO;
import io.laokou.admin.interfaces.vo.MessageDetailVO;
import io.laokou.admin.interfaces.vo.MessageVO;
import io.laokou.admin.interfaces.vo.MsgVO;
import io.laokou.common.constant.Constant;
import io.laokou.common.user.SecurityUser;
import io.laokou.common.utils.ConvertUtil;
import io.laokou.common.utils.SpringContextUtil;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
@Service
@Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRES_NEW)
public class SysMessageApplicationServiceImpl implements SysMessageApplicationService {

    @Autowired
    private WebsocketStompServer websocketStompServer;

    @Autowired
    private TaskPendingHolder taskPendingHolder;

    @Autowired
    private ProcessController processController;

    @Autowired
    private SysMessageService sysMessageService;

    @Autowired
    private SysMessageDetailService sysMessageDetailService;

    @Override
    public Boolean pushMessage(MessageDTO dto) {
        Iterator<String> iterator = dto.getReceiver().iterator();
        while (iterator.hasNext()) {
            String next = iterator.next();
            taskPendingHolder.route().execute(() -> websocketStompServer.oneToOne(next, MsgVO.builder().data(String.format("%s发来一条消息",dto.getUsername())).build()));
        }
        return true;
    }

    @Override
    public Boolean sendMessage(MessageDTO dto, HttpServletRequest request) {
        dto.setUsername(SecurityUser.getUsername(request));
        dto.setUserId(SecurityUser.getUserId(request));
        processController.process(dto);
        return true;
    }

    @Override
    public void consumeMessage(MessageDTO dto) {
        //1.插入日志
        SpringContextUtil.publishEvent(new SaveMessageEvent(dto));
        //2.推送消息
        Task task = SpringContextUtil.getBean(Task.class).setDto(dto);
        taskPendingHolder.route().execute(task);
    }

    @Override
    public Boolean insertMessage(MessageDTO dto) {
        SysMessageDO messageDO = ConvertUtil.sourceToTarget(dto, SysMessageDO.class);
        messageDO.setCreateDate(new Date());
        messageDO.setCreator(dto.getUserId());
        sysMessageService.save(messageDO);
        Iterator<String> iterator = dto.getReceiver().iterator();
        List<SysMessageDetailDO> detailDOList = Lists.newArrayList();
        while (iterator.hasNext()) {
            String next = iterator.next();
            SysMessageDetailDO detailDO = new SysMessageDetailDO();
            detailDO.setMessageId(messageDO.getId());
            detailDO.setUserId(Long.valueOf(next));
            detailDO.setCreateDate(new Date());
            detailDO.setCreator(detailDO.getUserId());
            detailDOList.add(detailDO);
        }
        if (CollectionUtils.isNotEmpty(detailDOList)) {
            sysMessageDetailService.saveBatch(detailDOList);
        }
        return true;
    }

    @Override
    public IPage<MessageVO> queryMessagePage(MessageQO qo) {
        IPage<MessageVO> page = new Page<>(qo.getPageNum(),qo.getPageSize());
        return sysMessageService.getMessageList(page,qo);
    }

    @Override
    public MessageDetailVO getMessageById(Long id) {
        return sysMessageService.getMessageById(id);
    }

    @Override
    public IPage<MessageVO> getUnReadList(HttpServletRequest request, MessageQO qo) {
        IPage<MessageVO> page = new Page<>(qo.getPageNum(),qo.getPageSize());
        final Long userId = SecurityUser.getUserId(request);
        return sysMessageService.getUnReadList(page,userId);
    }

    @Override
    public Boolean readMessage(Long id) {
        return sysMessageService.readMessage(id);
    }

    @Override
    public Integer unReadCount(HttpServletRequest request) {
        final Long userId = SecurityUser.getUserId(request);
        return sysMessageDetailService.count(Wrappers.lambdaQuery(SysMessageDetailDO.class).eq(SysMessageDetailDO::getUserId,userId)
                .eq(SysMessageDetailDO::getReadFlag, Constant.NO));
    }

}