package io.shulie.takin.cloud.data.param.report;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author 无涯
 * @date 2020/12/17 3:34 下午
 */
@Data
@Accessors(chain = true)
public class ReportQueryParam {
    private String endTime;
    /**
     * 状态:0就绪，1生成中，2已完成
     */
    private Integer status;
    /**
     * 是否已删除：0正常，1已删除
     */
    private Integer isDel;
}
