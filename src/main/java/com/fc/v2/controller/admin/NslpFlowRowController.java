package com.fc.v2.controller.admin;

import java.util.List;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.custom.FlowBatchSubmit;
import com.fc.v2.model.custom.FlowReceipt;
import com.fc.v2.model.custom.FlowRowCrossCheck;
import com.fc.v2.service.ITNslpFlowRowService;
import com.fc.v2.util.DateUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 声管科自动监测流水核收 Controller。
 *
 * 只有整拨进门与查询复核的口子：一拨一次交齐，不收零散逐行，
 * 不设单条增删改——自行落库与自行改写去向这两条路都堵死。
 *
 * @author fuce
 * @date 2026-10-04
 */
@Api(value = "声管科监测流水核收")
@Controller
@RequestMapping("/nslpFlow")
public class NslpFlowRowController extends BaseController {

    @Autowired
    private ITNslpFlowRowService nslpFlowRowService;

    @Log(title = "一拨监测流水进门核收", action = "receive")
    @ApiOperation(value = "整拨核收", notes = "点行数/看数据/定去处/出回执四动作一口气走完；同批次码再到只算照面")
    @PostMapping("/receive")
    @RequiresPermissions("nslp:flow:receive")
    @ResponseBody
    public AjaxResult receive(@RequestBody FlowBatchSubmit submit) {
        FlowReceipt receipt = nslpFlowRowService.receiveBatch(submit);
        // 回执以这一趟行里数出的那一份为凭，页面抢先摆出来的不算
        return AjaxResult.successData(200, receipt);
    }

    @Log(title = "核收回执查询", action = "receipt")
    @ApiOperation(value = "出回执", notes = "三数回流水里一行一行重数，页面那份只是转述")
    @GetMapping("/receipt")
    @RequiresPermissions("nslp:flow:view")
    @ResponseBody
    public AjaxResult receipt(String batchNo) {
        return AjaxResult.successData(200, nslpFlowRowService.receiptOf(batchNo));
    }

    @Log(title = "抽一行复核去处", action = "crossCheck")
    @ApiOperation(value = "抽行复核", notes = "行去处与三数互相印证，印不上的一行行点名")
    @GetMapping("/crossCheck")
    @RequiresPermissions("nslp:flow:view")
    @ResponseBody
    public AjaxResult crossCheck(Long id) {
        FlowRowCrossCheck cc = nslpFlowRowService.crossCheck(id);
        return AjaxResult.successData(200, cc);
    }

    @ApiOperation(value = "本拨现行行", notes = "按行次升序；历史册层的行不在现行名单")
    @GetMapping("/list")
    @RequiresPermissions("nslp:flow:view")
    @ResponseBody
    public AjaxResult list(String batchNo) {
        return AjaxResult.successData(200, nslpFlowRowService.listByBatch(batchNo));
    }

    @ApiOperation(value = "按退回条款问行", notes = "哪一行吃了哪一条，一句句问得出")
    @GetMapping("/rejected")
    @RequiresPermissions("nslp:flow:view")
    @ResponseBody
    public AjaxResult rejected(String batchNo, String rejectCode) {
        return AjaxResult.successData(200, nslpFlowRowService.listRejectedBy(batchNo, rejectCode));
    }

    @ApiOperation(value = "历史册层单独查", notes = "未勾满两年度转入，现行名单不列、这里查得着")
    @GetMapping("/history")
    @RequiresPermissions("nslp:flow:view")
    @ResponseBody
    public AjaxResult history(String batchNo) {
        return AjaxResult.successData(200, nslpFlowRowService.listHistoryLayer(batchNo));
    }

    @Log(title = "未勾行满两年度转入历史册层", action = "archive")
    @ApiOperation(value = "转历史册层", notes = "只由这一处作答，旁处不得自行挪动；只管存量未勾")
    @PostMapping("/archive")
    @RequiresPermissions("nslp:flow:archive")
    @ResponseBody
    public AjaxResult archive(String at) {
        int moved = nslpFlowRowService.archivePendingRows(DateUtils.parseDate(at));
        return AjaxResult.successData(200, moved);
    }

    @Log(title = "历史册层取出", action = "restore")
    @ApiOperation(value = "历史册层取出", notes = "转入与取出同一处作答")
    @PostMapping("/restore")
    @RequiresPermissions("nslp:flow:archive")
    @ResponseBody
    public AjaxResult restore(@RequestBody String[] ids, String operator) {
        List<Long> idList = new java.util.ArrayList<Long>();
        if (ids != null) {
            for (String s : ids) {
                if (s != null && !s.trim().isEmpty()) {
                    idList.add(Long.valueOf(s.trim()));
                }
            }
        }
        return AjaxResult.successData(200, nslpFlowRowService.restoreFromHistory(idList, operator));
    }
}
