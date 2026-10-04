package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 监测流水核收行对象 t_nslp_flow_row
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_nslp_flow_row")
@ApiModel(value = "TNslpFlowRow", description = "监测流水核收行")
public class TNslpFlowRow implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 监测流水批次码 */
    @TableField("batch_no")
    @ApiModelProperty(value = "监测流水批次码")
    private String batchNo;

    /** 本拨内行次（系统在本拨内排，不留手填格；被退的行重报仍显原行次） */
    @TableField("row_no")
    @ApiModelProperty(value = "本拨内行次")
    private Integer rowNo;

    /** 本行随该批次码第几遍落地 1首遍 2照面里的新行 */
    @TableField("arrive_seq")
    @ApiModelProperty(value = "随批次码第几遍落地")
    private Integer arriveSeq;

    /** 被对上的点位码（只认码，题名近似凑数不认） */
    @TableField("item_code")
    @ApiModelProperty(value = "被对上的点位码")
    private String itemCode;

    /** 本行读数次数（与达标次数一起到） */
    @TableField("qty")
    @ApiModelProperty(value = "本行读数次数")
    private BigDecimal qty;

    /** 本行达标次数 */
    @TableField("chk_num")
    @ApiModelProperty(value = "本行达标次数")
    private BigDecimal chkNum;

    /** 两数没一起到 0齐 1缺 */
    @TableField("miss_data")
    @ApiModelProperty(value = "两数没一起到 0齐 1缺")
    private Integer missData;

    /** 报文里那串读数原文 */
    @TableField("payload")
    @ApiModelProperty(value = "报文读数串原文")
    private String payload;

    /** 报文切不动标记 0切得动 1切不动（单行标出递给回核，不按整拨） */
    @TableField("parse_fail")
    @ApiModelProperty(value = "报文切不动 0否 1是")
    private Integer parseFail;

    /** 来路（自动站直采／监测车补报两条来路，各定各的去处） */
    @TableField("put_kind")
    @ApiModelProperty(value = "来路（自动站直采／监测车补报两条来路）")
    private String putKind;

    /** 报文带出的点位题名（只拿来与底册比对） */
    @TableField("raw_title")
    @ApiModelProperty(value = "报文点位题名")
    private String rawTitle;

    /** 报文带出的区册代号（只拿来与底册比对） */
    @TableField("raw_zone_no")
    @ApiModelProperty(value = "报文区册代号")
    private String rawZoneNo;

    /** 报文与点位底册不合 0合 1不合（以底册为准，回执单列） */
    @TableField("book_mismatch")
    @ApiModelProperty(value = "报文与底册不合 0合 1不合")
    private Integer bookMismatch;

    /** 行落地情形 0未勾 1已收下 2已退回（第四种不许落库；新拨只落1/2，0只存量里有） */
    @TableField("status")
    @ApiModelProperty(value = "行落地情形 0未勾 1已收下 2已退回")
    private Integer status;

    /** 退回吃了哪几条（码值逗号相连，一句句问得出） */
    @TableField("reject_code")
    @ApiModelProperty(value = "退回条款码")
    private String rejectCode;

    /** 退回写明为哪一条退（多条句句相连） */
    @TableField("reject_reason")
    @ApiModelProperty(value = "退回理由")
    private String rejectReason;

    /** 册层 0现行名单 1历史册层（未勾满两年度转入，不删行） */
    @TableField("layer")
    @ApiModelProperty(value = "册层 0现行 1历史")
    private Integer layer;

    /** 转入历史册层那一一日 */
    @TableField("layer_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "转入历史册层那一一日")
    private Date layerAt;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Integer getRowNo() {
        return rowNo;
    }

    public void setRowNo(Integer rowNo) {
        this.rowNo = rowNo;
    }

    public Integer getArriveSeq() {
        return arriveSeq;
    }

    public void setArriveSeq(Integer arriveSeq) {
        this.arriveSeq = arriveSeq;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public BigDecimal getChkNum() {
        return chkNum;
    }

    public void setChkNum(BigDecimal chkNum) {
        this.chkNum = chkNum;
    }

    public Integer getMissData() {
        return missData;
    }

    public void setMissData(Integer missData) {
        this.missData = missData;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Integer getParseFail() {
        return parseFail;
    }

    public void setParseFail(Integer parseFail) {
        this.parseFail = parseFail;
    }

    public String getPutKind() {
        return putKind;
    }

    public void setPutKind(String putKind) {
        this.putKind = putKind;
    }

    public String getRawTitle() {
        return rawTitle;
    }

    public void setRawTitle(String rawTitle) {
        this.rawTitle = rawTitle;
    }

    public String getRawZoneNo() {
        return rawZoneNo;
    }

    public void setRawZoneNo(String rawZoneNo) {
        this.rawZoneNo = rawZoneNo;
    }

    public Integer getBookMismatch() {
        return bookMismatch;
    }

    public void setBookMismatch(Integer bookMismatch) {
        this.bookMismatch = bookMismatch;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRejectCode() {
        return rejectCode;
    }

    public void setRejectCode(String rejectCode) {
        this.rejectCode = rejectCode;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public Integer getLayer() {
        return layer;
    }

    public void setLayer(Integer layer) {
        this.layer = layer;
    }

    public Date getLayerAt() {
        return layerAt;
    }

    public void setLayerAt(Date layerAt) {
        this.layerAt = layerAt;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
