package com.fc.v2.service.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fc.v2.model.auto.TNslpFlowRow;
import com.fc.v2.model.auto.TNslpPoint;

/**
 * 核收纯口径测试：顶额认定、两数齐到、码规范、报文切分（一拨一种）、退回条款、底册比对。
 */
public class FlowReceiveRulesTest {

    @Test
    public void capOnlyComesFromHeaderCell() {
        assertEquals(300, FlowReceiveRules.resolveCap(null));
        assertEquals(300, FlowReceiveRules.resolveCap(-1));
        assertEquals(300, FlowReceiveRules.resolveCap(0));
        assertEquals(300, FlowReceiveRules.resolveCap(999999));
        assertEquals(300, FlowReceiveRules.resolveCap(1000000));
        assertEquals(120, FlowReceiveRules.resolveCap(120));
    }

    @Test
    public void twoCountsMustArriveTogether() {
        assertTrue(FlowReceiveRules.missData(null, null));
        assertTrue(FlowReceiveRules.missData(new BigDecimal("3"), null));
        assertTrue(FlowReceiveRules.missData(null, new BigDecimal("3")));
        assertFalse(FlowReceiveRules.missData(new BigDecimal("3"), new BigDecimal("2")));
    }

    @Test
    public void pointCodeShape() {
        assertEquals(-1, FlowReceiveRules.codeShape(null));
        assertEquals(-1, FlowReceiveRules.codeShape("  "));
        assertEquals(1, FlowReceiveRules.codeShape("P123456789012345678901234567890123"));
        assertEquals(-2, FlowReceiveRules.codeShape("P1"));
        assertEquals(0, FlowReceiveRules.codeShape("P001"));
        // 恰在上沿 32 位（P + 31 位数字）
        assertEquals(0, FlowReceiveRules.codeShape("P1234567890123456789012345678901"));
    }

    @Test
    public void cutByDelimiterOneWayOnly() {
        List<String> parts = FlowReceiveRules.cutPayload(
                FlowReceiveRules.CUT_DELIMITER, "52.1,53.0,51.4", null, null);
        assertEquals(Arrays.asList("52.1", "53.0", "51.4"), parts);
        // 空段切不动
        assertNull(FlowReceiveRules.cutPayload(
                FlowReceiveRules.CUT_DELIMITER, "52.1,,51.4", null, null));
        // 找不到分隔符切不动
        assertNull(FlowReceiveRules.cutPayload(
                FlowReceiveRules.CUT_DELIMITER, "52.1", null, null));
    }

    @Test
    public void cutByFixedWidthMustLineUpExactly() {
        List<String> parts = FlowReceiveRules.cutPayload(
                FlowReceiveRules.CUT_FIXED, "521530514", null, Arrays.asList(3, 3, 3));
        assertEquals(Arrays.asList("521", "530", "514"), parts);
        // 总宽对不上：切不动，不许硬切
        assertNull(FlowReceiveRules.cutPayload(
                FlowReceiveRules.CUT_FIXED, "52153051", null, Arrays.asList(3, 3, 3)));
        assertNull(FlowReceiveRules.cutPayload(
                FlowReceiveRules.CUT_FIXED, "5215305140", null, Arrays.asList(3, 3, 3)));
        // 切法不明切不动
        assertNull(FlowReceiveRules.cutPayload("weird", "52.1,53.0", null, null));
    }

    @Test
    public void rejectReasonsHitEveryApplicableRuleInOrder() {
        TNslpFlowRow r = new TNslpFlowRow();
        r.setParseFail(1);
        r.setItemCode("");
        r.setQty(new BigDecimal("1"));
        r.setChkNum(null);
        List<String> codes = FlowReceiveRules.rejectReasons(r, null);
        assertEquals(Arrays.asList(
                FlowReceiveRules.R_PARSE_FAIL,
                FlowReceiveRules.R_NO_CODE,
                FlowReceiveRules.R_MISS_DATA), codes);
        // 缺数时不拿空值去套 0/负数条款
        assertFalse(codes.contains(FlowReceiveRules.R_ZERO_QTY));
        assertFalse(codes.contains(FlowReceiveRules.R_NEGATIVE));
    }

