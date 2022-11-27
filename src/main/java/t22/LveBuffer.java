package t22;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkMappedMemoryRange;

import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.system.MemoryUtil.memCopy;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE;
import static org.lwjgl.vulkan.VK10.VK_WHOLE_SIZE;
import static org.lwjgl.vulkan.VK10.vkDestroyBuffer;
import static org.lwjgl.vulkan.VK10.vkFlushMappedMemoryRanges;
import static org.lwjgl.vulkan.VK10.vkFreeMemory;
import static org.lwjgl.vulkan.VK10.vkInvalidateMappedMemoryRanges;
import static org.lwjgl.vulkan.VK10.vkMapMemory;
import static org.lwjgl.vulkan.VK10.vkUnmapMemory;
import static util.VKUtil._CHECK_;

public class LveBuffer {
    private LveDevice lveDevice;
    private long mapped;
    private long buffer;
    private long memory;
    private long bufferSize;
    private int instanceCount;
    private long instanceSize;
    private long alignmentSize;
    private int usageFlags;
    private int memoryPropertyFlags;

    public LveBuffer(LveDevice lveDevice, long instanceSize, int instanceCount, int usageFlags, int memoryPropertyFlags, long minOffsetAlignment) {
//        long minOffsetAlignmentLocal = Math.max(minOffsetAlignment, 1);
//        var minOffsetAlignmentLocal = Math.min(
//                lveDevice.gpu_props.limits().minUniformBufferOffsetAlignment(),
//                lveDevice.gpu_props.limits().nonCoherentAtomSize()
//        );
        this.lveDevice = lveDevice;
        this.instanceSize = instanceSize;
        this.instanceCount = instanceCount;
        this.usageFlags = usageFlags;
        this.memoryPropertyFlags = memoryPropertyFlags;

        alignmentSize = getAlignment(instanceSize, minOffsetAlignment);
//        alignmentSize = instanceSize;
        bufferSize = alignmentSize * instanceCount;
        var bufferCallback = lveDevice.createBuffer(bufferSize, usageFlags, memoryPropertyFlags);
        buffer = bufferCallback.get("buffer");
        memory = bufferCallback.get("bufferMemory");
    }

    public void onDestroy(){
        unmap();
        vkDestroyBuffer(lveDevice.getDevice(), buffer, null);
        vkFreeMemory(lveDevice.getDevice(), memory, null);
    }

    /**
     * Map a memory range of this buffer. If successful, mapped points to the specified buffer range.
     *
     * @param size (Optional) Size of the memory range to map. Pass VK_WHOLE_SIZE to map the complete
     * buffer range.
     * @param offset (Optional) Byte offset from beginning
     *
     * @return VkResult of the buffer mapping call
     */
    public long map(long size, long offset ){
//        long sizeLocal = Math.min(size, VK_WHOLE_SIZE);
//        long offsetLocal = Math.max(offset, 0);+

        assert buffer>0 && memory>0 : "Called map on buffer before create";
        PointerBuffer pData = memAllocPointer(1);
        long data;
//        if (size == VK_WHOLE_SIZE) {
//            _CHECK_(vkMapMemory(lveDevice.getDevice(), memory, 0, bufferSize, 0, pData), "Failed to map buffer memory");
//            data = pData.get(0);
//            memFree(pData);
//
//            mapped = data;
//            return data;
//        }
        _CHECK_(vkMapMemory(lveDevice.getDevice(), memory, offset, size, 0, pData), "Failed to map buffer memory");
        data = pData.get(0);
        memFree(pData);

        mapped = data;
        return data;

    }

    /**
     * Unmap a mapped memory range
     *
     * @note Does not return a result as vkUnmapMemory can't fail
     */
    public void unmap() {
        if (mapped != 0) {
            vkUnmapMemory(lveDevice.getDevice(), memory);
            mapped = 0;
        }
    }

    /**
     * Copies the specified data to the mapped buffer. Default value writes whole buffer range
     *
     * @param data Pointer to the data to copy
     * @param size (Optional) Size of the data to copy. Pass VK_WHOLE_SIZE to flush the complete buffer
     * range.
     * @param offset (Optional) Byte offset from beginning of mapped region
     *
     */
    public void writeToBuffer(long data, long size, long offset) {
        assert mapped != 0 : "Cannot copy to unmapped buffer";

        if (size == VK_WHOLE_SIZE) {
            memCopy(data, mapped, bufferSize);
        } else {
            long memOffset = mapped;
            memOffset += offset;
            memCopy(data, memOffset, size);
        }
    }

