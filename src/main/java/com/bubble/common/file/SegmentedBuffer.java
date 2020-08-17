package com.bubble.common.file;

import java.util.Arrays;

/**
 * 代替标准JDK列表或缓冲区，以避免不断的重新分配。
 *
 * @author wugang
 * date: 2020-08-14 11:05
 **/
public class SegmentedBuffer {

    /**
     * Let's start with relatively small chunks
     */
    private final static int INITIAL_CHUNK_SIZE = 1024;

    /**
     * 将条目块增加一倍直到16k(对于32位机器来说是64k)
     */
    final static int MAX_CHUNK_SIZE = (1 << 14);

    private Node bufferHead;

    private Node bufferTail;

    /**
     * 存储在Node中（缓冲区中）的对象数组的总大小
     * 此缓冲区中缓冲的条目总数，包括跟随{@link #bufferHead}形成的链表中的所有实例。
     */
    private int bufferedEntryCount;

    /**
     * 可重用的对象数组，在释放缓冲区之后存储在这里，以前已经使用过。
     */
    private Object[] freeBuffer;

    public SegmentedBuffer() {

    }

    /**
     * 调用该方法以开始缓冲过程。
     * 将确保缓冲区为空，然后返回一个对象数组以开始对内容进行分块
     *
     * @return 对象数组
     */
    public Object[] resetAndStart() {
        if (this.bufferedEntryCount > 0) {
            reset();
        }
        if (this.freeBuffer == null) {
            return new Object[INITIAL_CHUNK_SIZE];
        }
        return this.freeBuffer;
    }

    private void reset() {
        /*
         * 我们可以在下一次重用最后一个（从而最大）的数组吗？
         */
        if (bufferedEntryCount > 0) {
            if (bufferTail != null) {
                Object[] obs = bufferTail.getData();
                // 另外，以防万一，我们也要清除其中的内容
                Arrays.fill(obs, null);
                freeBuffer = obs;
            }
            // 无论哪种方式，都必须丢弃当前内容
            bufferHead = bufferTail = null;
            bufferedEntryCount = 0;
        }
    }

    /**
     * 调用此方法以将完整的Object数组添加为此缓冲区中缓冲的块，并获取要填充的新数组。
     * 调用者不要使用它给定的数组。 但是要使用返回的数组继续缓冲。
     *
     * @param fullChunk 调用者请求追加到此缓冲区的已完成块。
     *                  通常是先前调用{@link #resetAndStart}或{@link #appendCompletedChunk}返回的块
     *                  （尽管这不是必需的也不是强制执行的）
     * @return 新的块缓冲区，供调用者填充
     */
    public Object[] appendCompletedChunk(Object[] fullChunk) {
        Node next = new Node(fullChunk);
        if (bufferHead == null) {
            // first chunk
            bufferHead = bufferTail = next;
        } else {
            // have something already
            bufferTail.linkNext(next);
            bufferTail = next;
        }
        int len = fullChunk.length;
        // 更新已经存储在Node中的对象数组的总大小
        bufferedEntryCount += len;
        // double the size for small chunks
        // 如果当前数组的大小小于最大块的大小，就创建一个当前数组一倍大小的数组返回
        // 否则增加25%的容量到新数组
        if (len < MAX_CHUNK_SIZE) {
            len += len;
        } else {
            // but by +25% for larger (to limit overhead)
            len += (len >> 2);
        }
        return new Object[len];
    }

    /**
     * 调用该方法以指示缓冲过程已完成； 并构造一个组合的精确大小的结果数组。
     * 此外，缓冲区本身将被重置以减少内存保留。
     * 结果数组将是通用的Object[]类型：如果需要类型化数组，请使用带有附加类型实参的方法。
     *
     * @param lastChunk        lastChunk
     * @param lastChunkEntries 接受的最大的Item的大小
     * @return 通用的Object[]类型
     */
    public Object[] completeAndClearBuffer(Object[] lastChunk, int lastChunkEntries) {
        // 新的结果数组应有大小：接受的最大的Item的大小和缓冲区中缓冲的条目总数的大小之和
        int totalSize = lastChunkEntries + bufferedEntryCount;
        Object[] result = new Object[totalSize];
        // 将bufferHead和lastChunk都复制到新的resultArray数组中
        copyTo(result, totalSize, lastChunk, lastChunkEntries);
        // 减少内存使用
        reset();
        return result;
    }

    /**
     * 将bufferHead和lastChunk都复制到新的resultArray数组中
     *
     * @param resultArray      新的结果数组
     * @param totalSize        新的结果数组应有大小
     * @param lastChunk        最新的Chunk：最后一次获取lastChunkEntries的块数据
     * @param lastChunkEntries 接受的最大的Item的大小
     */
    private final void copyTo(Object[] resultArray, int totalSize, Object[] lastChunk, int lastChunkEntries) {
        // 记录resultArray的总大小
        int ptr = 0;
        // 将bufferHead中的数据，复制到resultArray数组中
        for (Node n = bufferHead; n != null; n = n.next()) {
            Object[] curr = n.getData();
            int len = curr.length;
            // 复制指定的源数组的数组，在指定的位置开始，到目标数组的指定位置。
            // 将当前curr数组指定index的值复制到resultArray指定的index
            System.arraycopy(curr, 0, resultArray, ptr, len);
            ptr += len;
        }
        // 将lastChunk中的数据，也复制到resultArray数组中
        System.arraycopy(lastChunk, 0, resultArray, ptr, lastChunkEntries);
        ptr += lastChunkEntries;

        // 完整性检查：sanity check (could have failed earlier due to out-of-bounds, too)
        if (ptr != totalSize) {
            throw new IllegalStateException("Should have gotten " + totalSize + " entries, got " + ptr);
        }
    }

    /**
     * 用于检查此实例开始时有多少空闲容量。
     * 可用于根据缓冲区保存引用的可重用对象块的大小选择要重用的最佳实例。
     *
     * @return int
     */
    public int initialCapacity() {
        return (freeBuffer == null) ? 0 : freeBuffer.length;
    }

    /**
     * 用于检查已在此缓冲区中缓冲了多少对象。
     *
     * @return int
     */
    public int bufferedSize() {
        return bufferedEntryCount;
    }


    /**
     * 用于在链表中存储实际数据的Helper类。
     */
    private final static class Node {
        /**
         * 数据存储在此节点中。数组被认为已满。
         */
        private final Object[] data;

        private Node next;

        public Node(Object[] data) {
            this.data = data;
        }

        public Object[] getData() {
            return data;
        }

        public Node next() {
            return next;
        }

        public void linkNext(Node next) {
            // sanity check 完整性检查
            if (this.next != null) {
                throw new IllegalStateException();
            }
            this.next = next;
        }
    }

}
