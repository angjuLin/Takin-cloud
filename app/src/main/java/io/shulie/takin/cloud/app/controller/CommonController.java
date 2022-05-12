package io.shulie.takin.cloud.app.controller;

import java.util.Map;
import java.util.HashMap;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import io.shulie.takin.cloud.app.conf.WatchmanConfig;
import io.shulie.takin.cloud.model.response.ApiResult;

/**
 * 健康检查接口
 *
 * @author <a href="mailto:472546172@qq.com">张天赐</a>
 */
@Tag(name = "通用接口")
@RestController
@RequestMapping("/common")
public class CommonController {

    @javax.annotation.Resource
    WatchmanConfig watchmanConfig;

    @Operation(summary = "健康检查")
    @GetMapping("health/checkup")
    public ApiResult<Long> checkUp() {
        return ApiResult.success(System.currentTimeMillis());
    }

    @Operation(summary = "版本信息")
    @GetMapping("version")
    public ApiResult<Map<String, Object>> version() {
        Map<String, Object> result = new HashMap<>(2);
        result.put("version", watchmanConfig.getApplicationVersion());
        return ApiResult.success(result);
    }
}
