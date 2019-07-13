package com.sankuai.inf.leaf.client.handler;

import com.sankuai.inf.leaf.client.utils.RingBuffer;

/**
 * @author qinshuang1998
 * @date 2019/7/11
 */
public interface RejectedTakeBufferHandler {

    /**
     * Reject take buffer request
     *
     * @param ringBuffer
     */
    void rejectTakeBuffer(RingBuffer ringBuffer);
}
