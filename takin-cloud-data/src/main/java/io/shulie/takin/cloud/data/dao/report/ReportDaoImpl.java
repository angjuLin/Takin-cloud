package io.shulie.takin.cloud.data.dao.report;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Calendar;

import javax.annotation.Resource;

import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.springframework.beans.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.apache.commons.collections4.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.shulie.takin.ext.content.script.ScriptNode;
import io.shulie.takin.ext.content.enums.NodeTypeEnum;
import io.shulie.takin.cloud.common.utils.JsonPathUtil;
import io.shulie.takin.cloud.data.model.mysql.ReportEntity;
import io.shulie.takin.cloud.data.mapper.mysql.ReportMapper;
import io.shulie.takin.cloud.common.constants.ReportConstants;
import io.shulie.takin.cloud.data.param.report.ReportQueryParam;
import io.shulie.takin.cloud.data.param.report.ReportUpdateParam;
import io.shulie.takin.cloud.data.param.report.ReportUpdateConclusionParam;
import io.shulie.takin.cloud.data.model.mysql.ReportBusinessActivityDetailEntity;
import io.shulie.takin.cloud.data.mapper.mysql.ReportBusinessActivityDetailMapper;

/**
 * @author 无涯
 * @date 2020/12/17 3:31 下午
 */
@Service
public class ReportDaoImpl implements ReportDao {

    @Resource
    private ReportMapper reportMapper;

    @Resource
    private ReportBusinessActivityDetailMapper detailMapper;

    @Override
    public int insert(ReportEntity entity) {
        if (Objects.nonNull(entity)) {
            Date insertDate = new Date();
            entity.setGmtCreate(insertDate);
            entity.setGmtUpdate(insertDate);
            entity.setStartTime(insertDate);
            return reportMapper.insert(entity);
        }
        return 0;
    }

