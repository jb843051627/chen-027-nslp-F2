package com.fc.v2.model.custom;

import com.fc.v2.model.auto.TNslpFlowRow;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 随手抽一行复核的回话：这一行的去处要和回执三个数互相印证。
 * 把行重新数一遍（不借页面那份），行落在哪个桶、三数轧不轧平、行与桶对不对得上，
 * 一项项给死；对不上时由调用方照行号点名，不认「大致相符」。
 *
 * @author fuce
 * @date 2026-10-04
 */
@ApiModel(value = "FlowRowCrossCheck", description = "抽行复核回话（去处与三数互印）")
public class FlowRowCrossCheck implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "被抽的行")
    private TNslpFlowRow row;

    @ApiModelProperty(value = "本拨行数（重数）")
    private int totalCount;

    @ApiModelProperty(value = "收下行数（重数）")
    private int acceptedCount;

    @ApiModelProperty(value = "退回行数（重数）")
    private int rejectedCount;

    @ApiModelProperty(value = "未勾行数（新拨应为0；存量未勾行计入历史册层口径）")
    private int pendingCount;

    @ApiModelProperty(value = "三数（含未勾存量）是否轧平")
    private boolean balanced;

    @ApiModelProperty(value = "本行去处与重数出的桶是否互相印证")
    private boolean consistent;

    @ApiModelProperty(value = "点名话：不一致时说清哪一行印不上")
    private String note;

    public TNslpFlowRow getRow() {
        return row;
    }

    public void setRow(TNslpFlowRow row) {
        this.row = row;
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

    public boolean isConsistent() {
        return consistent;
    }

    public void setConsistent(boolean consistent) {
        this.consistent = consistent;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
