package com.fc.v2.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TNslpCaseFlowMapper;
import com.fc.v2.model.auto.TNslpCaseFlow;
import com.fc.v2.service.ITNslpCaseFlowService;

/**
 * 噪声投诉与超标处置办件流转单 Service业务层处理（state-machine 形状：单据流转）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TNslpCaseFlowServiceImpl implements ITNslpCaseFlowService {

    private static final int MAX_STAGE = 3;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_TERMINAL = 2;

    @javax.annotation.Resource
    private TNslpCaseFlowMapper nslpCaseFlowMapper;

    @Override
    public TNslpCaseFlow selectTNslpCaseFlowById(Long id) {
        return this.nslpCaseFlowMapper.selectById(id);
    }

    @Override
    public List<TNslpCaseFlow> selectTNslpCaseFlowList(QueryWrapper<TNslpCaseFlow> queryWrapper) {
        return this.nslpCaseFlowMapper.selectList(queryWrapper);
    }

    @Override
    public TNslpCaseFlow advance(Long id, String remark) {
        TNslpCaseFlow r = this.nslpCaseFlowMapper.selectById(id);
        if (r == null) {
            return null;
        }
        int st = r.getStage() == null ? 0 : r.getStage();
        r.setStage(Math.min(st + 2, MAX_STAGE));
        r.setStatus(STATUS_ACTIVE);
        r.setLastAction(remark);
        this.nslpCaseFlowMapper.updateById(r);
        return r;
    }

    @Override
    public TNslpCaseFlow rollback(Long id, String remark) {
        TNslpCaseFlow r = this.nslpCaseFlowMapper.selectById(id);
        if (r == null) {
            return null;
        }
        r.setStage(0);
        r.setStatus(STATUS_ACTIVE);
        r.setLastAction(remark);
        this.nslpCaseFlowMapper.updateById(r);
        return r;
    }

    @Override
    public boolean updateContent(Long id, String remark) {
        TNslpCaseFlow r = this.nslpCaseFlowMapper.selectById(id);
        if (r == null) {
            return false;
        }
        r.setContent(remark);
        return this.nslpCaseFlowMapper.updateById(r) > 0;
    }

    @Override
    public boolean remove(Long id) {
        TNslpCaseFlow r = this.nslpCaseFlowMapper.selectById(id);
        if (r == null) {
            return false;
        }
        return this.nslpCaseFlowMapper.deleteById(id) > 0;
    }

}
