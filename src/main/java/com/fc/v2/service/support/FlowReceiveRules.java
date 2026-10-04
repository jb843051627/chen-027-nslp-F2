package com.fc.v2.service.support;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fc.v2.model.auto.TNslpFlowRow;
import com.fc.v2.model.auto.TNslpPoint;

/**
 * 声管科自动监测流水核收纯口径（不碰库、不沾 Spring）。
 *
 * 四个动作——点行数、看数据、定去处、出回执——里凡属「怎么算」的部分都收在这一处，
 * Service 只负责落库与编排，不许另备第二份算法。去处只有 0未勾/1收下/2退回三种，
 * 第四种写法不许落库；新拨一口气走完四动作，行只落 1/2（0 只接存量）。
 *
 * @author fuce
 * @date 2026-10-04
 */
public final class FlowReceiveRules {

    // ---- 行去处：只此三种，第四种不许落库 ----
    /** 未勾：只接存量；新拨四动作一口气走完，没有行停在这一地 */
    public static final int STATUS_PENDING = 0;
    /** 收下 */
    public static final int STATUS_ACCEPTED = 1;
    /** 退回 */
    public static final int STATUS_REJECTED = 2;

    // ---- 拨顶：顶只写在拨抬头那一格，别处不设第二个来源 ----
    /** 抬头空着不填，按 300 认 */
    public static final int DEFAULT_CAP = 300;
    /** 填成负数或离谱大数（≥此值）都当没填，仍按 300 认 */
    public static final int CAP_ABSURD = 999999;

    // ---- 点位码规范：点位只认码，题名近似凑数不认 ----
    /** 码长下沿（含）：比这短算对不上 */
    public static final int POINT_CODE_MIN_LEN = 4;
    /** 码长上沿（含）：比这长即「码长超出规范」 */
    public static final int POINT_CODE_MAX_LEN = 32;

    // ---- 报文切法：一拨之内只许一种 ----
    public static final String CUT_DELIMITER = "delimiter";
    public static final String CUT_FIXED = "fixed";
    /** 分隔符切法未指明分隔符时认逗号 */
    public static final String DEFAULT_DELIMITER = ",";

    // ---- 退回条款码：哪一行吃了哪一条，流水里一句句问得出 ----
    /** 本行读数次数高过当地那条核定上限（底册 need_num） */
    public static final String R_OVER_LIMIT = "R_OVER_LIMIT";
    /** 读数次数为 0 */
    public static final String R_ZERO_QTY = "R_ZERO_QTY";
    /** 读数为负（读数次数/达标次数任一个为负） */
    public static final String R_NEGATIVE = "R_NEGATIVE";
    /** 点位码没填 */
    public static final String R_NO_CODE = "R_NO_CODE";
    /** 码长超出规范 */
    public static final String R_CODE_TOO_LONG = "R_CODE_TOO_LONG";
    /** 点位码与底册对不上（码长不够、底册里查无此码；题名近似不认） */
    public static final String R_CODE_UNMATCHED = "R_CODE_UNMATCHED";
    /** 读数次数与达标次数没一起到：不代填、不当 0 收下 */
    public static final String R_MISS_DATA = "R_MISS_DATA";
    /** 报文读数串切不动：单行标出递给回核，不因此按住整拨 */
    public static final String R_PARSE_FAIL = "R_PARSE_FAIL";

    private FlowReceiveRules() {
    }

    /**
     * 认定拨抬头的顶。空着（null）按 300；负数按没填；离谱大数（≥{@link #CAP_ABSURD}）
     * 也按没填。正数小过离谱线的，照填的认。
     */
    public static int resolveCap(Integer declaredCap) {
        if (declaredCap == null || declaredCap.intValue() <= 0 || declaredCap.intValue() >= CAP_ABSURD) {
            return DEFAULT_CAP;
        }
        return declaredCap.intValue();
    }

    /** 点行数：进门数出来的就是交来行的条数，空拨为 0 */
    public static int countRows(List<?> rows) {
        return rows == null ? 0 : rows.size();
    }

    /** 两数必须一起到：两个都缺或缺一个，都算没一起到（系统不代填、不当 0） */
    public static boolean missData(BigDecimal qty, BigDecimal chkNum) {
        return qty == null || chkNum == null;
    }

    /**
     * 码的形状。0 形状齐整可查底册；-1 空着没填（R_NO_CODE）；
     * 1 长过上沿（R_CODE_TOO_LONG）；-2 短于下沿（对不上，R_CODE_UNMATCHED）。
     */
    public static int codeShape(String code) {
        if (code == null || code.trim().isEmpty()) {
            return -1;
        }
        int len = code.trim().length();
        if (len > POINT_CODE_MAX_LEN) {
            return 1;
        }
        if (len < POINT_CODE_MIN_LEN) {
            return -2;
        }
        return 0;
    }

