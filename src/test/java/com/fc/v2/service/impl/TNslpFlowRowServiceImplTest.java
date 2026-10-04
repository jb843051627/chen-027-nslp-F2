package com.fc.v2.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.exception.FlowReceiveException;
import com.fc.v2.mapper.auto.TNslpFlowBatchMapper;
import com.fc.v2.mapper.auto.TNslpFlowRowMapper;
import com.fc.v2.mapper.auto.TNslpPointMapper;
import com.fc.v2.model.auto.TNslpFlowBatch;
import com.fc.v2.model.auto.TNslpFlowRow;
import com.fc.v2.model.auto.TNslpPoint;
import com.fc.v2.model.custom.FlowBatchSubmit;
import com.fc.v2.model.custom.FlowReceipt;
import com.fc.v2.model.custom.FlowRowCrossCheck;
import com.fc.v2.service.support.FlowReceiveRules;

/**
 * 核收四动作服务口径测试（内存库）：
 * 点行数与顶存抬头、看数据（缺数/切分/只认码）、定去处（五条退回+同点两行各判）、
 * 回执三数行里数、同批次码第二遍照面、未勾满两年度转历史册层。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TNslpFlowRowServiceImplTest {

    @Mock
    private TNslpFlowRowMapper flowRowMapper;
    @Mock
    private TNslpFlowBatchMapper flowBatchMapper;
    @Mock
    private TNslpPointMapper pointMapper;
    @InjectMocks
    private TNslpFlowRowServiceImpl service;

    private static final SimpleDateFormat F = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final String STATION = "自动站直采";
    private static final String VEHICLE = "监测车补报";

    private static Date t(String s) {
        try {
            return F.parse(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** 内存行库 */
    private List<TNslpFlowRow> rows;
    /** 内存拨抬头库 */
    private List<TNslpFlowBatch> batches;
    /** 内存点位底册 */
    private List<TNslpPoint> points;
    private long idSeq = 1000L;

    @BeforeEach
    public void setUp() {
        rows = new ArrayList<TNslpFlowRow>();
        batches = new ArrayList<TNslpFlowBatch>();
        points = new ArrayList<TNslpPoint>();

        // 底册两个在册点位，核定上限都为 10
        points.add(point("P0001", "QN00", "临江一号点位", 10));
        points.add(point("P0002", "QN00", "临江二号点位", 10));

        when(flowBatchMapper.selectOne(any(QueryWrapper.class))).thenAnswer(inv -> {
            Map<String, Object> eq = eqValues(inv.getArgument(0));
            String no = (String) eq.get("batch_no");
            for (TNslpFlowBatch b : batches) {
                if (no != null && no.equals(b.getBatchNo()) && (b.getDelFlag() == null || b.getDelFlag() == 0)) {
                    return b;
                }
            }
            return null;
        });
        when(flowBatchMapper.insert(any(TNslpFlowBatch.class))).thenAnswer(inv -> {
            TNslpFlowBatch b = inv.getArgument(0);
            if (b.getDelFlag() == null) {
                b.setDelFlag(0);
            }
            batches.add(b);
            return 1;
        });
        when(flowBatchMapper.updateById(any(TNslpFlowBatch.class))).thenAnswer(inv -> {
            TNslpFlowBatch b = inv.getArgument(0);
            for (int i = 0; i < batches.size(); i++) {
                if (batches.get(i).getBatchNo().equals(b.getBatchNo())) {
                    batches.set(i, b);
                }
            }
            return 1;
        });

        when(flowRowMapper.insert(any(TNslpFlowRow.class))).thenAnswer(inv -> {
            TNslpFlowRow r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(Long.valueOf(++idSeq));
            }
            rows.add(r);
            return 1;
        });
        when(flowRowMapper.updateById(any(TNslpFlowRow.class))).thenAnswer(inv -> {
            TNslpFlowRow r = inv.getArgument(0);
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).getId().equals(r.getId())) {
                    rows.set(i, r);
                }
            }
            return 1;
        });
        // 显式 UpdateWrapper（清空白字段走这条）：把 set 列逐项映回内存行
        when(flowRowMapper.update(any(), any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenAnswer(inv -> {
                    com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<TNslpFlowRow> uw =
                            inv.getArgument(1);
                    Map<String, Object> where = eqValues(uw);
                    Long id = (Long) where.get("id");
                    Map<String, Object> sets = setValues(uw);
                    for (TNslpFlowRow r : rows) {
                        if (r.getId().equals(id)) {
                            applySets(r, sets);
                        }
                    }
                    return 1;
                });
        when(flowRowMapper.selectById(any())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            for (TNslpFlowRow r : rows) {
                if (r.getId().equals(id)) {
                    return r;
                }
            }
            return null;
        });
        when(flowRowMapper.selectList(any(QueryWrapper.class))).thenAnswer(inv -> {
            QueryWrapper<?> w = inv.getArgument(0);
            Map<String, Object> eq = eqValues(w);
            List<TNslpFlowRow> hit = new ArrayList<TNslpFlowRow>();
            // 历史册层收口那一路：status=0 且 create_time <= deadline
            boolean archiveQuery = eq.containsKey("status") && eq.containsKey("create_time");
            for (TNslpFlowRow r : rows) {
                if (r.getDelFlag() != null && r.getDelFlag() != 0) {
                    continue;
                }
                if (archiveQuery) {
                    if (r.getStatus() == null || r.getStatus() != (Integer) eq.get("status")) {
                        continue;
                    }
                    if (r.getLayer() == null || !eq.get("layer").equals(r.getLayer())) {
                        continue;
                    }
                    if (r.getCreateTime() == null
                            || r.getCreateTime().after((Date) eq.get("create_time"))) {
                        continue;
                    }
                    hit.add(r);
                    continue;
                }
                if (eq.containsKey("batch_no") && !eq.get("batch_no").equals(r.getBatchNo())) {
                    continue;
                }
                if (eq.containsKey("layer") && !eq.get("layer").equals(r.getLayer())) {
                    continue;
                }
                if (eq.containsKey("status") && !eq.get("status").equals(r.getStatus())) {
                    continue;
                }
                hit.add(r);
            }
            hit.sort((a, b) -> {
                int ra = a.getRowNo() == null ? Integer.MAX_VALUE : a.getRowNo();
                int rb = b.getRowNo() == null ? Integer.MAX_VALUE : b.getRowNo();
                return Integer.compare(ra, rb);
            });
            return hit;
        });

        // 底册 in 查询：测试里整册返回，服务按 point_no 自己建账
        when(pointMapper.selectList(any(QueryWrapper.class))).thenAnswer(inv -> new ArrayList<TNslpPoint>(points));
    }

    // ------------------------------------------------------------------
    // 动作一：点行数，顶只认抬头那一格
    // ------------------------------------------------------------------

    @Test
    public void firstArrivalCountsRowsAndFreezesDefaultCap() {
        FlowBatchSubmit sub = submit("B2026Q1-01", null,
                row("P0001", STATION, "5", "4", "5.0,4.0"),
                row("P0002", STATION, "8", "7", "8.0,7.0"),
                row("P0001", VEHICLE, "0", "0", "0.0,0.0"));
        FlowReceipt rc = service.receiveBatch(sub);

        assertEquals(1, rc.getArriveSeq());
        assertEquals(300, rc.getCapNum());
        assertEquals(3, rc.getFirstCount());
        assertEquals(3, rc.getTotalCount());
        assertEquals(2, rc.getAcceptedCount());
        assertEquals(1, rc.getRejectedCount());
        assertTrue(rc.isBalanced());
        // 点数与顶都存进了拨抬头，事后翻得出来
        TNslpFlowBatch b = batches.get(0);
        assertEquals(Integer.valueOf(3), b.getFirstCount());
        assertEquals(Integer.valueOf(300), b.getCapNum());
    }

    @Test
    public void negativeOrAbsurdCapTreatedAsBlank() {
        FlowReceipt r1 = service.receiveBatch(submit("B-CAP1", -5,
                row("P0001", STATION, "5", "4", "5.0,4.0")));
        assertEquals(300, r1.getCapNum());
        FlowReceipt r2 = service.receiveBatch(submit("B-CAP2", 999999,
                row("P0001", STATION, "5", "4", "5.0,4.0")));
        assertEquals(300, r2.getCapNum());
    }

    @Test
    public void rowsOverCapWholeBatchRejectedAndNothingLands() {
        FlowBatchSubmit sub = submit("B-OVER", 2,
                row("P0001", STATION, "5", "4", "5.0,4.0"),
                row("P0002", STATION, "5", "4", "5.0,4.0"),
                row("P0001", VEHICLE, "3", "2", "3.0,2.0"));
        FlowReceiveException ex = assertThrows(FlowReceiveException.class, () -> service.receiveBatch(sub));
        assertTrue(ex.getMessage().contains("顶破"));
        assertTrue(batches.isEmpty());
        assertTrue(rows.isEmpty());
    }

    @Test
    public void emptyOrHeadlessBatchDoesNotLand() {
        FlowBatchSubmit noCode = submit(null, null, row("P0001", STATION, "5", "4", "5.0,4.0"));
        assertThrows(FlowReceiveException.class, () -> service.receiveBatch(noCode));
        FlowBatchSubmit empty = new FlowBatchSubmit();
        empty.setBatchNo("B-EMPTY");
        empty.setCutKind(FlowReceiveRules.CUT_DELIMITER);
        assertThrows(FlowReceiveException.class, () -> service.receiveBatch(empty));
        assertTrue(batches.isEmpty() && rows.isEmpty());
    }

    // ------------------------------------------------------------------
    // 动作二+三：看数据、定去处
    // ------------------------------------------------------------------

    @Test
    public void fiveRejectReasonsEachAttachedToItsRow() {
        FlowBatchSubmit sub = submit("B-RULES", 300,
                row("P0001", STATION, "11", "9", "11.0,9.0"),   // 高过核定上限10
                row("P0002", STATION, "0", "0", "0.0,0.0"),     // 读数为0
                row("P0001", VEHICLE, "-1", "2", "-1.0,2.0"),   // 读数为负
                row("", STATION, "3", "2", "3.0,2.0"),          // 点位码没填
                row("P123456789012345678901234567890123",
                        STATION, "3", "2", "3.0,2.0"),           // 码长超出规范
                row("P0002", VEHICLE, "3", "2", "3.0,2.0"));    // 正常收下
        FlowReceipt rc = service.receiveBatch(sub);
        assertEquals(6, rc.getTotalCount());
        assertEquals(1, rc.getAcceptedCount());
        assertEquals(5, rc.getRejectedCount());

        assertEquals(1, service.listRejectedBy("B-RULES", FlowReceiveRules.R_OVER_LIMIT).size());
        assertEquals(1, service.listRejectedBy("B-RULES", FlowReceiveRules.R_ZERO_QTY).size());
        assertEquals(1, service.listRejectedBy("B-RULES", FlowReceiveRules.R_NEGATIVE).size());
        assertEquals(1, service.listRejectedBy("B-RULES", FlowReceiveRules.R_NO_CODE).size());
        assertEquals(1, service.listRejectedBy("B-RULES", FlowReceiveRules.R_CODE_TOO_LONG).size());
        // 超限那行的话写明是哪一条
        TNslpFlowRow over = service.listRejectedBy("B-RULES", FlowReceiveRules.R_OVER_LIMIT).get(0);
        assertTrue(over.getRejectReason().contains("核定上限"));
    }

    @Test
    public void missingOneCountIsNotZeroedAndNotAccepted() {
        FlowBatchSubmit sub = submit("B-MISS", null,
                row("P0001", STATION, "5", null, "5.0"),        // 只到读数次数
                row("P0002", STATION, null, "3", "3.0"),        // 只到达标次数
                row("P0001", VEHICLE, "3", "2", "3.0,2.0"));
        FlowReceipt rc = service.receiveBatch(sub);
        assertEquals(1, rc.getAcceptedCount());
        assertEquals(2, rc.getRejectedCount());
        for (TNslpFlowRow r : service.listByBatch("B-MISS")) {
            if (r.getChkNum() == null || r.getQty() == null) {
                assertEquals(Integer.valueOf(1), r.getMissData());
                assertTrue(r.getRejectCode().contains(FlowReceiveRules.R_MISS_DATA));
            }
        }
        // 缺着的那格没有被代填成 0（0 是一个读数，空是没到）
        TNslpFlowRow onlyQty = null;
        TNslpFlowRow onlyChk = null;
        for (TNslpFlowRow r : service.listByBatch("B-MISS")) {
            if (r.getQty() != null && r.getChkNum() == null) {
                onlyQty = r;
            }
            if (r.getQty() == null && r.getChkNum() != null) {
                onlyChk = r;
            }
        }
        assertNotNull(onlyQty);
        assertNotNull(onlyChk);
        assertNull(onlyQty.getChkNum());
        assertNull(onlyChk.getQty());
    }

    @Test
    public void samePointTwoRowsGetTwoIndependentFates() {
        FlowBatchSubmit sub = submit("B-DUP", null,
                row("P0001", STATION, "5", "4", "5.0,4.0"),     // 直采一趟：收
                row("P0001", VEHICLE, "0", "0", "0.0,0.0"));    // 监测车补报一趟：退
        service.receiveBatch(sub);
        List<TNslpFlowRow> got = service.listByBatch("B-DUP");
        assertEquals(2, got.size());
        int station = got.get(0).getPutKind().equals(STATION) ? got.get(0).getStatus() : got.get(1).getStatus();
        int vehicle = got.get(0).getPutKind().equals(VEHICLE) ? got.get(0).getStatus() : got.get(1).getStatus();
        assertEquals(FlowReceiveRules.STATUS_ACCEPTED, station);
        assertEquals(FlowReceiveRules.STATUS_REJECTED, vehicle);
    }

    @Test
    public void pointMatchedByCodeOnlyFuzzyTitleDoesNotCount() {
        // 码在底册里没有；题名写得再像，也算对不上
        TNslpFlowRow r = row("P9999", STATION, "3", "2", "3.0,2.0");
        r.setRawTitle("临江一号");
        FlowReceipt rc = service.receiveBatch(submit("B-CODE", null, r));
        assertEquals(1, rc.getRejectedCount());
        assertTrue(service.listByBatch("B-CODE").get(0).getRejectCode()
                .contains(FlowReceiveRules.R_CODE_UNMATCHED));
        // 码长不够同样对不上
        TNslpFlowRow short_ = row("P1", STATION, "3", "2", "3.0,2.0");
        FlowReceipt rc2 = service.receiveBatch(submit("B-SHORT", null, short_));
        assertTrue(service.listByBatch("B-SHORT").get(0).getRejectCode()
                .contains(FlowReceiveRules.R_CODE_UNMATCHED));
    }

    @Test
    public void bookMismatchRowStillAcceptedButListedOnReceipt() {
        TNslpFlowRow r = row("P0001", STATION, "5", "4", "5.0,4.0");
        r.setRawTitle("跑题的题名");
        r.setRawZoneNo("QN99");
        FlowReceipt rc = service.receiveBatch(submit("B-BOOK", null, r));
        assertEquals(1, rc.getAcceptedCount());
        assertEquals(1, rc.getBookMismatchRows().size());
        // 以底册为准：行面区册代号仍可从底册带出（book_mismatch 已记账）
        assertEquals(Integer.valueOf(1), rc.getBookMismatchRows().get(0).getBookMismatch());
    }

    @Test
    public void parseFailRowMarkedAloneAndDoesNotHoldWholeBatch() {
        FlowBatchSubmit sub = submit("B-PARSE", null,
                row("P0001", STATION, "5", "4", "切不动的一串"),
                row("P0002", STATION, "5", "4", "5.0,4.0"));
        FlowReceipt rc = service.receiveBatch(sub);
        // 整拨没被按住：好行收下，坏行单独标出退回
        assertEquals(1, rc.getAcceptedCount());
        assertEquals(1, rc.getRejectedCount());
        assertEquals(1, rc.getParseFailRows().size());
        assertTrue(rc.getParseFailRows().get(0).getRejectCode().contains(FlowReceiveRules.R_PARSE_FAIL));
    }

    @Test
    public void unknownOrMixedCutKindRejectsTheWave() {
        FlowBatchSubmit weird = submit("B-CUTX", null, row("P0001", STATION, "5", "4", "5.0,4.0"));
        weird.setCutKind("space");
        assertThrows(FlowReceiveException.class, () -> service.receiveBatch(weird));

        FlowBatchSubmit first = submit("B-CUTMIX", null, row("P0001", STATION, "5", "4", "5.0,4.0"));
        service.receiveBatch(first);
        FlowBatchSubmit second = submit("B-CUTMIX", null, row("P0002", STATION, "5", "4", "5040"));
        second.setCutKind(FlowReceiveRules.CUT_FIXED);
        second.setFixedWidths(java.util.Arrays.asList(2, 2));
        // 第二遍改切法：整遍不落地
        assertThrows(FlowReceiveException.class, () -> service.receiveBatch(second));
        // 首遍那一行没被翻动
        assertEquals(1, service.listByBatch("B-CUTMIX").size());
    }

    @Test
    public void rowNumbersAssignedBySystemInBatchOrder() {
        TNslpFlowRow a = row("P0001", STATION, "5", "4", "5.0,4.0");
        TNslpFlowRow b = row("P0002", STATION, "5", "4", "5.0,4.0");
        a.setRowNo(999); // 手填的格子不作数
        b.setRowNo(888);
        service.receiveBatch(submit("B-ROWNO", null, a, b));
        List<TNslpFlowRow> got = service.listByBatch("B-ROWNO");
        assertEquals(Integer.valueOf(1), got.get(0).getRowNo());
        assertEquals(Integer.valueOf(2), got.get(1).getRowNo());
    }

    // ------------------------------------------------------------------
    // 同一批次码第二遍：只许照面
    // ------------------------------------------------------------------

    @Test
    public void secondArrivalFacesOnlyDoesNotFlipOrRestart() {
        service.receiveBatch(submit("B-TWICE", 10,
                row("P0001", STATION, "5", "4", "5.0,4.0"),    // A 收下 行1
                row("P0001", VEHICLE, "0", "0", "0.0,0.0"),    // B 退回 行2
                row("P0002", STATION, "8", "7", "8.0,7.0")));  // C 收下 行3

        Long aId = idOfRow("P0001", STATION);
        Long bId = idOfRow("P0001", VEHICLE);

        FlowBatchSubmit again = submit("B-TWICE", 10,
                row("P0001", STATION, "0", "0", "0.0,0.0"),    // 与已收下的A同码同来路：只照面
                row("P0001", VEHICLE, "3", "2", "3.0,2.0"),    // B 改好重报
                row("P0002", VEHICLE, "3", "2", "3.0,2.0"));   // 新行 D：行4
        FlowReceipt rc = service.receiveBatch(again);

        assertEquals(2, rc.getArriveSeq());
        assertEquals(3, rc.getFirstCount(), 0, "首遍点行数不重数");
        assertEquals(4, rc.getTotalCount());
        assertEquals(4, rc.getAcceptedCount());
        assertEquals(0, rc.getRejectedCount());
        assertTrue(rc.isBalanced());
        // 抬头还是那一个，没另起一拨
        assertEquals(1, batches.size());
        // A 一行没被翻回收下之外的状态
        TNslpFlowRow a = service.selectTNslpFlowRowById(aId);
        assertEquals(FlowReceiveRules.STATUS_ACCEPTED, a.getStatus());
        assertEquals(new BigDecimal("5"), a.getQty());
        // B 重报：同一主键、原行次、原落地遍数，去处由退回改收下
        TNslpFlowRow b = service.selectTNslpFlowRowById(bId);
        assertEquals(Integer.valueOf(2), b.getRowNo());
        assertEquals(Integer.valueOf(1), b.getArriveSeq());
        assertEquals(FlowReceiveRules.STATUS_ACCEPTED, b.getStatus());
        assertNull(b.getRejectCode());
        // 新行接着排到 4，不重新排队
        TNslpFlowRow d = null;
        for (TNslpFlowRow r : service.listByBatch("B-TWICE")) {
            if ("P0002".equals(r.getItemCode()) && VEHICLE.equals(r.getPutKind())) {
                d = r;
            }
        }
        assertNotNull(d);
        assertEquals(Integer.valueOf(4), d.getRowNo());
    }

    @Test
    public void secondArrivalNewRowsOverCapRejectedButFirstWaveUntouched() {
        service.receiveBatch(submit("B-TWICECAP", 3,
                row("P0001", STATION, "5", "4", "5.0,4.0"),
                row("P0002", STATION, "5", "4", "5.0,4.0"),
                row("P0001", VEHICLE, "5", "4", "5.0,4.0")));
        FlowBatchSubmit again = submit("B-TWICECAP", 3,
                row("P0002", VEHICLE, "5", "4", "5.0,4.0"));
        assertThrows(FlowReceiveException.class, () -> service.receiveBatch(again));
        assertEquals(3, service.listByBatch("B-TWICECAP").size());
    }

    // ------------------------------------------------------------------
    // 动作四：回执与抽行互印
    // ------------------------------------------------------------------

    @Test
    public void receiptCountsFromRowsAndCrossCheckPrints() {
        FlowReceipt rc = service.receiveBatch(submit("B-RC", null,
                row("P0001", STATION, "5", "4", "5.0,4.0"),
                row("P0002", STATION, "0", "0", "0.0,0.0")));
        // 事后重取回执：与进门那趟同一数法
        FlowReceipt again = service.receiptOf("B-RC");
        assertEquals(rc.getTotalCount(), again.getTotalCount());
        assertEquals(rc.getAcceptedCount(), again.getAcceptedCount());
        assertEquals(rc.getRejectedCount(), again.getRejectedCount());
        assertTrue(again.isBalanced());

        Long acceptedId = idOfRow("P0001", STATION);
        FlowRowCrossCheck cc = service.crossCheck(acceptedId);
        assertTrue(cc.isConsistent());
        assertTrue(cc.isBalanced());
        assertTrue(cc.getNote().contains("收下"));
    }

    @Test
    public void receiptOfUnknownBatchRefuses() {
        assertThrows(FlowReceiveException.class, () -> service.receiptOf("B-NONE"));
    }

    // ------------------------------------------------------------------
    // 历史册层：未勾满两年度转入；只此一处挪得动
    // ------------------------------------------------------------------

    @Test
    public void pendingRowsTwoYearsOldMoveToHistoryLayer() {
        // 直接造存量：未勾老行（2024-09 入账）、未勾新行、已收下老行
        seedPending("B-OLD", "P0001", STATION, t("2024-09-01 00:00:00"));
        seedPending("B-OLD", "P0002", STATION, t("2025-06-01 00:00:00"));
        TNslpFlowRow acceptedOld = row("P0001", VEHICLE, "5", "4", null);
        acceptedOld.setStatus(FlowReceiveRules.STATUS_ACCEPTED);
        acceptedOld.setLayer(0);
        acceptedOld.setDelFlag(0);
        acceptedOld.setBatchNo("B-OLD");
        acceptedOld.setCreateTime(t("2023-01-01 00:00:00"));
        flowRowMapper.insert(acceptedOld);
        int before = rows.size();

        int moved = service.archivePendingRows(t("2026-10-04 00:00:00"));
        assertEquals(1, moved);
        assertEquals(before, rows.size(), "只转层不删行");

        // 现行名单不再列它，历史册层单独查得着，照主键仍点得到
        assertEquals(2, service.listByBatch("B-OLD").size());
        List<TNslpFlowRow> history = service.listHistoryLayer("B-OLD");
        assertEquals(1, history.size());
        assertEquals(Integer.valueOf(1), history.get(0).getLayer());
        assertNotNull(history.get(0).getLayerAt());
        assertNotNull(service.selectTNslpFlowRowById(history.get(0).getId()));

        // 取出同样只由这一处作答
        int back = service.restoreFromHistory(java.util.Arrays.asList(history.get(0).getId()), "clerk");
        assertEquals(1, back);
        assertEquals(3, service.listByBatch("B-OLD").size());
        assertTrue(service.listHistoryLayer("B-OLD").isEmpty());
    }

    @Test
    public void newBatchRowsNeverReachHistoryArchive() {
        service.receiveBatch(submit("B-NEW", null,
                row("P0001", STATION, "0", "0", "0.0,0.0"),   // 退回，不是未勾
                row("P0002", STATION, "5", "4", "5.0,4.0"))); // 收下
        int moved = service.archivePendingRows(t("2026-10-04 00:00:00"));
        assertEquals(0, moved);
    }

    // ------------------------------------------------------------------
    // 帮手
    // ------------------------------------------------------------------

    private static TNslpPoint point(String no, String zoneNo, String title, int needNum) {
        TNslpPoint p = new TNslpPoint();
        p.setPointNo(no);
        p.setZoneNo(zoneNo);
        p.setContent(title + "，依附声源交通干线");
        p.setNeedNum(needNum);
        p.setDelFlag(0);
        return p;
    }

    private static TNslpFlowRow row(String code, String putKind, String qty, String chk, String payload) {
        TNslpFlowRow r = new TNslpFlowRow();
        r.setItemCode(code);
        r.setPutKind(putKind);
        r.setQty(qty == null ? null : new BigDecimal(qty));
        r.setChkNum(chk == null ? null : new BigDecimal(chk));
        r.setPayload(payload);
        return r;
    }

    private static FlowBatchSubmit submit(String batchNo, Integer cap, TNslpFlowRow... rs) {
        FlowBatchSubmit s = new FlowBatchSubmit();
        s.setBatchNo(batchNo);
        s.setDeclaredCap(cap);
        s.setCutKind(FlowReceiveRules.CUT_DELIMITER);
        List<TNslpFlowRow> list = new ArrayList<TNslpFlowRow>();
        for (TNslpFlowRow r : rs) {
            list.add(r);
        }
        s.setRows(list);
        return s;
    }

    private void seedPending(String batchNo, String code, String putKind, Date createTime) {
        TNslpFlowRow r = row(code, putKind, null, null, null);
        r.setBatchNo(batchNo);
        r.setStatus(FlowReceiveRules.STATUS_PENDING);
        r.setLayer(0);
        r.setDelFlag(0);
        r.setMissData(1);
        r.setCreateTime(createTime);
        flowRowMapper.insert(r);
    }

    private Long idOfRow(String code, String putKind) {
        for (TNslpFlowRow r : rows) {
            if (code.equals(r.getItemCode()) && putKind.equals(r.getPutKind())) {
                return r.getId();
            }
        }
        return null;
    }

    /** 从 Wrapper 片段里把 列=#{参数} / 列<=#{参数} 解成 列→值（Query/Update 通用） */
    private static Map<String, Object> eqValues(
            com.baomidou.mybatisplus.core.conditions.AbstractWrapper<?, ?, ?> w) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        if (w == null) {
            return out;
        }
        String seg = w.getSqlSegment();
        Map<String, Object> params = w.getParamNameValuePairs();
        Matcher m = Pattern.compile("([a-z_]+)\\s*(=|<=|>=)\\s*#\\{([^}]+)\\}").matcher(seg);
        while (m.find()) {
            out.put(m.group(1), paramValue(params, m.group(3)));
        }
        return out;
    }

    /** 从 UpdateWrapper 的 SET 片段解 列→值 */
    private static Map<String, Object> setValues(
            com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<?> w) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        String setSeg = w.getSqlSet();
        Map<String, Object> params = w.getParamNameValuePairs();
        Matcher m = Pattern.compile("([a-z_]+)=#\\{([^}]+)\\}").matcher(setSeg);
        while (m.find()) {
            out.put(m.group(1), paramValue(params, m.group(2)));
        }
        return out;
    }

    private static Object paramValue(Map<String, Object> params, String ref) {
        String tail = ref.substring(ref.lastIndexOf('.') + 1);
        Object val = params.get(tail);
        return val == null ? params.get(ref) : val;
    }

    private static void applySets(TNslpFlowRow r, Map<String, Object> sets) {
        for (Map.Entry<String, Object> e : sets.entrySet()) {
            Object v = e.getValue();
            switch (e.getKey()) {
                case "batch_no": r.setBatchNo((String) v); break;
                case "row_no": r.setRowNo((Integer) v); break;
                case "arrive_seq": r.setArriveSeq((Integer) v); break;
                case "item_code": r.setItemCode((String) v); break;
                case "qty": r.setQty((BigDecimal) v); break;
                case "chk_num": r.setChkNum((BigDecimal) v); break;
                case "miss_data": r.setMissData((Integer) v); break;
                case "payload": r.setPayload((String) v); break;
                case "parse_fail": r.setParseFail((Integer) v); break;
                case "put_kind": r.setPutKind((String) v); break;
                case "raw_title": r.setRawTitle((String) v); break;
                case "raw_zone_no": r.setRawZoneNo((String) v); break;
                case "book_mismatch": r.setBookMismatch((Integer) v); break;
                case "status": r.setStatus((Integer) v); break;
                case "reject_code": r.setRejectCode((String) v); break;
                case "reject_reason": r.setRejectReason((String) v); break;
                case "layer": r.setLayer((Integer) v); break;
                case "layer_at": r.setLayerAt((Date) v); break;
                case "update_time": r.setUpdateTime((Date) v); break;
                case "update_by": r.setUpdateBy((String) v); break;
                default: break;
            }
        }
    }
}
