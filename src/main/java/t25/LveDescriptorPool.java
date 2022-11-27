package t25;

import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;

import java.nio.LongBuffer;
import java.util.Vector;

import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.VK10.vkAllocateDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkCreateDescriptorPool;
import static org.lwjgl.vulkan.VK10.vkDestroyDescriptorPool;
import static org.lwjgl.vulkan.VK10.vkFreeDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkResetDescriptorPool;
import static util.VKUtil._CHECK_;

public class LveDescriptorPool {
    // *************** Descriptor Pool Builder *********************
    public static class Builder {
        private LveDevice lveDevice;
        private final Vector<VkDescriptorPoolSize> poolSizes = new Vector<>();
        private int maxSets = 1000;
        private int poolFlags = 0;

        public Builder(LveDevice lveDevice) {
            this.lveDevice = lveDevice;

        }

        public Builder addPoolSize(int descriptorType, int count) {
            poolSizes.add(VkDescriptorPoolSize.create().set(descriptorType, count));
            return this;
        }

        public Builder setPoolFlags(int flags) {
            poolFlags = flags;
            return this;
        }

        public Builder setMaxSets(int count) {
            maxSets = count;
            return this;
        }

        public LveDescriptorPool build() {
            return new LveDescriptorPool(lveDevice, maxSets, poolFlags, poolSizes);
        }
    }
    // *************** Descriptor Pool *********************

    public LveDevice lveDevice;
    private long descriptorPool;

    public LveDescriptorPool(LveDevice lveDevice, int maxSets, int poolFlags, Vector<VkDescriptorPoolSize> poolSizes) {
        this.lveDevice = lveDevice;

        // We need to tell the API the number of max. requested descriptors per type
        VkDescriptorPoolSize.Buffer typeCounts = VkDescriptorPoolSize.calloc(poolSizes.size());
        poolSizes.forEach(typeCounts::put);
        typeCounts.flip();

        // Create the global descriptor pool
        // All descriptors used in this example are allocated from this pool
        VkDescriptorPoolCreateInfo descriptorPoolInfo = VkDescriptorPoolCreateInfo.calloc()
                .sType$Default()
                .pPoolSizes(typeCounts)
                // Set the max. number of sets that can be requested
                // Requesting descriptors beyond maxSets will result in an error
                .maxSets(maxSets)
                .flags(poolFlags);

        LongBuffer pDescriptorPool = memAllocLong(1);
        _CHECK_(vkCreateDescriptorPool(lveDevice.getDevice(), descriptorPoolInfo, null, pDescriptorPool), "Failed to create descriptor pool: ");
        descriptorPool = pDescriptorPool.get(0);
        memFree(pDescriptorPool);
        typeCounts.free();
        descriptorPoolInfo.free();

    }

    public void onDestroy() {
//        freeDescriptors();
        vkDestroyDescriptorPool(lveDevice.getDevice(), descriptorPool, null);
    }

    public long allocateDescriptor(long descriptorSetLayout) {
        LongBuffer pDescriptorSetLayout = memAllocLong(1).put(0, descriptorSetLayout);

        VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc()
                .sType$Default()
                .descriptorPool(descriptorPool)
                .pSetLayouts(pDescriptorSetLayout);

        LongBuffer pDescriptorSet = memAllocLong(1);
        _CHECK_(vkAllocateDescriptorSets(lveDevice.getDevice(), allocInfo, pDescriptorSet), "Failed to create descriptor set: ");
        long descriptorSet = pDescriptorSet.get(0);
        memFree(pDescriptorSet);
        memFree(pDescriptorSetLayout);
        allocInfo.free();

        return descriptorSet;
    }

    public void freeDescriptors(Vector<Long> descriptors) {
        long[] descriptorsArr = new long[descriptors.size()];
        for(int i=0; i<descriptors.size(); i++ ){
            descriptorsArr[i] = descriptors.get(i);
        }
        vkFreeDescriptorSets(lveDevice.getDevice(), descriptorPool, descriptorsArr);
    }

    public void resetPool() {
        vkResetDescriptorPool(lveDevice.getDevice(), descriptorPool, 0);
    }
}