    @Override
    public List<ReportEntity> queryReportList(ReportQueryParam param) {
        LambdaQueryWrapper<ReportEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(param.getEndTime())) {
            wrapper.le(ReportEntity::getGmtCreate, param.getEndTime());
        }
        if (null != param.getStatus()) {
            wrapper.eq(ReportEntity::getStatus, param.getStatus());
        }
        if (null != param.getIsDel()) {
            wrapper.eq(ReportEntity::getIsDeleted, param.getIsDel());
        }
        List<ReportEntity> entities = reportMapper.selectList(wrapper);
        if (entities != null && entities.size() > 0) {
            return entities;
        }
        return Lists.newArrayList();
    }

    @Override
    public ReportEntity selectById(Long id) {
        return reportMapper.selectById(id);
    }

    /**
     * 获取最新一条报告id
     *
     * @param sceneId 场景主键
     * @return -
     */
    @Override
    public ReportEntity getRecentlyReport(Long sceneId) {
        LambdaQueryWrapper<ReportEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportEntity::getSceneId, sceneId);
        wrapper.orderByDesc(ReportEntity::getId);
        wrapper.last("limit 1");
        List<ReportEntity> entities = reportMapper.selectList(wrapper);
        if (entities != null && entities.size() > 0) {
            return entities.get(0);
        }
        return null;
    }

    @Override
    public void updateReportConclusion(ReportUpdateConclusionParam param) {
        ReportEntity entity = new ReportEntity();
        BeanUtils.copyProperties(param, entity);
        reportMapper.updateById(entity);
    }

    @Override
    public void updateReport(ReportUpdateParam param) {
        ReportEntity entity = new ReportEntity();
        BeanUtils.copyProperties(param, entity);
        if (null == param.getGmtUpdate()) {
            entity.setGmtUpdate(Calendar.getInstance().getTime());
        }
        reportMapper.updateById(entity);
    }

    @Override
    public void finishReport(Long reportId) {
        ReportEntity entity = new ReportEntity();
        entity.setId(reportId);
        entity.setStatus(ReportConstants.FINISH_STATUS);
        entity.setGmtUpdate(new Date());
        reportMapper.updateById(entity);
    }

    @Override
    public void updateReportLock(Long resultId, Integer lock) {
        ReportEntity entity = new ReportEntity();
        entity.setId(resultId);
        entity.setLock(lock);
        entity.setGmtUpdate(new Date());
        reportMapper.updateById(entity);
    }

    @Override
    public ReportEntity getTempReportBySceneId(Long sceneId) {
        LambdaQueryWrapper<ReportEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportEntity::getSceneId, sceneId);
        wrapper.eq(ReportEntity::getIsDeleted, 0);
        wrapper.orderByDesc(ReportEntity::getId);
        wrapper.last("limit 1");
        List<ReportEntity> entities = reportMapper.selectList(wrapper);
        if (entities != null && entities.size() > 0) {
            return entities.get(0);
        }
        return null;
    }

    @Override
    public ReportEntity getReportBySceneId(Long sceneId) {
        LambdaQueryWrapper<ReportEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportEntity::getSceneId, sceneId);
        // 根据状态
        wrapper.eq(ReportEntity::getStatus, 0);
        wrapper.eq(ReportEntity::getIsDeleted, 0);
        wrapper.orderByDesc(ReportEntity::getId);
        wrapper.last("limit 1");
        List<ReportEntity> entities = reportMapper.selectList(wrapper);
        if (entities != null && entities.size() > 0) {
            return entities.get(0);
        }
        return null;
    }

    @Override
    public void updateReportStartTime(Long reportId, Date startTime) {
        ReportEntity entity = new ReportEntity();
        entity.setId(reportId);
        entity.setStartTime(startTime);
        entity.setGmtUpdate(new Date());
        reportMapper.updateById(entity);
    }

    /**
     * 根据场景主键设置压测报告状态
     *
     * @param sceneId 场景主键
     * @param status  状态值
     * @return 操作影响行数
     */
    @Override
    public int updateStatus(Long sceneId, Integer status) {
        return reportMapper.update(
            new ReportEntity() {{setStatus(status);}},
            Wrappers.lambdaQuery(ReportEntity.class).eq(ReportEntity::getSceneId, sceneId));
    }

    @Override
    public void updateReportEndTime(Long resultId, Date endTime) {
        ReportEntity entity = new ReportEntity();
        entity.setId(resultId);
        entity.setEndTime(endTime);
        entity.setGmtUpdate(new Date());
        reportMapper.updateById(entity);
    }

    @Override
    public ReportEntity getById(Long resultId) {
        if (resultId == null) {return null;}
        return reportMapper.selectById(resultId);
    }

    @Override
    public List<ReportBusinessActivityDetailEntity> getReportBusinessActivityDetailsByReportId(Long reportId,
        NodeTypeEnum nodeType) {
        LambdaQueryWrapper<ReportBusinessActivityDetailEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ReportBusinessActivityDetailEntity::getReportId, reportId);
        queryWrapper.eq(ReportBusinessActivityDetailEntity::getIsDeleted, 0);
        List<ReportBusinessActivityDetailEntity> entities = detailMapper.selectList(queryWrapper);
        if (Objects.isNull(nodeType)) {
            return entities;
        }
        ReportEntity reportEntity = reportMapper.selectById(reportId);
        if (Objects.nonNull(reportEntity) && StringUtils.isNotBlank(reportEntity.getScriptNodeTree())) {
            List<ScriptNode> nodeList = JsonPathUtil.getNodeListByType(reportEntity.getScriptNodeTree(), nodeType);
            if (CollectionUtils.isNotEmpty(nodeList)) {
                List<String> xpathMd5List = nodeList.stream().filter(Objects::nonNull)
                    .map(ScriptNode::getXpathMd5).collect(Collectors.toList());
                return entities.stream().filter(Objects::nonNull)
                    .filter(entity -> xpathMd5List.contains(entity.getBindRef()))
                    .collect(Collectors.toList());
            }
        }
        return null;
    }
}
