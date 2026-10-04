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
import java.util.Date;

/**
 * 监测流水核收拨抬头对象 t_nslp_flow_batch（一拨一抬头）
 *
 * @author fuce
 * @date 2026-10-04
 */
@TableName("t_nslp_flow_batch")
@ApiModel(value = "TNslpFlowBatch", description = "监测流水核收拨抬头")
public class TNslpFlowBatch implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 批次码（一拨一个，重码到达只算照面，不另起一拨） */
    @TableField("batch_no")
    @ApiModelProperty(value = "批次码")
    private String batchNo;

    /** 拨抬头写定的顶（空着/负数/离谱大数按300认；顶只此一处，别无第二来源） */
    @TableField("cap_num")
    @ApiModelProperty(value = "写定的顶（只此一处）")
    private Integer capNum;

    /** 进门首遍点出来的行数（事后翻得出当初点了几行） */
    @TableField("first_count")
    @ApiModelProperty(value = "首遍点出来的行数")
    private Integer firstCount;

    /** 该批次码落到流水上第几遍 1首遍 2照面 */
    @TableField("arrive_seq")
    @ApiModelProperty(value = "到达遍数 1首遍 2照面")
    private Integer arriveSeq;

    /** 本报文切法 delimiter按分隔符 fixed按定宽（一拨之内只许一种） */
    @TableField("cut_kind")
    @ApiModelProperty(value = "报文切法 delimiter/fixed")
    private String cutKind;

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

    public Integer getCapNum() {
        return capNum;
    }

    public void setCapNum(Integer capNum) {
        this.capNum = capNum;
    }

    public Integer getFirstCount() {
        return firstCount;
    }

    public void setFirstCount(Integer firstCount) {
        this.firstCount = firstCount;
    }

    public Integer getArriveSeq() {
        return arriveSeq;
    }

    public void setArriveSeq(Integer arriveSeq) {
        this.arriveSeq = arriveSeq;
    }

    public String getCutKind() {
        return cutKind;
    }

    public void setCutKind(String cutKind) {
        this.cutKind = cutKind;
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
