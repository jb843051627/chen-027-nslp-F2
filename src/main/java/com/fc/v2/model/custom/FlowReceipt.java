package com.fc.v2.model.custom;

import java.util.ArrayList;
import java.util.List;

import com.fc.v2.model.auto.TNslpFlowRow;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 核收回执。只写三个数：本拨行数、收下行数、退回行数，后两个加起来等于第一个。
 * 三个数一律回到流水里一行一行数出来，页面那份只是转述——同一次计算两处说话。
 * 报文题名/区册代号与底册不合的行另开一单列出（去处仍以底册为准，不因其被退回）。
 *
 * @author fuce
 * @date 2026-10-04
 */
@ApiModel(value = "FlowReceipt", description = "核收回执（三数同行里数出来）")
public class FlowReceipt implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "批次码")
    private String batchNo;

    /** 这一趟是该批次码第几遍落到流水上 1首遍 2照面 */
    @ApiModelProperty(value = "到达遍数 1首遍 2照面")
    private int arriveSeq;

    /** 抬头写定的顶（空/负/离谱已按300认） */
    @ApiModelProperty(value = "认定后的顶")
    private int capNum;

    /** 首遍进门点出来的行数（冻结存底，第二遍不重数） */
    @ApiModelProperty(value = "首遍点行数")
    private int firstCount;

    /** 本拨行数：行里一行一行数出来的总数（两遍及册行合并口径） */
    @ApiModelProperty(value = "本拨行数")
    private int totalCount;

    /** 收下的行数（status=1 逐行数） */
    @ApiModelProperty(value = "收下行数")
    private int acceptedCount;

    /** 退回的行数（status=2 逐行数） */
    @ApiModelProperty(value = "退回行数")
    private int rejectedCount;

    /** 空悬未勾的行数（新拨应为0；只可能是先前入过账的存量，满两年度由历史册层收口） */
    @ApiModelProperty(value = "未勾行数（新拨为0）")
    private int pendingCount;

    /** 收 + 退 是否等于本拨行数（且无未勾存量）；false 即两处对不住，不认「大致相符」 */
    @ApiModelProperty(value = "三数是否轧平")
    private boolean balanced;

    /** 报文题名/区册代号与底册不合、以底册为准的行，回执单列 */
    @ApiModelProperty(value = "与底册不合单列的行")
    private List<TNslpFlowRow> bookMismatchRows = new ArrayList<TNslpFlowRow>();

    /** 切不动、递给回核看的行（退回行里 parse_fail=1 者，单行标出） */
    @ApiModelProperty(value = "切不动递回核的行")
    private List<TNslpFlowRow> parseFailRows = new ArrayList<TNslpFlowRow>();

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public int getArriveSeq() {
        return arriveSeq;
    }

    public void setArriveSeq(int arriveSeq) {
        this.arriveSeq = arriveSeq;
    }

    public int getCapNum() {
        return capNum;
    }

    public void setCapNum(int capNum) {
        this.capNum = capNum;
    }

    public int getFirstCount() {
        return firstCount;
    }

    public void setFirstCount(int firstCount) {
        this.firstCount = firstCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getAcceptedCount() {
        return acceptedCount;
    }

    public void setAcceptedCount(int acceptedCount) {
        this.acceptedCount = acceptedCount;
    }

    public int getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(int rejectedCount) {
        this.rejectedCount = rejectedCount;
    }

    public int getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(int pendingCount) {
        this.pendingCount = pendingCount;
    }

    public boolean isBalanced() {
        return balanced;
    }

    public void setBalanced(boolean balanced) {
        this.balanced = balanced;
    }

    public List<TNslpFlowRow> getBookMismatchRows() {
        return bookMismatchRows;
    }

    public void setBookMismatchRows(List<TNslpFlowRow> bookMismatchRows) {
        this.bookMismatchRows = bookMismatchRows;
    }

    public List<TNslpFlowRow> getParseFailRows() {
        return parseFailRows;
    }

    public void setParseFailRows(List<TNslpFlowRow> parseFailRows) {
        this.parseFailRows = parseFailRows;
    }
}
