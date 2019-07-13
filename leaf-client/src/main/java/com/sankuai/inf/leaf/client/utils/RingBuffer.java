package com.sankuai.inf.leaf.client.utils;

import com.sankuai.inf.leaf.client.handler.RejectedPutBufferHandler;
import com.sankuai.inf.leaf.client.handler.RejectedTakeBufferHandler;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author qinshuang1998
 * @date 2019/7/11
 */
public class RingBuffer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RingBuffer.class);

    /**
     * Constants
     */
    private static final int START_POINT = -1;
    // 用于标记当前slot的状态，表示可以put一个id进去
    private static final long CAN_PUT_FLAG = 0L;
    // 用于标记当前slot的状态，表示可以take一个id
    private static final long CAN_TAKE_FLAG = 1L;
    // 用于控制何时填充slots的默认阈值：当剩余的可用的slot的个数，小于bufferSize的50%时，需要生成id将slots填满
    public static final int DEFAULT_PADDING_PERCENT = 50;

    /**
     * The size of RingBuffer's slots, each slot hold a UID
     */
    // slots的大小
    private final int bufferSize;
    private final long indexMask;
    // slots用于缓存已经生成的id
    private final long[] slots;
    // flags用于存储id的状态(是否可填充、是否可消费)
    private final PaddedAtomicLong[] flags;

    /**
     * Tail: last position sequence to produce
     */
    // Tail指针
    // 表示Producer生产的最大序号(此序号从0开始，持续递增)。Tail不能超过Cursor，即生产者不能覆盖未消费的slot
    // 当Tail已赶上curosr，此时可通过rejectedPutBufferHandler指定PutRejectPolicy
    private final AtomicLong tail = new PaddedAtomicLong(START_POINT);

    /**
     * Cursor: current position sequence to consume
     */
    // 表示Consumer消费到的最小序号(序号序列与Producer序列相同)。Cursor不能超过Tail，即不能消费未生产的slot
    // 当Cursor已赶上tail，此时可通过rejectedTakeBufferHandler指定TakeRejectPolicy
    private final AtomicLong cursor = new PaddedAtomicLong(START_POINT);

    /**
     * Threshold for trigger padding buffer
     */
    // 用于控制何时填充slots的阈值
    private final int paddingThreshold;

    /**
     * Reject put/take buffer handle policy
     */
    // 当slots满了，无法继续put时的处理策略。默认实现：无法进行put，仅记录日志
    private RejectedPutBufferHandler rejectedPutHandler = this::discardPutBuffer;
    // 当slots空了，无法继续take时的处理策略。默认实现：仅抛出异常
    private RejectedTakeBufferHandler rejectedTakeHandler = this::exceptionRejectedTakeBuffer;

    /**
     * Executor of padding buffer
     */
    // 用于运行【生成id将slots填满】任务
    private BufferPaddingExecutor bufferPaddingExecutor;

    /**
     * Constructor with buffer size, paddingFactor default as {@value #DEFAULT_PADDING_PERCENT}
     *
     * @param bufferSize must be positive & a power of 2
     */
    public RingBuffer(int bufferSize) {
        this(bufferSize, DEFAULT_PADDING_PERCENT);
    }

    /**
     * Constructor with buffer size & padding factor
     *
     * @param bufferSize    must be positive & a power of 2
     * @param paddingFactor percent in (0 - 100). When the count of rest available UIDs reach the threshold, it will trigger padding buffer<br>
     *                      Sample: paddingFactor=20, bufferSize=1000 -> threshold=1000 * 20 /100,
     *                      padding buffer will be triggered when tail-cursor<threshold
     */
    public RingBuffer(int bufferSize, int paddingFactor) {
        // check buffer size is positive & a power of 2; padding factor in (0, 100)
        Assert.assertTrue("RingBuffer size must be positive", bufferSize > 0L);
        Assert.assertTrue("RingBuffer size must be a power of 2", Integer.bitCount(bufferSize) == 1);
        Assert.assertTrue("RingBuffer size must be positive", paddingFactor > 0 && paddingFactor < 100);
        this.bufferSize = bufferSize;
        this.indexMask = bufferSize - 1;
        this.slots = new long[bufferSize];
        this.flags = initFlags(bufferSize);

        this.paddingThreshold = bufferSize * paddingFactor / 100;
    }

    /**
     * Put an UID in the ring & tail moved<br>
     * We use 'synchronized' to guarantee the UID fill in slot & publish new tail sequence as atomic operations<br>
     *
     * <b>Note that: </b> It is recommended to put UID in a serialize way, cause we once batch generate a series UIDs and put
     * the one by one into the buffer, so it is unnecessary put in multi-threads
     *
     * @param uid
     * @return false means that the buffer is full, apply {@link RejectedPutBufferHandler}
     */
    public synchronized boolean put(long uid) {
        long currentTail = tail.get();
        long currentCursor = cursor.get();

        // tail catches the cursor, means that you can't put any cause of RingBuffer is full
        long distance = currentTail - (currentCursor == START_POINT ? 0 : currentCursor);
        if (distance == bufferSize - 1) {
            rejectedPutHandler.rejectPutBuffer(this, uid);
            return false;
        }

        // 1. pre-check whether the flag is CAN_PUT_FLAG
        int nextTailIndex = calSlotIndex(currentTail + 1);
        if (flags[nextTailIndex].get() != CAN_PUT_FLAG) {
            rejectedPutHandler.rejectPutBuffer(this, uid);
            return false;
        }

        // 2. put UID in the next slot
        // 3. update next slot' flag to CAN_TAKE_FLAG
        // 4. publish tail with sequence increase by one
        slots[nextTailIndex] = uid;
        flags[nextTailIndex].set(CAN_TAKE_FLAG);
        tail.incrementAndGet();

        // The atomicity of operations above, guarantees by 'synchronized'. In another word,
        // the take operation can't consume the UID we just put, until the tail is published(tail.incrementAndGet())
        return true;
    }

    /**
     * Take an UID of the ring at the next cursor, this is a lock free operation by using atomic cursor<p>
     * <p>
     * Before getting the UID, we also check whether reach the padding threshold,
     * the padding buffer operation will be triggered in another thread<br>
     * If there is no more available UID to be taken, the specified {@link RejectedTakeBufferHandler} will be applied<br>
     *
     * @return UID
     * @throws IllegalStateException if the cursor moved back
     */
    public long take() {
        // spin get next available cursor
        long currentCursor = cursor.get();
        long nextCursor = cursor.updateAndGet(old -> old == tail.get() ? old : old + 1);

        // check for safety consideration, it never occurs
        Assert.assertTrue("Curosr can't move back", nextCursor >= currentCursor);

        // trigger padding in an async-mode if reach the threshold
        long currentTail = tail.get();
        if (currentTail - nextCursor < paddingThreshold) {
            LOGGER.info("Reach the padding threshold:{}. tail:{}, cursor:{}, rest:{}", paddingThreshold, currentTail,
                    nextCursor, currentTail - nextCursor);
            bufferPaddingExecutor.asyncPadding();
        }

        // cursor catch the tail, means that there is no more available UID to take
        if (nextCursor == currentCursor) {
            rejectedTakeHandler.rejectTakeBuffer(this);
        }

        // 1. check next slot flag is CAN_TAKE_FLAG
        int nextCursorIndex = calSlotIndex(nextCursor);
        Assert.assertTrue("Curosr not in can take status", flags[nextCursorIndex].get() == CAN_TAKE_FLAG);

        // 2. get UID from next slot
        // 3. set next slot flag as CAN_PUT_FLAG.
        long uid = slots[nextCursorIndex];
        flags[nextCursorIndex].set(CAN_PUT_FLAG);

        // Note that: Step 2,3 can not swap. If we set flag before get value of slot, the producer may overwrite the
        // slot with a new UID, and this may cause the consumer take the UID twice after walk a round the ring
        return uid;
    }

    /**
     * Calculate slot index with the slot sequence (sequence % bufferSize)
     */
    protected int calSlotIndex(long sequence) {
        return (int) (sequence & indexMask);
    }

    /**
     * Discard policy for {@link RejectedPutBufferHandler}, we just do logging
     */
    protected void discardPutBuffer(RingBuffer ringBuffer, long uid) {
        LOGGER.warn("Rejected putting buffer for uid:{}. {}", uid, ringBuffer);
    }

    /**
     * Policy for {@link RejectedTakeBufferHandler}, throws {@link RuntimeException} after logging
     */
    protected void exceptionRejectedTakeBuffer(RingBuffer ringBuffer) {
        LOGGER.warn("Rejected take buffer. {}", ringBuffer);
        throw new RuntimeException("Rejected take buffer. " + ringBuffer);
    }

    /**
     * Initialize flags as CAN_PUT_FLAG
     */
    private PaddedAtomicLong[] initFlags(int bufferSize) {
        PaddedAtomicLong[] flags = new PaddedAtomicLong[bufferSize];
        for (int i = 0; i < bufferSize; i++) {
            flags[i] = new PaddedAtomicLong(CAN_PUT_FLAG);
        }

        return flags;
    }

    /**
     * Getters
     */
    public long getTail() {
        return tail.get();
    }

    public long getCursor() {
        return cursor.get();
    }

    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Setters
     */
    public void setBufferPaddingExecutor(BufferPaddingExecutor bufferPaddingExecutor) {
        this.bufferPaddingExecutor = bufferPaddingExecutor;
    }

    public void setRejectedPutHandler(RejectedPutBufferHandler rejectedPutHandler) {
        this.rejectedPutHandler = rejectedPutHandler;
    }

    public void setRejectedTakeHandler(RejectedTakeBufferHandler rejectedTakeHandler) {
        this.rejectedTakeHandler = rejectedTakeHandler;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RingBuffer [bufferSize=").append(bufferSize)
                .append(", tail=").append(tail)
                .append(", cursor=").append(cursor)
                .append(", paddingThreshold=").append(paddingThreshold).append("]");

        return builder.toString();
    }

}
