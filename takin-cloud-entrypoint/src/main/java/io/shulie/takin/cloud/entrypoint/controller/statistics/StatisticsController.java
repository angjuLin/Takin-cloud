package io.shulie.takin.cloud.entrypoint.controller.statistics;

import java.util.List;

import javax.annotation.Resource;

import io.shulie.takin.cloud.biz.input.statistics.PressureTotalInput;
import io.shulie.takin.cloud.biz.output.statistics.PressureListTotalOutput;
import io.shulie.takin.cloud.biz.service.statistics.PressureStatisticsService;
import io.shulie.takin.cloud.entrypoint.convert.StatisticsConvert;
import io.shulie.takin.cloud.sdk.constant.EntrypointUrl;
import io.shulie.takin.cloud.sdk.model.request.statistics.PressureTotalReq;
import io.shulie.takin.cloud.sdk.model.response.statistics.PressureListTotalResp;
import io.shulie.takin.cloud.sdk.model.response.statistics.PressurePieTotalResp;
import io.shulie.takin.cloud.sdk.model.response.statistics.ReportTotalResp;
import io.shulie.takin.common.beans.response.ResponseResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 无涯
 * @date 2020/11/30 7:07 下午
 */
@RestController
@RequestMapping(EntrypointUrl.BASIC + "/" + EntrypointUrl.MODULE_STATISTICS)
public class StatisticsController {

    @Resource
    private PressureStatisticsService pressureStatisticsService;

    /**
     * 统计场景分类，返回饼状图数据
     *
     * @return -
     */
    @GetMapping(EntrypointUrl.METHOD_STATISTICS_PRESSURE_PIE_TOTAL)
    @ApiOperation("统计压测场景分类")
    public ResponseResult<PressurePieTotalResp> getPressurePieTotal(PressureTotalReq req) {
        PressureTotalInput input = new PressureTotalInput();
        input.setStartTime(req.getStartTime());
        input.setEndTime(req.getEndTime());
        PressurePieTotalResp output = pressureStatisticsService.getPressurePieTotal(input);
        return ResponseResult.success(output);
    }

    /**
     * 统计报告通过/未通过
     *
     * @return -
     */
    @GetMapping(EntrypointUrl.METHOD_STATISTICS_REPORT_TOTAL)
    @ApiOperation("统计报告通过以及未通过")
    public ResponseResult<ReportTotalResp> getReportTotal(PressureTotalReq req) {
        PressureTotalInput input = new PressureTotalInput();
        input.setStartTime(req.getStartTime());
        input.setEndTime(req.getEndTime());
        ReportTotalResp output = pressureStatisticsService.getReportTotal(input);
        return ResponseResult.success(output);
    }

    /**
     * 压测场景次数统计 && 压测脚本次数统计
     *
     * @return -
     */
    @PostMapping(EntrypointUrl.METHOD_STATISTICS_PRESSURE_LIST_TOTAL)
    @ApiOperation("统计压测场景次数以及压测脚本次数")
    public ResponseResult<List<PressureListTotalResp>> getPressureListTotal(@RequestBody PressureTotalReq req) {
        PressureTotalInput input = new PressureTotalInput();
        input.setStartTime(req.getStartTime());
        input.setEndTime(req.getEndTime());
        input.setScriptIds(req.getScriptIds());
        input.setType(req.getType());
        List<PressureListTotalOutput> output = pressureStatisticsService.getPressureListTotal(input);
        return ResponseResult.success(StatisticsConvert.of(output));
    }

}
