package io.shulie.takin.ext.helper;

import io.shulie.takin.ext.content.enginecall.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: liyuanba
 * @Date: 2021/11/3 9:37 上午
 */
public class DataConvertHelper {
    /**
     * 构建压测引擎的配置参数
     * @param request               启动参数
     * @param scriptFileDir         当前文档目录
     * @return 返回压测引擎的配置参数
     */
    public static EngineRunConfig buildEngineRunConfig(final ScheduleRunRequest request, final String scriptFileDir) {
        ScheduleStartRequestExt startRequest = request.getRequest();
        StrategyConfigExt strategyConfig = request.getStrategyConfig();
        Long sceneId = startRequest.getSceneId();
        Long taskId = startRequest.getTaskId();
        Long customerId = startRequest.getCustomerId();


        EngineRunConfig config = new EngineRunConfig();
        config.setSceneId(sceneId);
        config.setTaskId(taskId);
        config.setCustomerId(customerId);
        config.setConsoleUrl(startRequest.getConsole());
        config.setCallbackUrl(startRequest.getCallbackUrl());
        config.setPodCount(startRequest.getTotalIp());
        String scriptFile = CommonHelper.mergeDirPath(scriptFileDir, startRequest.getScriptPath());
        config.setScriptFile(scriptFile);
        config.setScriptFileDir(CommonHelper.mergeDirPath(scriptFileDir, File.separator));
        config.setPressureScene(startRequest.getPressureScene());
        config.setContinuedTime(startRequest.getContinuedTime());
        if (null != startRequest.getExpectThroughput()) {
            config.setExpectThroughput(startRequest.getExpectThroughput() / startRequest.getTotalIp());
        }
        if (CollectionUtils.isNotEmpty(startRequest.getDataFile())) {
            List<String> jarFiles = startRequest.getDataFile().stream().filter(Objects::nonNull)
                    .filter(o -> StringUtils.isNotBlank(o.getName()))
                    .filter(o -> o.getName().startsWith(".jar"))
                    .map(ScheduleStartRequestExt.DataFile::getPath)
                    .filter(StringUtils::isNotBlank)
                    .map(s -> CommonHelper.mergeDirPath(scriptFileDir, s))
                    .collect(Collectors.toList());
            config.setEnginePluginsFiles(jarFiles);
            startRequest.getDataFile().forEach(
                    dataFile -> dataFile.setPath(CommonHelper.mergeDirPath(scriptFileDir, dataFile.getPath()))
            );
            config.setFileSets(startRequest.getDataFile());
        }
        config.setBusinessMap(startRequest.getBusinessData());
        config.setBindByXpathMd5(startRequest.getBindByXpathMd5());
        config.setMemSetting(request.getMemSetting());

        EnginePressureConfig pressureConfig = new EnginePressureConfig();
        pressureConfig.setPressureEngineBackendQueueCapacity(request.getPressureEngineBackendQueueCapacity());
        pressureConfig.setEngineRedisAddress(request.getEngineRedisAddress());
        pressureConfig.setEngineRedisPort(request.getEngineRedisPort());
        pressureConfig.setEngineRedisSentinelNodes(request.getEngineRedisSentinelNodes());
        pressureConfig.setEngineRedisSentinelMaster(request.getEngineRedisSentinelMaster());
        pressureConfig.setEngineRedisPassword(request.getEngineRedisPassword());
        if (startRequest.isTryRun()) {
            pressureConfig.setFixedTimer(startRequest.getFixedTimer());
            pressureConfig.setLoopsNum(startRequest.getLoopsNum());
        }
        pressureConfig.setTraceSampling(request.getTraceSampling());
        pressureConfig.setPtlLogConfig(request.getPtlLogConfig());
        pressureConfig.setZkServers(request.getZkServers());
        pressureConfig.setLogQueueSize(request.getLogQueueSize());
        pressureConfig.setThreadGroupConfigMap(startRequest.getThreadGroupConfigMap());

        pressureConfig.setTotalTpsTargetLevel(startRequest.getTotalTps());
        pressureConfig.setTpsTargetLevel(startRequest.getTps());

        if (null != strategyConfig) {
            pressureConfig.setTpsThreadMode(strategyConfig.getTpsThreadMode());
            pressureConfig.setTpsTargetLevelFactor(strategyConfig.getTpsTargetLevelFactor());
            pressureConfig.setMaxThreadNum(strategyConfig.getTpsRealThreadNum());
        }
        config.setPressureConfig(pressureConfig);
        return config;
    }
}