    /**
     * Flush a memory range of the buffer to make it visible to the device
     *
     * @note Only required for non-coherent memory
     *
     * @param size (Optional) Size of the memory range to flush. Pass VK_WHOLE_SIZE to flush the
     * complete buffer range.
     * @param offset (Optional) Byte offset from beginning
     *
     * @return VkResult of the flush call
     */
    public int flush(long size, long offset) {
        VkMappedMemoryRange mappedRange = VkMappedMemoryRange.calloc()
                .sType(VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE)
                .memory(memory)
                .offset(offset)
                .size(size);

        return vkFlushMappedMemoryRanges(lveDevice.getDevice(), mappedRange);
    }

    /**
     * Create a buffer info descriptor
     *
     * @param size (Optional) Size of the memory range of the descriptor
     * @param offset (Optional) Byte offset from beginning
     *
     * @return VkDescriptorBufferInfo of specified offset and range
     */
    public VkDescriptorBufferInfo descriptorInfo(long size, long offset) {
        return VkDescriptorBufferInfo.create().set(buffer, offset, size);
    }

    /**
     * Invalidate a memory range of the buffer to make it visible to the host
     *
     * @note Only required for non-coherent memory
     *
     * @param size (Optional) Size of the memory range to invalidate. Pass VK_WHOLE_SIZE to invalidate
     * the complete buffer range.
     * @param offset (Optional) Byte offset from beginning
     *
     * @return VkResult of the invalidate call
     */
    public int invalidate(long size, long offset) {
        VkMappedMemoryRange mappedRange = VkMappedMemoryRange.calloc()
                .sType(VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE)
                .memory(memory)
                .offset(offset)
                .size(size);

        return vkInvalidateMappedMemoryRanges(lveDevice.getDevice(), mappedRange);
    }

    /**
     * Copies "instanceSize" bytes of data to the mapped buffer at an offset of index * alignmentSize
     *
     * @param data Pointer to the data to copy
     * @param index Used in offset calculation
     *
     */
    public synchronized void writeToIndex(long data, int index) {
        writeToBuffer(data, instanceSize, index * alignmentSize);
    }

    /**
     *  Flush the memory range at index * alignmentSize of the buffer to make it visible to the device
     *
     * @param index Used in offset calculation
     *
     */
    public long flushIndex(int index){
        return flush(alignmentSize, index * alignmentSize);
    }

    /**
     * Create a buffer info descriptor
     *
     * @param index Specifies the region given by index * alignmentSize
     *
     * @return VkDescriptorBufferInfo for instance at index
     */
    public VkDescriptorBufferInfo descriptorInfoForIndex(int index) {
        return descriptorInfo(alignmentSize, index * alignmentSize);
    }

    /**
     * Invalidate a memory range of the buffer to make it visible to the host
     *
     * @note Only required for non-coherent memory
     *
     * @param index Specifies the region to invalidate: index * alignmentSize
     *
     * @return VkResult of the invalidate call
     */
    public long invalidateIndex(int index) {
        return invalidate(alignmentSize, index * alignmentSize);
    }

    public long getBuffer(){
        return buffer;
    }

    public long getMappedMemory(){
        return mapped;
    }

    public int getInstanceCount(){
        return instanceCount;
    }

    public long getInstanceSize(){
        return instanceSize;
    }

    public long getAlignmentSize(){
        return instanceSize;
    }

    public long getUsageFlags(){
        return usageFlags;
    }

    public long getMemoryPropertyFlags(){
        return memoryPropertyFlags;
    }

    public long getBufferSize(){
        return bufferSize;
    }

    /**
     * Returns the minimum instance size required to be compatible with devices minOffsetAlignment
     *
     * @param instanceSize The size of an instance
     * @param minOffsetAlignment The minimum required alignment, in bytes, for the offset member (eg
     * minUniformBufferOffsetAlignment)
     *
     * @return VkResult of the buffer mapping call
     */
    private static long getAlignment(long instanceSize, long minOffsetAlignment) {
        if (minOffsetAlignment > 0) {
            return (instanceSize + minOffsetAlignment - 1) & -minOffsetAlignment;
        }
        return instanceSize;
    }
}