    @Test
    public void rejectZeroAndNegativeAndOverLimit() {
        TNslpPoint p = new TNslpPoint();
        p.setPointNo("P001");
        p.setNeedNum(10);
        TNslpFlowRow over = row("P001", "11", "0");
        assertEquals(Arrays.asList(FlowReceiveRules.R_OVER_LIMIT),
                FlowReceiveRules.rejectReasons(over, p));
        assertEquals(Arrays.asList(FlowReceiveRules.R_ZERO_QTY),
                FlowReceiveRules.rejectReasons(row("P001", "0", "0"), p));
        // 负数达标次数也算读数为负
        assertEquals(Arrays.asList(FlowReceiveRules.R_NEGATIVE),
                FlowReceiveRules.rejectReasons(row("P001", "3", "-1"), p));
        // 贴着上限（含等号）收下
        assertTrue(FlowReceiveRules.rejectReasons(row("P001", "10", "9"), p).isEmpty());
    }

    @Test
    public void codeTooShortOrNotInBookIsUnmatched() {
        TNslpPoint p = new TNslpPoint();
        p.setPointNo("P001");
        p.setNeedNum(10);
        // 码长不够：题名再像也对不上
        assertEquals(Arrays.asList(FlowReceiveRules.R_CODE_UNMATCHED),
                FlowReceiveRules.rejectReasons(row("P1", "3", "2"), null));
        // 形状齐整但底册查无此码
        assertEquals(Arrays.asList(FlowReceiveRules.R_CODE_UNMATCHED),
                FlowReceiveRules.rejectReasons(row("P999", "3", "2"), null));
        // 码超长是另一条
        TNslpFlowRow longCode = row("P123456789012345678901234567890123", "3", "2");
        assertEquals(Arrays.asList(FlowReceiveRules.R_CODE_TOO_LONG),
                FlowReceiveRules.rejectReasons(longCode, null));
    }

    @Test
    public void bookMismatchDefersToBook() {
        TNslpPoint p = new TNslpPoint();
        p.setPointNo("P001");
        p.setZoneNo("QN00");
        p.setContent("临江一号片区点位");
        assertFalse(FlowReceiveRules.bookMismatch("临江一号", "QN00", p));
        assertTrue(FlowReceiveRules.bookMismatch("跑题题名", "QN00", p));
        assertTrue(FlowReceiveRules.bookMismatch("临江一号", "QN99", p));
        // 报文没带的不挑错
        assertFalse(FlowReceiveRules.bookMismatch(null, null, p));
    }

    @Test
    public void onlyThreeStatusesAllowed() {
        assertTrue(FlowReceiveRules.validStatus(0));
        assertTrue(FlowReceiveRules.validStatus(1));
        assertTrue(FlowReceiveRules.validStatus(2));
        assertFalse(FlowReceiveRules.validStatus(3));
        assertFalse(FlowReceiveRules.validStatus(null));
    }

    @Test
    public void reasonsJoinSentenceBySentence() {
        String joined = FlowReceiveRules.joinReasons(Arrays.asList(
                FlowReceiveRules.R_ZERO_QTY, FlowReceiveRules.R_NEGATIVE));
        assertEquals("读数次数为0；读数为负", joined);
        assertEquals("R_ZERO_QTY,R_NEGATIVE",
                FlowReceiveRules.joinCodes(Arrays.asList("R_ZERO_QTY", "R_NEGATIVE")));
    }

    private static TNslpFlowRow row(String code, String qty, String chk) {
        TNslpFlowRow r = new TNslpFlowRow();
        r.setItemCode(code);
        r.setQty(qty == null ? null : new BigDecimal(qty));
        r.setChkNum(chk == null ? null : new BigDecimal(chk));
        return r;
    }
}
