package com.fc.v2.common.exception;

/**
 * 流水核收进门不合法（拨形不成、顶被顶破、切法混着来）。
 * 四动作一口气走完：进门不合规整拨不落地，回一句能交代的话；
 * 进门之后的单行毛病只退回该行，不许按住整拨。
 *
 * @author fuce
 * @date 2026-10-04
 */
public class FlowReceiveException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FlowReceiveException(String message) {
        super(message);
    }
}
