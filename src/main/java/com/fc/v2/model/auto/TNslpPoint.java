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
 * 声环境监测点位底册对象 t_nslp_point
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_nslp_point")
@ApiModel(value = "TNslpPoint", description = "声环境监测点位底册")
public class TNslpPoint implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 点位码 */
    @TableField("point_no")
    @ApiModelProperty(value = "点位码")
    private String pointNo;

    /** 挂在哪一个区册名下 */
    @TableField("zone_id")
    @ApiModelProperty(value = "挂在哪一个区册名下")
    private Integer zoneId;

    /** 区册代号（冗余自区册） */
    @TableField("zone_no")
    @ApiModelProperty(value = "区册代号（冗余自区册）")
    private String zoneNo;

    /** 来路（自动站／移动监测车／人工核查三条来路） */
    @TableField("src_kind")
    @ApiModelProperty(value = "来路（自动站／移动监测车／人工核查三条来路）")
    private String srcKind;

    /** 核定应测次数 */
    @TableField("need_num")
    @ApiModelProperty(value = "核定应测次数")
    private Integer needNum;

    /** 已核验达标的次数 */
    @TableField("got_num")
    @ApiModelProperty(value = "已核验达标的次数")
    private Integer gotNum;

    /** 还差几回（轧出来，不手填） */
    @TableField("lack_num")
    @ApiModelProperty(value = "还差几回（轧出来，不手填）")
    private Integer lackNum;

    /** 随件交来的点位题名与依附声源 */
    @TableField("content")
    @ApiModelProperty(value = "随件交来的点位题名与依附声源")
    private String content;

    /** 册面情形 0新入册 1已核齐 2缺项 */
    @TableField("status")
    @ApiModelProperty(value = "册面情形 0新入册 1已核齐 2缺项")
    private Integer status;

    /** 归层时钉住的线代号；不归层清成空白（日后追得出当年照的是哪条线） */
    @TableField("pinned_rule_code")
    @ApiModelProperty(value = "归层钉住的线代号，不归层为空白")
    private String pinnedRuleCode;

    /** 归层时钉住的起用那一一日；不归层清成空白 */
    @TableField("pinned_eff_start")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "归层钉住的起用那一一日，不归层为空白")
    private Date pinnedEffStart;

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

    public String getPointNo() {
        return pointNo;
    }

    public void setPointNo(String pointNo) {
        this.pointNo = pointNo;
    }

    public Integer getZoneId() {
        return zoneId;
    }

    public void setZoneId(Integer zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneNo() {
        return zoneNo;
    }

    public void setZoneNo(String zoneNo) {
        this.zoneNo = zoneNo;
    }

    public String getSrcKind() {
        return srcKind;
    }

    public void setSrcKind(String srcKind) {
        this.srcKind = srcKind;
    }

    public Integer getNeedNum() {
        return needNum;
    }

    public void setNeedNum(Integer needNum) {
        this.needNum = needNum;
    }

    public Integer getGotNum() {
        return gotNum;
    }

    public void setGotNum(Integer gotNum) {
        this.gotNum = gotNum;
    }

    public Integer getLackNum() {
        return lackNum;
    }

    public void setLackNum(Integer lackNum) {
        this.lackNum = lackNum;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getPinnedRuleCode() {
        return pinnedRuleCode;
    }

    public void setPinnedRuleCode(String pinnedRuleCode) {
        this.pinnedRuleCode = pinnedRuleCode;
    }

    public Date getPinnedEffStart() {
        return pinnedEffStart;
    }

    public void setPinnedEffStart(Date pinnedEffStart) {
        this.pinnedEffStart = pinnedEffStart;
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