    /**
     * 切报文读数串。一拨之内切法只许一种：{@link #CUT_DELIMITER} 按分隔符切，
     * {@link #CUT_FIXED} 按定宽切。切得动返回切出的各段；切不动返回 null
     * （调用方单行标出，递给回核，不许按住整拨，也不许抛出去）。
     */
    public static List<String> cutPayload(String cutKind, String payload, String delimiter,
                                          List<Integer> fixedWidths) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        if (CUT_DELIMITER.equals(cutKind)) {
            String sep = (delimiter == null || delimiter.isEmpty()) ? DEFAULT_DELIMITER : delimiter;
            if (!payload.contains(sep)) {
                return null;
            }
            String[] parts = payload.split(java.util.regex.Pattern.quote(sep), -1);
            if (parts.length < 2) {
                return null;
            }
            List<String> out = new ArrayList<String>();
            for (String p : parts) {
                if (p == null || p.trim().isEmpty()) {
                    // 切出空格子等于没切出该段：切不动
                    return null;
                }
                out.add(p.trim());
            }
            return out;
        }
        if (CUT_FIXED.equals(cutKind)) {
            if (fixedWidths == null || fixedWidths.isEmpty()) {
                return null;
            }
            int sum = 0;
            for (Integer w : fixedWidths) {
                if (w == null || w.intValue() <= 0) {
                    return null;
                }
                sum += w.intValue();
            }
            if (payload.length() != sum) {
                // 总宽对不上，整串没法定格：切不动
                return null;
            }
            List<String> out = new ArrayList<String>();
            int cursor = 0;
            for (Integer w : fixedWidths) {
                String piece = payload.substring(cursor, cursor + w.intValue());
                if (piece.trim().isEmpty()) {
                    return null;
                }
                out.add(piece.trim());
                cursor += w.intValue();
            }
            return out;
        }
        // 切法不明：切不动
        return null;
    }

    /**
     * 定去处的唯一口径：把本行吃着的条款一条条挑出来。返回空 = 收下；非空 = 退回，
     * 多条照固定次序都给上（一条行同时吃几条就写几条，谁也不抹谁）。
     *
     * @param row   行（parseFail/missData 等标记由看数据那一步先落好）
     * @param point 按点位码从底册请出的那一行；码空/超长/对不上时为 null
     */
    public static List<String> rejectReasons(TNslpFlowRow row, TNslpPoint point) {
        List<String> codes = new ArrayList<String>();
        if (row == null) {
            return codes;
        }
        if (row.getParseFail() != null && row.getParseFail().intValue() == 1) {
            codes.add(R_PARSE_FAIL);
        }
        int shape = codeShape(row.getItemCode());
        if (shape == -1) {
            codes.add(R_NO_CODE);
        } else if (shape == 1) {
            codes.add(R_CODE_TOO_LONG);
        } else if (shape == -2 || point == null) {
            // 只认点位码：码长不够、底册查无此码，题名再近似也算对不上
            codes.add(R_CODE_UNMATCHED);
        }

        BigDecimal qty = row.getQty();
        BigDecimal chk = row.getChkNum();
        if (missData(qty, chk)) {
            codes.add(R_MISS_DATA);
        } else {
            // 两数齐了才谈大小；缺数时不拿空值去套 0 或负数的条款
            if (qty.signum() == 0) {
                codes.add(R_ZERO_QTY);
            }
            if (qty.signum() < 0 || chk.signum() < 0) {
                codes.add(R_NEGATIVE);
            }
            // 当地核定上限只从底册 need_num 请；没挂上底册（对不上）无从谈超限
            if (point != null && point.getNeedNum() != null
                    && qty.compareTo(new BigDecimal(point.getNeedNum().intValue())) > 0) {
                codes.add(R_OVER_LIMIT);
            }
        }
        return codes;
    }

    /** 条款码翻译成回执/流水上写明的那句话；未知码原样给回 */
    public static String reasonText(String code) {
        if (code == null) {
            return "";
        }
        switch (code) {
            case R_OVER_LIMIT:
                return "本行读数次数高过当地那条核定上限";
            case R_ZERO_QTY:
                return "读数次数为0";
            case R_NEGATIVE:
                return "读数为负";
            case R_NO_CODE:
                return "点位码没填";
            case R_CODE_TOO_LONG:
                return "码长超出规范";
            case R_CODE_UNMATCHED:
                return "点位码与底册对不上（只认点位码，题名近似不认）";
            case R_MISS_DATA:
                return "读数次数与达标次数没一起到，不代填、不当0收下";
            case R_PARSE_FAIL:
                return "报文读数串切不动，单行标出递给回核";
            default:
                return code;
        }
    }

    /** 多条码值连成存库的 reject_code（逗号相连） */
    public static String joinCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String c : codes) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /** 多条理由连成一句句说得清的话（「；」相连，次序同条款码） */
    public static String joinReasons(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String c : codes) {
            if (sb.length() > 0) {
                sb.append("；");
            }
            sb.append(reasonText(c));
        }
        return sb.toString();
    }

    /**
     * 报文带出的题名/区册代号与底册比对：两处不合以底册为准，回执单列。
     * 区册代号须字字相同；题名只要报文那串落在底册记要里即认合（底册记要含依附声源）。
     * 报文没带的不挑错；底册行不在（码对不上）不在此处判，归 R_CODE_UNMATCHED。
     */
    public static boolean bookMismatch(String rawTitle, String rawZoneNo, TNslpPoint point) {
        if (point == null) {
            return false;
        }
        if (rawZoneNo != null && !rawZoneNo.trim().isEmpty()
                && !rawZoneNo.trim().equals(point.getZoneNo())) {
            return true;
        }
        if (rawTitle != null && !rawTitle.trim().isEmpty()) {
            String book = point.getContent() == null ? "" : point.getContent();
            if (!book.contains(rawTitle.trim())) {
                return true;
            }
        }
        return false;
    }

    /** 去处合法性：只许 0/1/2，第四种落不进来 */
    public static boolean validStatus(Integer status) {
        return status != null && status >= 0 && status <= 2;
    }
}
