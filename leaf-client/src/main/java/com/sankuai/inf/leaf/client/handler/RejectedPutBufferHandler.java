package com.sankuai.inf.leaf.client.handler;

import com.sankuai.inf.leaf.client.utils.RingBuffer;

/**
 * @author qinshuang1998
 * @date 2019/7/11
 */
public interface RejectedPutBufferHandler {

    /**
     * Reject put buffer request
     *
     * @param ringBuffer
     * @param uid
     */
    void rejectPutBuffer(RingBuffer ringBuffer, long uid);
}
