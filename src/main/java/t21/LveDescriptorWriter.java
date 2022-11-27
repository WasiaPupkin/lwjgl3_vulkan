package t21;

import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.Vector;

import static org.lwjgl.vulkan.VK10.vkUpdateDescriptorSets;

// *************** Descriptor Writer *********************
public class LveDescriptorWriter {
    private final LveDescriptorSetLayout setLayout;
    private final LveDescriptorPool pool;
    private final Vector<VkWriteDescriptorSet> writes = new Vector<>();

    public LveDescriptorWriter(LveDescriptorSetLayout setLayout, LveDescriptorPool pool) {
        this.setLayout = setLayout;
        this.pool = pool;
    }

    public LveDescriptorWriter writeBuffer(int binding, VkDescriptorBufferInfo bufferInfo){
        assert setLayout.bindings.containsKey(binding) : "Layout does not contain specified binding";
        var bindingDescription = setLayout.bindings.get(binding);

        assert bindingDescription.descriptorCount() == 1 : "Binding single descriptor info, but binding expects multiple";

        VkDescriptorBufferInfo.Buffer bufferInfoBuffer = VkDescriptorBufferInfo.calloc(1).put(bufferInfo);
        bufferInfoBuffer.flip();

        VkWriteDescriptorSet write = VkWriteDescriptorSet.create()
                .sType$Default()
                .descriptorType(bindingDescription.descriptorType())
                .dstBinding(binding) // <- Binds this uniform buffer to binding point 0
                .pBufferInfo(bufferInfoBuffer)
                .descriptorCount(1);

        writes.add(write);
//        bufferInfoBuffer.free();
        return this;
    }

    public LveDescriptorWriter writeImage(int binding, VkDescriptorImageInfo imageInfo){
        assert setLayout.bindings.containsKey(binding) : "Layout does not contain specified binding";
        var bindingDescription = setLayout.bindings.get(binding);

        assert bindingDescription.descriptorCount() == 1 : "Binding single descriptor info, but binding expects multiple";

        VkDescriptorImageInfo.Buffer bufferInfoBuffer = VkDescriptorImageInfo.calloc(1).put(imageInfo);
        VkWriteDescriptorSet write = VkWriteDescriptorSet.create()
                .sType$Default()
                .descriptorType(bindingDescription.descriptorType())
                .dstBinding(binding) // <- Binds this uniform buffer to binding point 0
                .pImageInfo(bufferInfoBuffer)
                .descriptorCount(1);

        writes.add(write);
        bufferInfoBuffer.free();
        return this;
    }

    public long build() {
        long set = pool.allocateDescriptor(setLayout.getDescriptorSetLayout());
//        if (!success) {
//            return false;
//        }
        overwrite(set);
        return set;
    }

    public void overwrite(long set) {
        for (var write : writes) {
            write.dstSet(set);
        }

        VkWriteDescriptorSet.Buffer data = VkWriteDescriptorSet.calloc(writes.size());
        writes.forEach(data::put);
        data.flip();

        vkUpdateDescriptorSets(pool.lveDevice.getDevice(), data, null);
        data.free();
    }
}
