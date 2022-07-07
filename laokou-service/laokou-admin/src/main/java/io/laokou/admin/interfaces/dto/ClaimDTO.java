package io.laokou.admin.interfaces.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Kou Shenhai
 * @version 1.0
 * @date 2022/7/7 0007 下午 4:05
 */
@Data
public class ClaimDTO implements Serializable {
    /** 任务id */
    private String taskId;

}