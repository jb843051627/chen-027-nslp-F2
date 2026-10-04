package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TNslpCaseFlow;
import com.fc.v2.service.ITNslpCaseFlowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 噪声投诉与超标处置办件流转单 Controller（state-machine 形状：流转入口）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Api(value = "噪声投诉与超标处置办件流转单")
@Controller
@RequestMapping("/nslpCaseFlow")
public class NslpCaseFlowController extends BaseController {

    private final String prefix = "admin/nslpCaseFlow";

    @Autowired
    private ITNslpCaseFlowService nslpCaseFlowService;

    @ApiOperation(value = "流转台账跳转", notes = "流转台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("nslpCaseFlow:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "噪声投诉与超标处置办件流转单流转台账", action = "list")
    @ApiOperation(value = "流转台账", notes = "流转台账")
    @GetMapping("/list")
    @RequiresPermissions("nslpCaseFlow:list")
    @ResponseBody
    public ResultTable list(TNslpCaseFlow record) {
        QueryWrapper<TNslpCaseFlow> queryWrapper = new QueryWrapper<TNslpCaseFlow>();
        startPage();
        com.github.pagehelper.PageInfo<TNslpCaseFlow> page =
                new com.github.pagehelper.PageInfo<TNslpCaseFlow>(nslpCaseFlowService.selectTNslpCaseFlowList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "噪声投诉与超标处置办件流转单推进", action = "advance")
    @ApiOperation(value = "推进一档", notes = "推进一档")
    @PostMapping("/advance")
    @RequiresPermissions("nslpCaseFlow:advance")
    @ResponseBody
    public AjaxResult advance(Long id, String remark) {
        return toAjax(nslpCaseFlowService.advance(id, remark) != null ? 1 : 0);
    }

    @Log(title = "噪声投诉与超标处置办件流转单回退", action = "rollback")
    @ApiOperation(value = "回退一档", notes = "回退一档")
    @PostMapping("/rollback")
    @RequiresPermissions("nslpCaseFlow:rollback")
    @ResponseBody
    public AjaxResult rollback(Long id, String remark) {
        return toAjax(nslpCaseFlowService.rollback(id, remark) != null ? 1 : 0);
    }
}
