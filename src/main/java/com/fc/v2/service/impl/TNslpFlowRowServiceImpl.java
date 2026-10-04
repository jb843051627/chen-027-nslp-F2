package com.fc.v2.service.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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
import com.fc.v2.service.ITNslpFlowRowService;
import com.fc.v2.service.support.FlowReceiveRules;

/**
 * 监测流水核收 Service 实现。
 *
 * 核收只做四个动作，一口气走完，中间不插人手：
 * 点行数（点数与写定的顶存进拨抬头）→ 看数据（两数齐到、只认点位码、一拨一种切法）
 * → 定去处（只落收下/退回，退回逐条写明）→ 出回执（回行里一行一行重数）。
 *
 * 去向、同批次码再到的处置、拨首三数的出处、历史册层的转入取出，一概只认本类这一处：
 * 不设第二条把手，不提供自行落库/自行改写去向的口子，不许先落流水再回头补说法。
 *
 * @author fuce
 * @date 2026-10-04
 */
@Service
public class TNslpFlowRowServiceImpl implements ITNslpFlowRowService {

    @javax.annotation.Resource
    private TNslpFlowRowMapper flowRowMapper;

    @javax.annotation.Resource
    private TNslpFlowBatchMapper flowBatchMapper;

    @javax.annotation.Resource
    private TNslpPointMapper pointMapper;

