package io.shulie.takin.cloud.app.controller;

import javax.servlet.http.HttpServletRequest;

import io.shulie.takin.cloud.model.notify.ResourceExampleInfo;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Parameter;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.shulie.takin.cloud.app.util.IpUtils;
import io.shulie.takin.cloud.model.notify.Ack;
import io.shulie.takin.cloud.model.notify.Metrics;
import io.shulie.takin.cloud.app.service.JsonService;
import io.shulie.takin.cloud.constant.enums.EventType;
import io.shulie.takin.cloud.model.response.ApiResult;
import io.shulie.takin.cloud.app.entity.WatchmanEntity;
import io.shulie.takin.cloud.app.service.CommandService;
import io.shulie.takin.cloud.app.service.MetricsService;
import io.shulie.takin.cloud.app.service.WatchmanService;
import io.shulie.takin.cloud.model.notify.JobExampleStop;
import io.shulie.takin.cloud.model.notify.ResourceUpload;
import io.shulie.takin.cloud.app.service.JobExampleServer;
import io.shulie.takin.cloud.model.notify.JobExampleError;
import io.shulie.takin.cloud.model.notify.JobExampleStart;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.shulie.takin.cloud.model.notify.JobExampleHeartbeat;
import io.shulie.takin.cloud.model.notify.ResourceExampleStop;
import io.shulie.takin.cloud.model.notify.ResourceExampleError;
import io.shulie.takin.cloud.model.notify.ResourceExampleStart;
import io.shulie.takin.cloud.app.service.ResourceExampleService;
import io.shulie.takin.cloud.model.notify.ResourceExampleHeartbeat;

/**
 * 上报控制器
 *
 * @author <a href="mailto:472546172@qq.com">张天赐</a>
 */
@Slf4j
@RestController
public class NotifyController {

    @javax.annotation.Resource
    CommandService commandService;
    @javax.annotation.Resource
    MetricsService metricsService;
    @javax.annotation.Resource
    WatchmanService watchmanService;
    @javax.annotation.Resource
    JobExampleServer jobExampleServer;
    @javax.annotation.Resource
    ResourceExampleService resourceExampleService;
    @javax.annotation.Resource
    JsonService jsonService;

    /**
     * 所有事件全部回调到这里
     *
     * @return 回调响应
     */
    @PostMapping("/notify")
    @SuppressWarnings("AlibabaMethodTooLong")
    public ApiResult<?> index(
        @Parameter(description = "类型", required = true) @RequestParam Integer type,
        @Parameter(description = "关键词签名", required = true) @RequestParam String refSign,
        @RequestBody String content,
        HttpServletRequest request) {
        try {
            WatchmanEntity entity = watchmanService.ofRefSign(refSign);
            if (entity == null) {return ApiResult.fail("调度机未上报");}
            long watchmanId = entity.getId();
            EventType typeEnum = EventType.of(type);
            switch (typeEnum) {
                case WATCHMAN_UPLOAD: {
                    watchmanService.upload(watchmanId, jsonService.readValue(content, ResourceUpload.class));
                    break;
                }
                case WATCHMAN_HEARTBEAT: {
                    watchmanService.onHeartbeat(watchmanId);
                    break;
                }
                case WATCHMAN_NORMAL: {
                    watchmanService.onNormal(watchmanId);
                    break;
                }
                case WATCHMAN_ABNORMAL: {
                    watchmanService.onAbnormal(watchmanId, content);
                    break;
                }
                case RESOUECE_EXAMPLE_HEARTBEAT: {
                    ResourceExampleHeartbeat context = jsonService.readValue(content, ResourceExampleHeartbeat.class);
                    resourceExampleService.onHeartbeat(context.getData());
                    break;
                }
                case RESOUECE_EXAMPLE_START: {
                    ResourceExampleStart context = jsonService.readValue(content, ResourceExampleStart.class);
                    resourceExampleService.onStart(context.getData());
                    break;
                }
                case RESOUECE_EXAMPLE_STOP: {
                    ResourceExampleStop context = jsonService.readValue(content, ResourceExampleStop.class);
                    resourceExampleService.onStop(context.getData());
                    break;
                }
                case RESOUECE_EXAMPLE_ERROR: {
                    ResourceExampleError context = jsonService.readValue(content, ResourceExampleError.class);
                    resourceExampleService.onError(context.getData(), context.getMessage());
                    break;
                }
                case RESOUECE_EXAMPLE_INFO: {
                    ResourceExampleInfo context = jsonService.readValue(content, ResourceExampleInfo.class);
                    resourceExampleService.onInfo(context.getData(), context.getInfo());
                    break;
                }
                case JOB_EXAMPLE_HEARTBEAT: {
                    JobExampleHeartbeat context = jsonService.readValue(content, JobExampleHeartbeat.class);
                    jobExampleServer.onHeartbeat(context.getData());
                    break;
                }
                case JOB_EXAMPLE_START: {
                    JobExampleStart context = jsonService.readValue(content, JobExampleStart.class);
                    jobExampleServer.onStart(context.getData());
                    break;
                }
                case JOB_EXAMPLE_STOP: {
                    JobExampleStop context = jsonService.readValue(content, JobExampleStop.class);
                    jobExampleServer.onStop(context.getData());
                    break;
                }
                case JOB_EXAMPLE_ERROR: {
                    JobExampleError context = jsonService.readValue(content, JobExampleError.class);
                    jobExampleServer.onError(context.getData(), context.getMessage());
                    break;
                }
                case METRICS: {
                    Metrics context = jsonService.readValue(content, Metrics.class);
                    metricsService.upload(context.getJobExampleId(), context.getData(), IpUtils.getIp(request));
                    break;
                }
                case COMMAND_ACK: {
                    Ack ack = jsonService.readValue(content, Ack.class);
                    return ApiResult.success(commandService.ack(ack.getData(), ack.getContent()));
                }
                default: {
                    return ApiResult.fail("未识别的事件类型");
                }
            }
            return ApiResult.success();
        } catch (JsonProcessingException e) {
            log.error("事件上报失败.\n", e);
            return ApiResult.fail("JSON解析失败");
        } finally {
            log.info("事件上报信息:\n{}\n{}\n{}", type, refSign, content);
        }
    }
}
