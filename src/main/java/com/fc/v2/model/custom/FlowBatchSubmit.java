package com.fc.v2.model.custom;

import java.util.List;

import com.fc.v2.model.auto.TNslpFlowRow;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 一拨核收进门的样子：一拨一次交齐，不收零散逐行。
 * 顶只认抬头那一格 declaredCap（空着/负数/离谱大数按 300），别处不再设第二个来源；
 * 切法 cutKind 一拨之内只许一种，定宽切法时取 fixedWidths。
 *
 * @author fuce
 * @date 2026-10-04
 */
@ApiModel(value = "FlowBatchSubmit", description = "一拨监测流水核收入门（整拨一次交齐）")
public class FlowBatchSubmit implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /** 批次码：一拨一个；同码再到只算照面，不另起一拨 */
    @ApiModelProperty(value = "批次码", required = true)
    private String batchNo;

    /** 拨抬头那一格写定的顶；空着不填按300，负数/离谱大数当没填 */
    @ApiModelProperty(value = "抬头写定的顶（空按300）")
    private Integer declaredCap;

    /** 报文切法 delimiter按分隔符 fixed按定宽；一拨之内只许一种 */
    @ApiModelProperty(value = "切法 delimiter/fixed", required = true)
    private String cutKind;

    /** 分隔符切法的分隔符，留空认逗号 */
    @ApiModelProperty(value = "分隔符，空为逗号")
    private String delimiter;

    /** 定宽切法各段宽度（与读数串总长逐格对齐） */
    @ApiModelProperty(value = "定宽切法各段宽度")
    private List<Integer> fixedWidths;

    /** 本拨交来的行（一行对一处点位的一回读数，不许拆拨、不许挪到别拨） */
    @ApiModelProperty(value = "本拨各行", required = true)
    private List<TNslpFlowRow> rows;

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Integer getDeclaredCap() {
        return declaredCap;
    }

    public void setDeclaredCap(Integer declaredCap) {
        this.declaredCap = declaredCap;
    }

    public String getCutKind() {
        return cutKind;
    }

    public void setCutKind(String cutKind) {
        this.cutKind = cutKind;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public List<Integer> getFixedWidths() {
        return fixedWidths;
    }

    public void setFixedWidths(List<Integer> fixedWidths) {
        this.fixedWidths = fixedWidths;
    }

    public List<TNslpFlowRow> getRows() {
        return rows;
    }

    public void setRows(List<TNslpFlowRow> rows) {
        this.rows = rows;
    }
}