    @Override
    public TNslpFlowRow selectTNslpFlowRowById(Long id) {
        // 现行、历史册层一体照主键点得到，先前入过账的一条不减
        return this.flowRowMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowReceipt receiveBatch(FlowBatchSubmit submit) {
        // ========== 进门先把拨形校死，形不成整拨不落地（与"单行毛病变通"是两回事） ==========
        if (submit == null || isBlank(submit.getBatchNo())) {
            throw new FlowReceiveException("批次码没填，一拨无抬头，整拨不落地");
        }
        String batchNo = submit.getBatchNo().trim();
        List<TNslpFlowRow> incoming = submit.getRows();
        if (incoming == null || incoming.isEmpty()) {
            throw new FlowReceiveException("空拨不收：一拨一次交齐，不收零散逐行");
        }
        String cutKind = normalizeCutKind(submit.getCutKind());
        if (FlowReceiveRules.CUT_FIXED.equals(cutKind)
                && (submit.getFixedWidths() == null || submit.getFixedWidths().isEmpty())) {
            throw new FlowReceiveException("定宽切法须随拨带各段宽度，拨形不成，整拨不落地");
        }

        TNslpFlowBatch batch = this.flowBatchMapper.selectOne(new QueryWrapper<TNslpFlowBatch>()
                .eq("batch_no", batchNo).eq("del_flag", 0));
        boolean firstArrive = batch == null;
        int arriveSeq;
        int cap;
        List<TNslpFlowRow> existing;
        if (firstArrive) {
            // ========== 动作一（首遍）：点行数；顶只认抬头那一格，别处没有第二来源 ==========
            cap = FlowReceiveRules.resolveCap(submit.getDeclaredCap());
            int firstCount = FlowReceiveRules.countRows(incoming);
            if (firstCount > cap) {
                throw new FlowReceiveException("本拨点出 " + firstCount + " 行，顶破抬头写定的顶 "
                        + cap + " 行，整拨不落地");
            }
            batch = new TNslpFlowBatch();
            batch.setBatchNo(batchNo);
            batch.setCapNum(Integer.valueOf(cap));
            batch.setFirstCount(Integer.valueOf(firstCount));
            batch.setArriveSeq(Integer.valueOf(1));
            batch.setCutKind(cutKind);
            batch.setDelFlag(Integer.valueOf(0));
            this.flowBatchMapper.insert(batch);
            arriveSeq = 1;
            existing = new ArrayList<TNslpFlowRow>();
        } else {
            // ========== 同一批次码再到：只许照面，不另起一拨、不翻首遍的行 ==========
            cap = batch.getCapNum() == null ? FlowReceiveRules.DEFAULT_CAP : batch.getCapNum().intValue();
            if (batch.getCutKind() != null && !batch.getCutKind().equals(cutKind)) {
                throw new FlowReceiveException("切法一拨之内只许一种：本拨首遍按「"
                        + batch.getCutKind() + "」切，这趟改「" + cutKind + "」，整遍不落地");
            }
            arriveSeq = 2;
            batch.setArriveSeq(Integer.valueOf(arriveSeq));
            batch.setUpdateTime(new Date());
            this.flowBatchMapper.updateById(batch);
            existing = listRowsAllLayers(batchNo);
        }

        // 首遍落定行按 (点位码+来路) 建账，第二遍照面时认得出谁是重报、谁已收下
        Map<String, TNslpFlowRow> rejectedIndex = new HashMap<String, TNslpFlowRow>();
        int maxRowNo = 0;
        for (TNslpFlowRow r : existing) {
            if (r.getRowNo() != null && r.getRowNo().intValue() > maxRowNo) {
                maxRowNo = r.getRowNo().intValue();
            }
            if (r.getStatus() != null && r.getStatus().intValue() == FlowReceiveRules.STATUS_REJECTED) {
                String key = reissueKey(r);
                if (key != null && !rejectedIndex.containsKey(key)) {
                    rejectedIndex.put(key, r);
                }
            }
        }

        // 先把这趟分成：重报 / 新行 / 照面（已收下或未勾存量的同 key 行只照面，不翻不插）
        List<TNslpFlowRow> freshRows = new ArrayList<TNslpFlowRow>();
        List<ReissuePair> reissues = new ArrayList<ReissuePair>();
        int nextRowNo = maxRowNo;
        for (TNslpFlowRow in : incoming) {
            if (in == null) {
                continue;
            }
            in.setBatchNo(batchNo);
            String key = reissueKey(in);
            TNslpFlowRow priorRejected = key == null ? null : rejectedIndex.get(key);
            if (priorRejected != null) {
                reissues.add(new ReissuePair(in, priorRejected));
                // 重报认领过就出账：同一趟里同码同来路再来一条，不再当成同一行重报
                rejectedIndex.remove(key);
            } else if (firstArrive || !alreadyOnBooks(key, existing)) {
                // 新行：系统在本拨内续排行次，不留手填格
                nextRowNo++;
                in.setRowNo(Integer.valueOf(nextRowNo));
                freshRows.add(in);
            }
            // else：首遍已收下/未勾存量的同 key 行——只照面，不动它，也不落第二条
        }

        if (!firstArrive && existing.size() + freshRows.size() > cap) {
            throw new FlowReceiveException("照面这趟带的新行会使本拨达 "
                    + (existing.size() + freshRows.size()) + " 行，顶破抬头的顶 " + cap
                    + " 行，整遍不落地（首遍已落的行一行不动）");
        }

        // 底册只请一面：本趟涉及的点位码一次取齐，判定不各查一遍
        Map<String, TNslpPoint> book = loadBook(incoming);

        // ========== 动作二+三：看数据 → 定去处（重报行与新行走同一份口径） ==========
        for (TNslpFlowRow r : freshRows) {
            r.setArriveSeq(Integer.valueOf(arriveSeq));
            settleRow(r, cutKind, submit.getDelimiter(), submit.getFixedWidths(), book);
            r.setDelFlag(Integer.valueOf(0));
            r.setLayer(Integer.valueOf(0));
            this.flowRowMapper.insert(r);
        }
        for (ReissuePair pair : reissues) {
            TNslpFlowRow r = pair.incoming;
            TNslpFlowRow landed = pair.landed;
            // 行面仍是当初那一个行次，不重新排队；批次码、来路归属也不挪
            r.setId(landed.getId());
            r.setRowNo(landed.getRowNo());
            r.setArriveSeq(landed.getArriveSeq());
            r.setLayer(landed.getLayer() == null ? Integer.valueOf(0) : landed.getLayer());
            settleRow(r, cutKind, submit.getDelimiter(), submit.getFixedWidths(), book);
            r.setDelFlag(landed.getDelFlag() == null ? Integer.valueOf(0) : landed.getDelFlag());
            r.setUpdateTime(new Date());
            // 整行显式落一遍：改收下时 reject_code/reject_reason 要清成空白，
            // updateById 默认跳过 null 字段，清不干净，故走显式 set 的 UpdateWrapper
            overwriteRow(r);
        }

        // ========== 动作四：出回执——三数回流水里一行一行数出来 ==========
        return buildReceipt(batchNo);
    }

    @Override
    public FlowReceipt receiptOf(String batchNo) {
        if (isBlank(batchNo)) {
            throw new FlowReceiveException("批次码没填，取不到回执");
        }
        return buildReceipt(batchNo.trim());
    }

    @Override
    public FlowRowCrossCheck crossCheck(Long rowId) {
        TNslpFlowRow row = this.flowRowMapper.selectById(rowId);
        if (row == null) {
            throw new FlowReceiveException("照主键点不到这一行，无从复核");
        }
        List<TNslpFlowRow> all = listRowsAllLayers(row.getBatchNo());
        int total = 0;
        int accepted = 0;
        int rejected = 0;
        int pending = 0;
        for (TNslpFlowRow r : all) {
            total++;
            int st = r.getStatus() == null ? -1 : r.getStatus().intValue();
            if (st == FlowReceiveRules.STATUS_ACCEPTED) {
                accepted++;
            } else if (st == FlowReceiveRules.STATUS_REJECTED) {
                rejected++;
            } else if (st == FlowReceiveRules.STATUS_PENDING) {
                pending++;
            }
        }
        boolean mathBalanced = accepted + rejected + pending == total;
        // 回执口径：新拨没有空悬未勾，收+退必须正好等于总数
        boolean receiptBalanced = pending == 0 && accepted + rejected == total;
        int rowStatus = row.getStatus() == null ? -1 : row.getStatus().intValue();
        boolean inRightBucket = rowStatus == FlowReceiveRules.STATUS_ACCEPTED
                || rowStatus == FlowReceiveRules.STATUS_REJECTED
                || rowStatus == FlowReceiveRules.STATUS_PENDING;
        boolean consistent = mathBalanced && receiptBalanced && inRightBucket
                && (row.getLayer() == null || row.getLayer().intValue() == 0 || row.getLayer().intValue() == 1);

        FlowRowCrossCheck cc = new FlowRowCrossCheck();
        cc.setRow(row);
        cc.setTotalCount(total);
        cc.setAcceptedCount(accepted);
        cc.setRejectedCount(rejected);
        cc.setPendingCount(pending);
        cc.setBalanced(receiptBalanced);
        cc.setConsistent(consistent);
        if (consistent) {
            cc.setNote("批次" + row.getBatchNo() + "第" + row.getRowNo() + "行去处为「"
                    + statusText(rowStatus) + "」，与重数三数互相印证");
        } else {
            // 印不上的一行行点名，不认「大致相符」
            cc.setNote("批次" + row.getBatchNo() + "第" + row.getRowNo() + "行印不上：去处「"
                    + statusText(rowStatus) + "」，重数为总" + total + "/收" + accepted + "/退"
                    + rejected + "/未勾" + pending + (mathBalanced ? "" : "，三数不轧平")
                    + (receiptBalanced ? "" : "，收+退对不住总数"));
        }
        return cc;
    }

    @Override
    public List<TNslpFlowRow> listByBatch(String batchNo) {
        // 现行名单：已转入历史册层的不列在这里
        return this.flowRowMapper.selectList(new QueryWrapper<TNslpFlowRow>()
                .eq("batch_no", batchNo).eq("del_flag", 0).eq("layer", 0)
                .orderByAsc("row_no"));
    }

    @Override
    public List<TNslpFlowRow> listRejectedBy(String batchNo, String rejectCode) {
        if (isBlank(rejectCode)) {
            return new ArrayList<TNslpFlowRow>();
        }
        // reject_code 逗号相连，按整码片段像，避免 R_X 撞上 R_XY
        List<TNslpFlowRow> hit = new ArrayList<TNslpFlowRow>();
        for (TNslpFlowRow r : listRowsAllLayers(batchNo)) {
            if (r.getStatus() != null && r.getStatus().intValue() == FlowReceiveRules.STATUS_REJECTED
                    && r.getRejectCode() != null && containsCode(r.getRejectCode(), rejectCode.trim())) {
                hit.add(r);
            }
        }
        return hit;
    }

    @Override
    public List<TNslpFlowRow> listHistoryLayer(String batchNo) {
        return this.flowRowMapper.selectList(new QueryWrapper<TNslpFlowRow>()
                .eq("batch_no", batchNo).eq("del_flag", 0).eq("layer", 1)
                .orderByAsc("row_no"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int archivePendingRows(Date at) {
        Date now = at == null ? new Date() : at;
        // 连着满两个年度：以入账日算，到 at 已满两周年
        Calendar c = Calendar.getInstance();
        c.setTime(now);
        c.add(Calendar.YEAR, -2);
        Date deadline = c.getTime();

        // 只动空悬未勾、仍在现行名单、入过账的存量；新拨行只落收/退，走不到它跟前
        List<TNslpFlowRow> pending = this.flowRowMapper.selectList(new QueryWrapper<TNslpFlowRow>()
                .eq("status", FlowReceiveRules.STATUS_PENDING)
                .eq("layer", 0).eq("del_flag", 0)
                .isNotNull("create_time").le("create_time", deadline));
        int moved = 0;
        for (TNslpFlowRow r : pending) {
            r.setLayer(Integer.valueOf(1));
            r.setLayerAt(now);
            r.setUpdateTime(now);
            this.flowRowMapper.updateById(r);
            moved++;
        }
        return moved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int restoreFromHistory(List<Long> rowIds, String operator) {
        if (rowIds == null || rowIds.isEmpty()) {
            return 0;
        }
        int moved = 0;
        for (Long id : rowIds) {
            TNslpFlowRow r = this.flowRowMapper.selectById(id);
            // 取出同样只由这一处作答：只接历史册层里的行，旁处的行一概不挪
            if (r == null || r.getLayer() == null || r.getLayer().intValue() != 1) {
                continue;
            }
            r.setLayer(Integer.valueOf(0));
            r.setLayerAt(null);
            r.setUpdateBy(operator);
            r.setUpdateTime(new Date());
            overwriteRow(r);
            moved++;
        }
        return moved;
    }

    // ------------------------------------------------------------------
    // 内部：唯一的看数据→定去处口径，重报与新行同走这一遍，无第二份
    // ------------------------------------------------------------------

    /**
     * 看数据（缺数/切分/底册比对）紧接着定去处（收下/退回，逐条写明）。
     * 两个动作一口气落进行对象，status 只会被写成 1 或 2，第四种写法进不来。
     */
    private void settleRow(TNslpFlowRow r, String cutKind, String delimiter, List<Integer> fixedWidths,
                           Map<String, TNslpPoint> book) {
        // —— 看数据其一：两数一起到，缺一个不代填、不当 0（缺格就以 null 落库，不写 0） ——
        boolean miss = FlowReceiveRules.missData(r.getQty(), r.getChkNum());
        r.setMissData(miss ? Integer.valueOf(1) : Integer.valueOf(0));

        // —— 看数据其二：报文按本拨唯一切法过一遍，切不动单行标出，不按住整拨 ——
        List<String> pieces = FlowReceiveRules.cutPayload(
                cutKind, r.getPayload(), delimiter, fixedWidths);
        r.setParseFail(pieces == null ? Integer.valueOf(1) : Integer.valueOf(0));

        // —— 看数据其三：只认点位码，请底册 ——
        TNslpPoint point = null;
        int shape = FlowReceiveRules.codeShape(r.getItemCode());
        String code = r.getItemCode() == null ? null : r.getItemCode().trim();
        if (shape == 0) {
            point = book.get(code);
            if (point != null) {
                r.setItemCode(code);
            }
        }

        // 报文题名/区册代号只拿来与底册比对，不合以底册为准、回执单列
        boolean mismatch = FlowReceiveRules.bookMismatch(r.getRawTitle(), r.getRawZoneNo(), point);
        r.setBookMismatch(mismatch ? Integer.valueOf(1) : Integer.valueOf(0));

        // —— 定去处：条款一条条挑，空=收下，非空=退回且写明每一条 ——
        List<String> codes = FlowReceiveRules.rejectReasons(r, point);
        if (codes.isEmpty()) {
            r.setStatus(Integer.valueOf(FlowReceiveRules.STATUS_ACCEPTED));
            r.setRejectCode(null);
            r.setRejectReason(null);
        } else {
            r.setStatus(Integer.valueOf(FlowReceiveRules.STATUS_REJECTED));
            r.setRejectCode(FlowReceiveRules.joinCodes(codes));
            r.setRejectReason(FlowReceiveRules.joinReasons(codes));
        }
    }

    /** 回执三数的唯一数法：回流水行里逐行数，页面那份只是转述 */
    private FlowReceipt buildReceipt(String batchNo) {
        TNslpFlowBatch batch = this.flowBatchMapper.selectOne(new QueryWrapper<TNslpFlowBatch>()
                .eq("batch_no", batchNo).eq("del_flag", 0));
        if (batch == null) {
            throw new FlowReceiveException("批次码 " + batchNo + " 在流水上无拨抬头，不出回执");
        }
        List<TNslpFlowRow> all = listRowsAllLayers(batchNo);
        FlowReceipt rc = new FlowReceipt();
        rc.setBatchNo(batchNo);
        rc.setArriveSeq(batch.getArriveSeq() == null ? 1 : batch.getArriveSeq().intValue());
        rc.setCapNum(batch.getCapNum() == null ? FlowReceiveRules.DEFAULT_CAP : batch.getCapNum().intValue());
        rc.setFirstCount(batch.getFirstCount() == null ? 0 : batch.getFirstCount().intValue());

        int total = 0;
        int accepted = 0;
        int rejected = 0;
        int pending = 0;
        List<TNslpFlowRow> mismatchRows = new ArrayList<TNslpFlowRow>();
        List<TNslpFlowRow> parseFailRows = new ArrayList<TNslpFlowRow>();
        for (TNslpFlowRow r : all) {
            total++;
            int st = r.getStatus() == null ? -1 : r.getStatus().intValue();
            if (st == FlowReceiveRules.STATUS_ACCEPTED) {
                accepted++;
            } else if (st == FlowReceiveRules.STATUS_REJECTED) {
                rejected++;
            } else if (st == FlowReceiveRules.STATUS_PENDING) {
                pending++;
            }
            if (r.getBookMismatch() != null && r.getBookMismatch().intValue() == 1) {
                mismatchRows.add(r);
            }
            if (r.getParseFail() != null && r.getParseFail().intValue() == 1) {
                parseFailRows.add(r);
            }
        }
        rc.setTotalCount(total);
        rc.setAcceptedCount(accepted);
        rc.setRejectedCount(rejected);
        rc.setPendingCount(pending);
        rc.setBalanced(pending == 0 && accepted + rejected == total);
        rc.setBookMismatchRows(mismatchRows);
        rc.setParseFailRows(parseFailRows);
        return rc;
    }

    /**
     * 整行显式落一遍（含清空白）。updateById 默认不更新 null 字段，
     * 重报改收下要清掉 reject_code/reject_reason、历史册层取出要清掉 layer_at，
     * 故照本工程 applyVerdict 的写法走显式 set 的 UpdateWrapper。
     */
    private void overwriteRow(TNslpFlowRow r) {
        UpdateWrapper<TNslpFlowRow> uw = new UpdateWrapper<TNslpFlowRow>()
                .eq("id", r.getId())
                .set("batch_no", r.getBatchNo())
                .set("row_no", r.getRowNo())
                .set("arrive_seq", r.getArriveSeq())
                .set("item_code", r.getItemCode())
                .set("qty", r.getQty())
                .set("chk_num", r.getChkNum())
                .set("miss_data", r.getMissData())
                .set("payload", r.getPayload())
                .set("parse_fail", r.getParseFail())
                .set("put_kind", r.getPutKind())
                .set("raw_title", r.getRawTitle())
                .set("raw_zone_no", r.getRawZoneNo())
                .set("book_mismatch", r.getBookMismatch())
                .set("status", r.getStatus())
                .set("reject_code", r.getRejectCode())
                .set("reject_reason", r.getRejectReason())
                .set("layer", r.getLayer())
                .set("layer_at", r.getLayerAt())
                .set("update_time", r.getUpdateTime())
                .set("update_by", r.getUpdateBy());
        this.flowRowMapper.update(null, uw);
    }

    /** 本拨全部行（含历史册层），按行次升序——数三数、认重报都从这一面取 */    private List<TNslpFlowRow> listRowsAllLayers(String batchNo) {
        return this.flowRowMapper.selectList(new QueryWrapper<TNslpFlowRow>()
                .eq("batch_no", batchNo).eq("del_flag", 0).orderByAsc("row_no"));
    }

    /** 底册只请一面：本趟出现过的、形状齐整的点位码一次取齐 */
    private Map<String, TNslpPoint> loadBook(List<TNslpFlowRow> incoming) {
        Map<String, TNslpPoint> book = new HashMap<String, TNslpPoint>();
        List<String> codes = new ArrayList<String>();
        for (TNslpFlowRow r : incoming) {
            if (r == null || r.getItemCode() == null) {
                continue;
            }
            String code = r.getItemCode().trim();
            int shape = FlowReceiveRules.codeShape(code);
            if (shape == 0 && !codes.contains(code)) {
                codes.add(code);
            }
        }
        if (!codes.isEmpty()) {
            List<TNslpPoint> points = this.pointMapper.selectList(new QueryWrapper<TNslpPoint>()
                    .in("point_no", codes).eq("del_flag", 0));
            for (TNslpPoint p : points) {
                if (p.getPointNo() != null) {
                    book.put(p.getPointNo().trim(), p);
                }
            }
        }
        return book;
    }

    /** 重报认账键：同批次下同一处点位的同一来路（直采、补报两行 key 不同，各认各的） */
    private static String reissueKey(TNslpFlowRow r) {
        if (r == null || r.getItemCode() == null || r.getItemCode().trim().isEmpty()) {
            // 码没填的退回行无从认账，重报只能按新行续号——它本就没有可追的行面
            return null;
        }
        int shape = FlowReceiveRules.codeShape(r.getItemCode());
        if (shape != 0) {
            return null;
        }
        String put = r.getPutKind() == null ? "" : r.getPutKind().trim();
        return r.getItemCode().trim() + "||" + put;
    }

    private static boolean alreadyOnBooks(String key, List<TNslpFlowRow> existing) {
        if (key == null) {
            return false;
        }
        for (TNslpFlowRow r : existing) {
            if (key.equals(reissueKey(r))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsCode(String joined, String code) {
        for (String c : joined.split(",")) {
            if (code.equals(c.trim())) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeCutKind(String cutKind) {
        if (cutKind == null || cutKind.trim().isEmpty()) {
            throw new FlowReceiveException("报文切法没报：按分隔符还是按定宽，一拨之内只许一种，拨形不成");
        }
        String k = cutKind.trim();
        if (!FlowReceiveRules.CUT_DELIMITER.equals(k) && !FlowReceiveRules.CUT_FIXED.equals(k)) {
            throw new FlowReceiveException("报文切法只许 delimiter/fixed 两种，给的是「" + k + "」，拨形不成");
        }
        return k;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String statusText(int status) {
        if (status == FlowReceiveRules.STATUS_ACCEPTED) {
            return "收下";
        }
        if (status == FlowReceiveRules.STATUS_REJECTED) {
            return "退回";
        }
        if (status == FlowReceiveRules.STATUS_PENDING) {
            return "未勾";
        }
        return "第四种写法（不许落库）";
    }

    /** 重报配对：进门行 + 它要接续的当初那一行 */
    private static final class ReissuePair {
        private final TNslpFlowRow incoming;
        private final TNslpFlowRow landed;

        private ReissuePair(TNslpFlowRow incoming, TNslpFlowRow landed) {
            this.incoming = incoming;
            this.landed = landed;
        }
    }
}
