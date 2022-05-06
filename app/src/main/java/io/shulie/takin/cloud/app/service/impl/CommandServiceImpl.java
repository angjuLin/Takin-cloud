package io.shulie.takin.cloud.app.service.impl;

import java.util.Map;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Objects;
import java.util.ArrayList;
import java.util.stream.Collectors;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.Page;
import cn.hutool.core.util.NumberUtil;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.PageHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;

import io.shulie.takin.cloud.app.entity.JobEntity;
import io.shulie.takin.cloud.app.service.JobService;
import io.shulie.takin.cloud.app.service.JsonService;
import io.shulie.takin.cloud.app.conf.WatchmanConfig;
import io.shulie.takin.cloud.app.entity.CommandEntity;
import io.shulie.takin.cloud.app.entity.MetricsEntity;
import io.shulie.takin.cloud.app.entity.ResourceEntity;
import io.shulie.takin.cloud.app.service.CommandService;
import io.shulie.takin.cloud.constant.enums.CommandType;
import io.shulie.takin.cloud.app.entity.JobExampleEntity;
import io.shulie.takin.cloud.app.service.ResourceService;
import io.shulie.takin.cloud.app.entity.ThreadConfigEntity;
import io.shulie.takin.cloud.constant.enums.ThreadGroupType;
import io.shulie.takin.cloud.app.entity.ResourceExampleEntity;
import io.shulie.takin.cloud.app.entity.ThreadConfigExampleEntity;
import io.shulie.takin.cloud.app.service.mapper.MetricsMapperService;
import io.shulie.takin.cloud.app.service.mapper.CommandMapperService;
import io.shulie.takin.cloud.app.service.mapper.ThreadConfigMapperService;
import io.shulie.takin.cloud.app.service.mapper.ThreadConfigExampleMapperService;

/**
 * 命令服务 - 实例
 *
 * @author <a href="mailto:472546172@qq.com">张天赐</a>
 */
@Slf4j
@Service
public class CommandServiceImpl implements CommandService {

    @Lazy
    @javax.annotation.Resource
    JobService jobService;
    @Lazy
    @javax.annotation.Resource
    ResourceService resourceService;
    @Lazy
    @javax.annotation.Resource
    MetricsMapperService metricsMapperService;

    @javax.annotation.Resource
    JsonService jsonService;
    @javax.annotation.Resource
    WatchmanConfig watchmanConfig;

    @javax.annotation.Resource
    CommandMapperService commandMapperService;
    @javax.annotation.Resource
    ThreadConfigMapperService threadConfigMapperService;
    @javax.annotation.Resource
    ThreadConfigExampleMapperService threadConfigExampleMapperService;

