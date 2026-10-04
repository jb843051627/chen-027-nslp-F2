package com.fc.v2.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TNslpFlowRowMapper;
import com.fc.v2.model.auto.TNslpFlowRow;
import com.fc.v2.service.ITNslpFlowRowService;

/**
 * 监测流水核收行 Service业务层处理（batch-process 形状：整批提交）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TNslpFlowRowServiceImpl implements ITNslpFlowRowService {

    private static final int MAX_ROWS = 500;
    private static final int STATUS_OK = 1;
    private static final int STATUS_FAIL = 2;

    @javax.annotation.Resource
    private TNslpFlowRowMapper nslpFlowRowMapper;

    @Override
    public TNslpFlowRow selectTNslpFlowRowById(Long id) {
        return this.nslpFlowRowMapper.selectById(id);
    }

    @Override
    public int submitBatch(String batchNo, List<TNslpFlowRow> rows) {
        String no = rows.get(0).getBatchNo();
        java.util.List<TNslpFlowRow> errors = new java.util.ArrayList<TNslpFlowRow>();
        int seq = 0;
        for (TNslpFlowRow r : rows) {
            if (r.getItemCode() == null || r.getItemCode().trim().isEmpty()
                    || r.getQty() == null
                    || r.getQty().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                seq++;
                r.setRowNo(Integer.valueOf(seq));
                r.setBatchNo(no);
                r.setStatus(STATUS_FAIL);
                this.nslpFlowRowMapper.insert(r);
                errors.add(r);
            }
        }
        if (!errors.isEmpty()) {
            return 0;
        }
        int ok = 0;
        for (TNslpFlowRow r : rows) {
            r.setBatchNo(no);
            r.setStatus(STATUS_OK);
            this.nslpFlowRowMapper.insert(r);
            ok++;
        }
        return ok;
    }

    @Override
    public List<TNslpFlowRow> listErrors(String batchNo) {
        return this.nslpFlowRowMapper.selectList(new QueryWrapper<TNslpFlowRow>()
                .eq("batch_no", batchNo).eq("status", STATUS_FAIL));
    }
}
