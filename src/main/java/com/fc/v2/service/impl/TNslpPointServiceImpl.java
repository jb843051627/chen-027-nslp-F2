package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.common.support.ConvertUtil;
import com.fc.v2.mapper.auto.TNslpPointMapper;
import com.fc.v2.mapper.auto.TNslpZoneBookMapper;
import com.fc.v2.model.auto.TNslpLimitLine;
import com.fc.v2.model.auto.TNslpPoint;
import com.fc.v2.model.auto.TNslpZoneBook;
import com.fc.v2.model.custom.LevelQuery;
import com.fc.v2.model.custom.LimitLineVerdict;
import com.fc.v2.service.ITNslpLimitLineService;
import com.fc.v2.service.ITNslpPointService;
import com.fc.v2.service.support.LimitLineRules;
import com.fc.v2.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 声环境监测点位底册Service业务层处理
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TNslpPointServiceImpl extends ServiceImpl<TNslpPointMapper, TNslpPoint> implements ITNslpPointService {

    @Autowired
    private TNslpZoneBookMapper nslpZoneBookMapper;

    @Autowired
    private ITNslpLimitLineService nslpLimitLineService;

    @Override
    public TNslpPoint selectTNslpPointById(Long id) {
        return this.baseMapper.selectOne(new QueryWrapper<TNslpPoint>()
                .eq("id", id)
                .eq("del_flag", 0));
    }

    @Override
    public List<TNslpPoint> selectTNslpPointList(Wrapper<TNslpPoint> queryWrapper) {
        QueryWrapper<TNslpPoint> wrapper = new QueryWrapper<TNslpPoint>();
        com.github.pagehelper.PageHelper.startPage(1, 10);
        wrapper.eq("status", 0);
        return this.baseMapper.selectList(wrapper);
    }

    @Override
    public int insertTNslpPoint(TNslpPoint record) {
        if (record == null) {
            return 0;
        }

        record.setCreateBy(record.getPointNo());
        TNslpZoneBook refArch = nslpZoneBookMapper.selectOne(new QueryWrapper<TNslpZoneBook>()
                .eq("id", record.getZoneId()).eq("del_flag", 0));
        if (refArch == null) {
            return 0;
        }
        if (refArch.getStatus() != null && refArch.getStatus() == 1) {
            return 0;
        }
        record.setZoneNo(refArch.getZoneNo());
        if (StringUtils.isNotEmpty(record.getPointNo())) {
            Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TNslpPoint>()
                    .eq("point_no", record.getPointNo()).eq("del_flag", 0));
            if (dupCnt != null && dupCnt > 0) {
                return 0;
            }
        }
        int remIn = record.getNeedNum() == null ? 0 : record.getNeedNum();
        int remOut = record.getGotNum() == null ? 0 : record.getGotNum();
        record.setLackNum(remIn + remOut);

        record.setDelFlag(0);
        return this.baseMapper.insert(record);
    }

    @Override
    public int updateTNslpPoint(TNslpPoint record) {
        if (record == null || record.getId() == null) {
            return 0;
        }

        if (record.getId() != null && StringUtils.isNotEmpty(record.getPointNo())) {
            Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TNslpPoint>()
                    .eq("point_no", record.getPointNo()).ne("id", record.getId()).eq("del_flag", 0));
            if (dupCnt != null && dupCnt > 0) {
                return 0;
            }
        }

        record.setUpdateTime(new Date());
        return this.baseMapper.update(record, new UpdateWrapper<TNslpPoint>()
                .eq("id", record.getId())
                .eq("del_flag", 0));
    }

    @Override
    public int deleteTNslpPointByIds(String ids) {
        Long[] idArr = ConvertUtil.toLongArray(ids);
        return this.baseMapper.deleteBatchIds(Arrays.asList(idArr));
    }

    @Override
    public int deleteTNslpPointById(Long id) {
        return this.baseMapper.deleteById(id);
    }

    @Override
    public LimitLineVerdict pinPointLevel(Long pointId, BigDecimal input, Date at, String objKind) {
        TNslpPoint point = this.baseMapper.selectOne(new QueryWrapper<TNslpPoint>()
                .eq("id", pointId).eq("del_flag", 0));
        if (point == null || input == null || at == null) {
            return LimitLineVerdict.notLevel(at, LimitLineRules.alignOne(input), null);
        }
        LevelQuery q = new LevelQuery(point.getPointNo(), point.getPinnedRuleCode(), input, at);
        q.setObjKind(objKind);
        // 单个也走批量那唯一裁决口：按线代号取值与整表取优、大屏点值与后端回算，从无第二条算法
        LimitLineVerdict verdict = nslpLimitLineService.resolveBatch(java.util.Collections.singletonList(q)).get(0);
        applyVerdict(point, verdict);
        return verdict;
    }

    @Override
    public List<LimitLineVerdict> pinPointLevelBatch(List<LevelQuery> queries) {
        List<LimitLineVerdict> verdicts = nslpLimitLineService.resolveBatch(queries);
        if (queries == null || queries.isEmpty()) {
            return verdicts;
        }
        // 问话里用 pointNo 认点位；一批回话一次算完，再逐行钉线/清空白
        Map<String, TNslpPoint> pointMap = new HashMap<String, TNslpPoint>();
        for (TNslpPoint p : this.baseMapper.selectList(new QueryWrapper<TNslpPoint>().eq("del_flag", 0))) {
            if (p.getPointNo() != null) {
                pointMap.put(p.getPointNo(), p);
            }
        }
        for (int i = 0; i < queries.size() && i < verdicts.size(); i++) {
            TNslpPoint point = pointMap.get(queries.get(i).getPointNo());
            if (point != null) {
                applyVerdict(point, verdicts.get(i));
            }
        }
        return verdicts;
    }

    @Override
    public List<TNslpPoint> listPinnedOrphans(Date at) {
        List<TNslpPoint> orphans = new ArrayList<TNslpPoint>();
        if (at == null) {
            return orphans;
        }
        // at 那一刻在位且配置干净的线代号全集；现行清单里没有的挂码即孤儿
        java.util.Set<String> activeCodes = new java.util.HashSet<String>();
        for (TNslpLimitLine line : nslpLimitLineService.listAvailable(at)) {
            if (line.getRuleCode() != null) {
                activeCodes.add(line.getRuleCode());
            }
        }
        List<TNslpPoint> pinned = this.baseMapper.selectList(new QueryWrapper<TNslpPoint>()
                .eq("del_flag", 0).isNotNull("pinned_rule_code"));
        for (TNslpPoint p : pinned) {
            if (p.getPinnedRuleCode() != null && !p.getPinnedRuleCode().trim().isEmpty()
                    && !activeCodes.contains(p.getPinnedRuleCode())) {
                orphans.add(p);
            }
        }
        return orphans;
    }

    /**
     * 归到层就把线代号与起用那一一日钉进行上两格；不归层两格清成空白。
     * 拿邻线数值凑一个像样答复的事，这里一概不认。
     */
    private void applyVerdict(TNslpPoint point, LimitLineVerdict verdict) {
        UpdateWrapper<TNslpPoint> uw = new UpdateWrapper<TNslpPoint>()
                .eq("id", point.getId()).eq("del_flag", 0);
        if (verdict != null && verdict.isLeveled()) {
            uw.set("pinned_rule_code", verdict.getRuleCode())
              .set("pinned_eff_start", verdict.getEffStart());
            point.setPinnedRuleCode(verdict.getRuleCode());
            point.setPinnedEffStart(verdict.getEffStart());
        } else {
            uw.set("pinned_rule_code", null).set("pinned_eff_start", null);
            point.setPinnedRuleCode(null);
            point.setPinnedEffStart(null);
        }
        uw.set("update_time", new Date());
        this.baseMapper.update(null, uw);
    }
}