    /**
     * {@inheritDoc}
     */
    @Override
    public void graspResource(long resourceId) {
        // 获取资源
        ResourceEntity resourceEntity = resourceService.entity(resourceId);
        // 获取资源实例
        List<ResourceExampleEntity> resourceExampleEntityList = resourceService.listExample(resourceEntity.getId());
        // 组装命令内容
        List<HashMap<String, Object>> exampleList = resourceExampleEntityList.stream()
            .map(t -> new HashMap<String, Object>(16) {{
                put("type", 1);
                put("id", t.getId());
                put("cpu", t.getCpu());
                put("memory", t.getMemory());
                put("limitCpu", t.getLimitCpu());
                put("limitMemory", t.getLimitMemory());
                put("nfsDir", watchmanConfig.getNfsDirectory());
                put("nfsServer", watchmanConfig.getNfsServer());
                put("image", watchmanConfig.getContainerImage());
            }})
            .collect(Collectors.toList());
        // 补充index
        long minId = resourceExampleEntityList.stream().mapToLong(ResourceExampleEntity::getId).min().orElse(1);
        for (HashMap<String, Object> item : exampleList) {
            long id = Long.parseLong(item.get("id").toString());
            item.put("indexNumber", (id - minId) + 1);
        }
        // 组装数据
        HashMap<String, Object> content = new HashMap<String, Object>(2) {{
            put("example", exampleList);
            put("resourceId", resourceId);
        }};
        // 生成命令
        long commandId = create(resourceEntity.getWatchmanId(), CommandType.GRASP_RESOURCE, jsonService.writeValueAsString(content));
        log.info("下发命令:生成资源实例:{},命令主键{}.", resourceId, commandId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void releaseResource(long resourceId) {
        // 获取资源
        ResourceEntity resourceEntity = resourceService.entity(resourceId);
        HashMap<String, Object> content = new HashMap<String, Object>(1) {{
            put("resourceId", resourceEntity.getId());
        }};
        long commandId = create(resourceEntity.getWatchmanId(), CommandType.RELEASE_RESOURCE, jsonService.writeValueAsString(content));
        log.info("下发命令:释放资源:{},命令主键{}.", resourceId, commandId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startApplication(long jobId) {
        // TODO 实现
        // 不切分下发
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopApplication(long jobId) {
        // 获取任务
        JobEntity jobEntity = jobService.jobEntity(jobId);
        // 获取资源
        ResourceEntity resourceEntity = resourceService.entity(jobEntity.getResourceId());
        HashMap<String, Object> content = new HashMap<String, Object>(1) {{
            put("jobId", jobEntity.getId());
            put("resourceId", jobEntity.getResourceId());
        }};
        long commandId = create(resourceEntity.getWatchmanId(), CommandType.STOP_APPLICATION, jsonService.writeValueAsString(content));
        log.info("下发命令:停止任务:{},命令主键{}.", jobId, commandId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateConfig(long jobId) {
        // 获取任务
        JobEntity jobEntity = jobService.jobEntity(jobId);
        if (jobEntity == null) {throw new RuntimeException("未找到任务:" + jobId);}
        // 获取资源
        ResourceEntity resourceEntity = resourceService.entity(jobEntity.getResourceId());
        // 声明命令内容
        List<HashMap<String, Object>> content = new ArrayList<>();
        // 获取所有的线程配置实例
        List<ThreadConfigExampleEntity> threadConfigExampleEntityList = threadConfigExampleMapperService.lambdaQuery()
            .eq(ThreadConfigExampleEntity::getJobId, jobEntity.getId()).list();
        // 根据ref进行分组
        Map<String, List<ThreadConfigExampleEntity>> groupByRef = threadConfigExampleEntityList.stream().collect(Collectors.groupingBy(ThreadConfigExampleEntity::getRef));
        // 根据分组聚合数值
        groupByRef.forEach((k, v) -> {
            // 列出所有的项
            List<HashMap<String, String>> contextList = v.stream().map(t -> {
                try {
                    return jsonService.readValue(t.getContext(), new TypeReference<HashMap<String, String>>() {});
                } catch (JsonProcessingException e) {
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
            // 线程数
            int numberSum = contextList.stream().mapToInt(t -> NumberUtil.parseInt(t.getOrDefault("number", "0"))).sum();
            // TPS数
            double tpsSum = contextList.stream().mapToDouble(t -> NumberUtil.parseDouble(t.getOrDefault("tps", "0.0"))).sum();
            // 组装对象
            HashMap<String, Object> contentItem = new HashMap<String, Object>(3) {{
                put("ref", k);
                put("tps", tpsSum);
                put("number", numberSum);
            }};
            content.add(contentItem);
        });
        // 下发命令
        long commandId = create(resourceEntity.getWatchmanId(), CommandType.MODIFY_THREAD_CONFIG, jsonService.writeValueAsString(content));
        // 输出日志
        log.info("下发命令:更新线程组配置:{},命令主键{}.", jobId, commandId);
    }

    /**
     * 命令入库
     *
     * @param watchmanId  调度主键
     * @param commandType 命令类型
     * @param content     命令内容容
     * @return 命令主键
     */
    private long create(Long watchmanId, CommandType commandType, String content) {
        CommandEntity commandEntity = new CommandEntity() {{
            setContent(content);
            setType(commandType.getValue());
            setWatchmanId(watchmanId);
        }};
        commandMapperService.save(commandEntity);
        return commandEntity.getId();
    }

    /**
     * 命令确认
     *
     * @param id      命令主键
     * @param message ack内容
     */
    public boolean ack(long id, String type, String message) {
        HashMap<String, String> content = new HashMap<String, String>(2) {{
            put("type", type);
            put("message", message);
        }};
        return commandMapperService.lambdaUpdate()
            .set(CommandEntity::getAckContent, jsonService.writeValueAsString(content))
            .set(CommandEntity::getAckTime, new Date())
            .eq(CommandEntity::getId, id)
            .update();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageInfo<CommandEntity> range(long watchmanId, int number, CommandType type) {
        try (Page<?> ignored = PageHelper.startPage(1, number)) {
            List<CommandEntity> list = commandMapperService.lambdaQuery()
                .eq(type != null, CommandEntity::getType, type == null ? null : type.getValue())
                .eq(CommandEntity::getWatchmanId, watchmanId)
                .isNull(CommandEntity::getAckTime)
                .list();
            return new PageInfo<>(list);
        }
    }

    /**
     * 打包启动任务参数
     *
     * @param jobId 任务主键
     * @return 启动任务参数
     */
    @SuppressWarnings("AlibabaMethodTooLong")
    public String packageStartJob(long jobId) {
        // 任务
        JobEntity jobEntity = jobService.jobEntity(jobId);
        // 任务实例集合
        List<JobExampleEntity> jobExampleEntityList = jobService.jobExampleEntityList(jobId);
        // 线程组配置
        List<ThreadConfigEntity> threadConfigEntityList =
            threadConfigMapperService.lambdaQuery()
                .eq(ThreadConfigEntity::getJobId, jobId).list();
        // 压测指标配置
        List<MetricsEntity> metricsEntityList = metricsMapperService.lambdaQuery()
            .eq(MetricsEntity::getJobId, jobId).list();
        HashMap<String, Object> basicConfig = new HashMap<String, Object>(22) {{
            put("taskId", jobId);
            put("pressureType", jobEntity.getType());
            put("resourceId", jobEntity.getResourceId());
            put("continuedTime", jobEntity.getDuration());
            put("traceSampling", jobEntity.getSampling());
            put("zkServers", watchmanConfig.getZkAddress());
            put("memSetting", watchmanConfig.getJavaOptions());
            put("logQueueSize", watchmanConfig.getLogQueueSize());
            put("backendQueueCapacity", watchmanConfig.getBackendQueueCapacity());
            put("tpsTargetLevelFactor", watchmanConfig.getTpsTargetLevelFactor());
            put("businessMap", "");
            put("ptlLogConfig", "");
            put("dataFileList", null);
            put("threadGroupConfigMap", "");
            put("loopsNum", null);
            put("maxThreadNum", null);
            put("tpsThreadMode", null);
            put("bindByXpathMd5", null);
            put("tpsTargetLevel", null);
            put("expectThroughput", null);
            // 下面的应该不用填
            put("consoleUrl", null);
            put("customerId", null);
        }};
        // 压测指标配置
        {
            List<String> businessMap = new ArrayList<>();
            String template = FileUtil.readUtf8String("classpath:template/businessMap.json");
            metricsEntityList.forEach(t -> {
                HashMap<String, String> metrics = new HashMap<>(5);
                try {
                    HashMap<String, String> context = jsonService.readValue(t.getContext(), new TypeReference<HashMap<String, String>>() {});
                    metrics.putAll(context);
                    metrics.put("ref", context.get(t.getRef()));
                    metrics.put("rate", context.get("successRate"));
                    metrics.put("activityName", context.get(t.getRef()));
                } catch (JsonProcessingException e) {
                    log.error("JSON反序列化失败", e);
                }
                businessMap.add(StrUtil.format("\"{}\":{}", t.getRef(), StrUtil.format(template, metrics)));
            });
            basicConfig.put("businessMap", StrUtil.format("\\{{}\\}", String.join(",\n", businessMap)));
        }
        // 压测日志配置
        {
            String template = FileUtil.readUtf8String("classpath:template/ptlLogConfig.json");
            HashMap<String, Object> logConfig = new HashMap<String, Object>(6) {{
                put("logCutOff", watchmanConfig.getLogCutOff());
                put("ptlFileEnable", watchmanConfig.getPtlFileEnable());
                put("ptlUploadFrom", watchmanConfig.getPtlUploadFrom());
                put("ptlFileErrorOnly", watchmanConfig.getPtlFileErrorOnly());
                put("timeoutThreshold", watchmanConfig.getTimeoutThreshold());
                put("ptlFileTimeoutOnly", watchmanConfig.getPtlFileTimeoutOnly());
            }};
            basicConfig.put("ptlLogConfig", StrUtil.format(template, logConfig));
        }
        // 线程组配置
        {
            List<String> threadGroupConfigMap = new ArrayList<>();

            String template = FileUtil.readUtf8String("classpath:template/threadGroupConfigMap.json");

            threadConfigEntityList.forEach(t -> {
                try {
                    HashMap<String, String> context = jsonService.readValue(t.getContext(), new TypeReference<HashMap<String, String>>() {});
                    ThreadGroupType threadGroupType = ThreadGroupType.of(t.getMode());
                    HashMap<String, Object> threadConfig = new HashMap<String, Object>(6) {{
                        put("rampUpUnit", "s");
                        put("estimateFlow", null);
                        put("steps", context.get("step"));
                        put("type", threadGroupType.getType());
                        put("mode", threadGroupType.getModel());
                        put("threadNum", context.get("number"));
                        put("rampUp", context.get("growthTime"));
                    }};
                    threadGroupConfigMap.add(StrUtil.format("\"{}\":{}", t.getRef(), StrUtil.format(template, threadConfig)));
                } catch (JsonProcessingException e) {
                    log.error("JSON反序列化失败", e);
                }
            });
            basicConfig.put("threadGroupConfigMap", StrUtil.format("\\{{}\\}", String.join(",\n", threadGroupConfigMap)));
        }

        // 全部拼装
        String template = FileUtil.readUtf8String("classpath:template/basic.json");
        return StrUtil.format(template, basicConfig);
    }
}
