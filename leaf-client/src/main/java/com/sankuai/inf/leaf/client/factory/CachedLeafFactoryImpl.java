package com.sankuai.inf.leaf.client.factory;

import com.sankuai.inf.leaf.client.config.LeafClientConfig;
import com.sankuai.inf.leaf.client.exception.LeafClientException;
import com.sankuai.inf.leaf.client.handler.RejectedPutBufferHandler;
import com.sankuai.inf.leaf.client.handler.RejectedTakeBufferHandler;
import com.sankuai.inf.leaf.client.utils.BufferPaddingExecutor;
import com.sankuai.inf.leaf.client.utils.RingBuffer;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author qinshuang1998
 * @date 2019/7/12
 */
public class CachedLeafFactoryImpl extends DefaultLeafFactoryImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(CachedLeafFactoryImpl.class);

    private int paddingFactor = RingBuffer.DEFAULT_PADDING_PERCENT;
    private Long scheduleInterval;

    private RingBuffer ringBuffer;
    private BufferPaddingExecutor bufferPaddingExecutor;

    private RejectedPutBufferHandler rejectedPutBufferHandler;
    private RejectedTakeBufferHandler rejectedTakeBufferHandler;

    private int bufferSize = LeafClientConfig.getInstance().getCachedSize();

    protected List<Long> nextIdsProvider() {
        // provide bufferSize + 1
        // let (BufferPaddingExecutor -> paddingBuffer -> isFullRingBuffer) become true at first loop
        List<Long> uidList = new ArrayList<>(bufferSize + 1);
        for (int offset = 1; offset < bufferSize + 1; offset++) {
            uidList.add(super.nextId());
        }
        return uidList;
    }

    @Override
    public void init() {
        super.init();
        // initialize RingBuffer
        this.ringBuffer = new RingBuffer(bufferSize, paddingFactor);
        LOGGER.info("Initialized ring buffer size:{}, paddingFactor:{}", bufferSize, paddingFactor);

        // initialize RingBufferPaddingExecutor
        boolean usingSchedule = (scheduleInterval != null);
        this.bufferPaddingExecutor = new BufferPaddingExecutor(ringBuffer, this::nextIdsProvider, usingSchedule);
        if (usingSchedule) {
            bufferPaddingExecutor.setScheduleInterval(scheduleInterval);
        }

        LOGGER.info("Initialized BufferPaddingExecutor. Using schdule:{}, interval:{}", usingSchedule, scheduleInterval);

        // set rejected put/take handle policy
        this.ringBuffer.setBufferPaddingExecutor(bufferPaddingExecutor);
        if (rejectedPutBufferHandler != null) {
            this.ringBuffer.setRejectedPutHandler(rejectedPutBufferHandler);
        }
        if (rejectedTakeBufferHandler != null) {
            this.ringBuffer.setRejectedTakeHandler(rejectedTakeBufferHandler);
        }

        // fill in all slots of the RingBuffer
        bufferPaddingExecutor.paddingBuffer();

        // start buffer padding threads
        bufferPaddingExecutor.start();
    }

    @Override
    public long nextId() {
        if (!initOk) {
            throw new LeafClientException("Must be initialized before use");
        }
        try {
            return ringBuffer.take();
        } catch (Exception e) {
            LOGGER.error("Generate unique id exception.", e);
            throw new LeafClientException(e);
        }
    }

    public void setRejectedPutBufferHandler(RejectedPutBufferHandler rejectedPutBufferHandler) {
        Assert.assertNotNull("RejectedPutBufferHandler can't be null!", rejectedPutBufferHandler);
        this.rejectedPutBufferHandler = rejectedPutBufferHandler;
    }

    public void setRejectedTakeBufferHandler(RejectedTakeBufferHandler rejectedTakeBufferHandler) {
        Assert.assertNotNull("RejectedTakeBufferHandler can't be null!", rejectedTakeBufferHandler);
        this.rejectedTakeBufferHandler = rejectedTakeBufferHandler;
    }

    public void setScheduleInterval(long scheduleInterval) {
        Assert.assertTrue("Schedule interval must positive!", scheduleInterval > 0);
        this.scheduleInterval = scheduleInterval;
    }
}
