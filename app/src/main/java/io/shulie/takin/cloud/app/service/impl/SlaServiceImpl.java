package io.shulie.takin.cloud.app.service.impl;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import io.shulie.takin.cloud.model.callback.Sla;
import io.shulie.takin.cloud.data.entity.SlaEntity;
import io.shulie.takin.cloud.app.service.SlaService;
import io.shulie.takin.cloud.app.service.JsonService;
import io.shulie.takin.cloud.data.entity.PressureEntity;
import io.shulie.takin.cloud.data.entity.SlaEventEntity;
import io.shulie.takin.cloud.model.callback.Sla.SlaInfo;
import io.shulie.takin.cloud.app.service.PressureService;
import io.shulie.takin.cloud.app.service.ResourceService;
import io.shulie.takin.cloud.app.service.CallbackService;
import io.shulie.takin.cloud.constant.enums.CallbackType;
import io.shulie.takin.cloud.constant.enums.FormulaSymbol;
import io.shulie.takin.cloud.constant.enums.FormulaTarget;
import io.shulie.takin.cloud.data.service.SlaMapperService;
import io.shulie.takin.cloud.data.entity.PressureExampleEntity;
import io.shulie.takin.cloud.data.entity.ResourceExampleEntity;
import io.shulie.takin.cloud.data.service.SlaEventMapperService;
import io.shulie.takin.cloud.model.request.job.pressure.MetricsInfo;

/**
 * SLA服务 - 实例
 *
 * @author <a href="mailto:472546172@qq.com">张天赐</a>
 */
@Service
@Slf4j
public class SlaServiceImpl implements SlaService {
    @javax.annotation.Resource
    JsonService jsonService;
    @javax.annotation.Resource
    PressureService pressureService;
    @javax.annotation.Resource
    ResourceService resourceService;
    @javax.annotation.Resource
    CallbackService callbackService;
    @javax.annotation.Resource(name = "slaMapperServiceImpl")
    SlaMapperService slaMapper;
    @javax.annotation.Resource(name = "slaEventMapperServiceImpl")
    SlaEventMapperService slaEventMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SlaEntity> list(long pressureId) {
        return slaMapper.lambdaQuery().eq(SlaEntity::getPressureId, pressureId).list();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create(long pressureId, String ref, FormulaTarget target, FormulaSymbol symbol, double number) {
        slaMapper.save(new SlaEntity()
            .setRef(ref)
            .setPressureId(pressureId)
            .setFormulaNumber(number)
            .setFormulaTarget(target.getCode())
            .setFormulaSymbol(symbol.getCode())
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void event(Long pressureId, Long pressureExampleId, List<SlaEventEntity> slaEventEntityList) {
        if (CollUtil.isEmpty(slaEventEntityList)) {return;}
        slaEventMapper.saveBatch(slaEventEntityList);
        PressureEntity pressureEntity = pressureService.entity(pressureId);
        PressureExampleEntity pressureExampleEntity = pressureService.exampleEntity(pressureExampleId);
        ResourceExampleEntity resourceExampleEntity = resourceService.exampleEntity(pressureExampleEntity.getResourceExampleId());
        List<SlaInfo> slaInfoList = slaEventEntityList.stream().map(t -> new SlaInfo()
            .setPressureId(pressureId)
            .setRef(t.getRef())
            .setNumber(t.getNumber())
            .setAttach(t.getAttach())
            .setPressureExampleId(pressureExampleId)
            .setFormulaNumber(t.getFormulaNumber())
            .setFormulaSymbol(t.getFormulaSymbol())
            .setFormulaTarget(t.getFormulaTarget())
            .setResourceExampleId(resourceExampleEntity.getId())
            .setResourceId(resourceExampleEntity.getResourceId())
        ).collect(Collectors.toList());
        Sla sla = new Sla();
        sla.setTime(new Date());
        sla.setCallbackTime(new Date());
        sla.setData(slaInfoList);
        String slaString = jsonService.writeValueAsString(sla);
        // 创建回调
        callbackService.create(pressureEntity.getCallbackUrl(), CallbackType.SLA, slaString);
        log.info("SLA触发：{}", slaString);
    }

    @Override
    public List<SlaEventEntity> check(Long pressureId, Long pressureExampleId, List<MetricsInfo> metricsInfoList) {
        // 业务结果
        List<SlaEventEntity> result = new ArrayList<>();
        // 获取条件
        List<SlaEntity> slaEntityList = slaMapper.lambdaQuery().eq(SlaEntity::getPressureId, pressureId).list();
        // 逐个数据判断
        for (MetricsInfo metricsInfo : metricsInfoList) {
            // 对应的条件列表
            List<SlaEntity> conditionList = slaEntityList.stream()
                .filter(t -> "".equals(t.getRef()) || t.getRef().equals(metricsInfo.getTransaction()))
                .collect(Collectors.toList());
            // 逐个条件判断
            for (SlaEntity condition : conditionList) {
                FormulaSymbol formulaSymbol = FormulaSymbol.of(condition.getFormulaSymbol());
                FormulaTarget formulaTarget = FormulaTarget.of(condition.getFormulaTarget());
                Double compareResult = compare(metricsInfo, formulaTarget, formulaSymbol, condition.getFormulaNumber());
                // 符合校验则添加到业务结果里面
                if (compareResult != null) {
                    result.add(new SlaEventEntity()
                        .setPressureId(pressureId)
                        .setNumber(compareResult)
                        .setRef(metricsInfo.getTransaction())
                        .setSlaId(condition.getId())
                        .setPressureExampleId(pressureExampleId)
                        .setAttach(condition.getAttach())
                        .setFormulaNumber(condition.getFormulaNumber())
                        .setFormulaTarget(condition.getFormulaTarget())
                        .setFormulaSymbol(condition.getFormulaSymbol()));
                }
            }
        }
        return result;
    }

    /**
     * 算式是否成立
     *
     * @param info   数据源信息
     * @param target 算式目标
     * @param symbol 算式符号
     * @param value  比较值
     * @return 成立则返回算式目标的实际值<br />
     * 否则返回null
     */
    Double compare(MetricsInfo info, FormulaTarget target, FormulaSymbol symbol, double value) {
        Double targetValue = null;
        switch (target) {
            case RT:
                targetValue = info.getRt();
                break;
            case SA:
                targetValue = ((info.getSaCount() * 1.0) / info.getCount()) * 100;
                break;
            case TPS:
                targetValue = (info.getCount() * 1.0) / 5;
                break;
            case SUCCESS_RATE:
                targetValue = 100 - ((info.getFailCount() * 100.0) / info.getCount());
                break;
            default:
                return targetValue;
        }
        // 进行数值比较
        int compareResult = targetValue.compareTo(value);
        switch (symbol) {
            case EQUAL:
                targetValue = compareResult == 0 ? targetValue : null;
                break;
            case GREATER_THAN:
                targetValue = compareResult > 0 ? targetValue : null;
                break;
            case GREATER_THAN_OR_EQUAL_TO:
                targetValue = compareResult >= 0 ? targetValue : null;
                break;
            case LESS_THAN:
                targetValue = compareResult < 0 ? targetValue : null;
                break;
            case LESS_THAN_OR_EQUAL_TO:
                targetValue = compareResult <= 0 ? targetValue : null;
                break;
            default:
                return targetValue;
        }
        return targetValue;
    }
}
