package com.fc.v2.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TNslpCaseFlow;

/**
 * 噪声投诉与超标处置办件流转单 Service接口（state-machine 形状：单据流转，不提供增删改查全套）
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITNslpCaseFlowService {

    /** 按主键查询单据 */
    TNslpCaseFlow selectTNslpCaseFlowById(Long id);

    /** 列表查询（流转台账） */
    List<TNslpCaseFlow> selectTNslpCaseFlowList(QueryWrapper<TNslpCaseFlow> queryWrapper);

    /** 推进一档：返回更新后的单据；被拒返回 null */
    TNslpCaseFlow advance(Long id, String remark);

    /** 回退一档：返回更新后的单据；被拒返回 null */
    TNslpCaseFlow rollback(Long id, String remark);

    /** 修改内容：已办结不得再改，被拒返回 false */
    boolean updateContent(Long id, String remark);

    /** 删除单据：已办结不得删除，被拒返回 false */
    boolean remove(Long id);
}
