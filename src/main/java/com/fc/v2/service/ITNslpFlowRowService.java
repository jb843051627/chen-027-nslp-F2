package com.fc.v2.service;

import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TNslpFlowRow;
import com.fc.v2.model.custom.FlowBatchSubmit;
import com.fc.v2.model.custom.FlowReceipt;
import com.fc.v2.model.custom.FlowRowCrossCheck;

/**
 * 监测流水核收 Service 接口（核收四动作一口气走完，无逐行增删改入口）。
 *
 * 动作次序固定：点行数 → 看数据 → 定去处 → 出回执，中间不插人手。
 * 一拨一次交齐；同批次码再到只算照面——不翻首遍已落地的行、不另起一拨。
 *
 * @author fuce
 * @date 2026-10-04
 */
public interface ITNslpFlowRowService {

    /** 按主键回查单行（历史册层的行也照主键点得到，一条不减） */
    TNslpFlowRow selectTNslpFlowRowById(Long id);

    /**
     * 一拨进门，四个动作一口气走完，出这一趟的回执。
     *
     * 点行数：进门先点，点数连同抬头写定的顶（空/负/离谱按300）存进拨抬头；
     * 看数据：两数一起到（缺不代填不当0）、只认点位码、报文按一拨一种切法切，
     *        切不动的单行标出递回核，不按住整拨；
     * 定去处：只落收下/退回，退回逐条写明条款；同一点位两行（直采＋补报）各定各的；
     * 出回执：三数回流水里一行一行数出来，报文与底册不合的单列。
     *
     * 同一批次码第二遍到达：首遍的行一行不翻（不退回未勾），抬头不另起，
     * 新行接着原行次往后排；被退的行重报仍用当初行次、不重新排队。
     *
     * @throws com.fc.v2.common.exception.FlowReceiveException 拨形不成（空拨/无批次码/
     *         切法不明或混用/行数顶破抬头的顶）——整拨不落地
     */
    FlowReceipt receiveBatch(FlowBatchSubmit submit);

    /**
     * 出回执：回到流水里一行一行重数这一拨的三个数，与核收那趟同一次口径。
     * 页面那份只是转述，两处对不住以这里为准。
     */
    FlowReceipt receiptOf(String batchNo);

    /**
     * 随手抽一行复核：行的去处与重数出的三个数互相印证；印不上的一行行点名。
     * 不认「大致相符」。
     */
    FlowRowCrossCheck crossCheck(Long rowId);

    /** 本拨全部行（按行次升序；含已转入历史册层的行，点得到） */
    List<TNslpFlowRow> listByBatch(String batchNo);

    /** 本拨里吃了某条退回条款的行（一句句问得出哪行吃了哪条） */
    List<TNslpFlowRow> listRejectedBy(String batchNo, String rejectCode);

    /** 历史册层单独查：只列已转入的行（现行名单不列） */
    List<TNslpFlowRow> listHistoryLayer(String batchNo);

    /**
     * 未勾行空悬连着满两个年度，转入历史册层；现行名单不再列，历史册层单独查得着。
     * 转入与取出只由这一处作答，旁处不得自行挪动；只动 status=0 的存量，新拨走不到它跟前。
     *
     * @return 这一趟转入了几行
     */
    int archivePendingRows(Date at);

    /** 历史册层取出回现行名单（同样只由这一处作答）；返回取出的行数 */
    int restoreFromHistory(List<Long> rowIds, String operator);
}
