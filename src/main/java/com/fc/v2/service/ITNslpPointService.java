package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.TNslpPoint;
import com.fc.v2.model.custom.LevelQuery;
import com.fc.v2.model.custom.LimitLineVerdict;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 声环境监测点位底册 Service接口
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITNslpPointService {

    /** 按主键查询 */
    TNslpPoint selectTNslpPointById(Long id);

    /** 按条件查询列表（分页由调用方统一处理） */
    List<TNslpPoint> selectTNslpPointList(Wrapper<TNslpPoint> queryWrapper);

    /** 新增 */
    int insertTNslpPoint(TNslpPoint record);

    /** 修改 */
    int updateTNslpPoint(TNslpPoint record);

    /** 批量删除 */
    int deleteTNslpPointByIds(String ids);

    /** 按主键删除 */
    int deleteTNslpPointById(Long id);

    /**
     * 点位归层（新方法承接，不动旧签名）：问一枚折算读数，走限值线那一份唯一算法。
     * 点位行上挂着线代号就按线代号取值，没挂走整表取优；
     * 归到 1..4 层就把开口线的线代号与起用那一一日钉进点位行两格；
     * 贴不上层或压根没有在位线，回话只有「不归层」，两格清成空白，不拿邻线数值凑答复。
     */
    LimitLineVerdict pinPointLevel(Long pointId, BigDecimal input, Date at, String objKind);

    /**
     * 一批点位一次回算、一次钉线：大屏昼段夜段折线、折线上的点值、后端逐条回算
     * 消费同一批发话（库只照一面，算法只走一遍）。问话里用 pointNo 认点位。
     */
    List<LimitLineVerdict> pinPointLevelBatch(List<LevelQuery> queries);

    /**
     * 看板上挂着线代号、回到 at 那一刻却寻不到在位版本的点位，一条条列出来
     * （撤下线照主键仍点得到，但不替它回话；挂码与在位版本对不上即列出）。
     */
    List<TNslpPoint> listPinnedOrphans(Date at);
}
