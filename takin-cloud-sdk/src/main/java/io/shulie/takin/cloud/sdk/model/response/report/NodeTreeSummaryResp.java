package io.shulie.takin.cloud.sdk.model.response.report;

import java.util.List;

import io.shulie.takin.cloud.ext.content.trace.ContextExt;
import io.shulie.takin.cloud.sdk.model.ScriptNodeSummaryBean;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author moriarty
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class NodeTreeSummaryResp extends ContextExt {
    private List<ScriptNodeSummaryBean> scriptNodeSummaryBeans;
}
