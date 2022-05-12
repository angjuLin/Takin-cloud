package io.shulie.takin.cloud.constant.enums;

import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;

import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * 通知事件类型
 *
 * @author <a href="mailto:472546172@qq.com">张天赐</a>
 */
@Getter
@AllArgsConstructor
public enum NotifyEventType {
    /**
     * 消除警告
     */
    WATCHMAN_HEARTBEAT(100, "调度心跳上报"),
    WATCHMAN_UPLOAD(101, "调度资源上报"),
    WATCHMAN_NORMAL(102, "调度正常"),
    WATCHMAN_ABNORMAL(103, "调度异常"),
    RESOUECE_EXAMPLE_HEARTBEAT(200, "资源实例心跳"),
    RESOUECE_EXAMPLE_START(201, "资源实例启动"),
    RESOUECE_EXAMPLE_STOP(202, "资源实例停止"),
    RESOUECE_EXAMPLE_ERROR(203, "资源实例发生异常"),
    RESOUECE_EXAMPLE_INFO(204, "资源实例信息"),
    JOB_EXAMPLE_HEARTBEAT(300, "任务实例心跳"),
    JOB_EXAMPLE_START(301, "任务实例启动"),
    JOB_EXAMPLE_STOP(302, "任务实例停止"),
    JOB_EXAMPLE_ERROR(303, "任务实例发生异常"),
    // 格式化用
    ;
    @Getter
    private final Integer code;
    private final String description;
    private static final Map<Integer, NotifyEventType> EXAMPLE_MAP = new HashMap<>(6);

    static {
        Arrays.stream(values()).forEach(t -> EXAMPLE_MAP.put(t.getCode(), t));
    }

    public static NotifyEventType of(Integer code) {
        return EXAMPLE_MAP.get(code);
    }

}
