package com.example.activiti.query;

import lombok.Data;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * @Author syl
 * @Date 2022/7/29 9:23
 * @Description
 */
@Data
@ApiModel(description="消息查询")
public class ActivitiTaskQuery {
    @ApiModelProperty(value = "页码", required = false)
    private Integer pageNum = 1;
    @ApiModelProperty(value = "页尺寸", required = false)
    private Integer pageSize = 10;
    @ApiModelProperty(value = "Key")
    private String key;
    @ApiModelProperty(value = "用户ID", hidden = true)
    private Long id;
}
