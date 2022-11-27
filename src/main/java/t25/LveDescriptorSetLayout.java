package t25;

import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;

import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.VK10.vkCreateDescriptorSetLayout;
import static org.lwjgl.vulkan.VK10.vkDestroyDescriptorSetLayout;
import static util.VKUtil._CHECK_;

public class LveDescriptorSetLayout {
    // *************** Descriptor Set Layout Builder *********************
    public static class Builder {
        private final LveDevice lveDevice;
        private final Map<Integer, VkDescriptorSetLayoutBinding> bindings = new HashMap<>();
        public Builder(LveDevice lveDevice) {
            this.lveDevice = lveDevice;
        }

        public Builder addBinding(int binding, int descriptorType, int stageFlags, int count) {
            int countLocal = Math.max(count, 1);
            assert !bindings.containsKey(binding) : "Binding already in use";

            VkDescriptorSetLayoutBinding layoutBinding = VkDescriptorSetLayoutBinding.create()
                    .binding(binding) // <- Binding 0 : Uniform buffer (Vertex shader)
                    .descriptorType(descriptorType)
                    .descriptorCount(countLocal)
                    .stageFlags(stageFlags);

            bindings.put(binding, layoutBinding);
            return this;
        }

        public LveDescriptorSetLayout build() {
            return new LveDescriptorSetLayout(lveDevice, bindings);
        }
    }

    // *************** Descriptor Set Layout *********************

    private final LveDevice lveDevice;
    private final long descriptorSetLayout;
    public Map<Integer, VkDescriptorSetLayoutBinding> bindings;

    public LveDescriptorSetLayout(LveDevice lveDevice, Map<Integer, VkDescriptorSetLayoutBinding> bindings) {
        this.lveDevice = lveDevice;
        this.bindings = bindings;

        var bindingsVal = bindings.values();
        VkDescriptorSetLayoutBinding.Buffer setLayoutBindings = VkDescriptorSetLayoutBinding.calloc(bindingsVal.size());
        bindingsVal.forEach(setLayoutBindings::put);
        setLayoutBindings.flip();

        // Build a create-info struct to create the descriptor set layout
        VkDescriptorSetLayoutCreateInfo descriptorSetLayoutInfo = VkDescriptorSetLayoutCreateInfo.calloc()
                .sType$Default()
                .pBindings(setLayoutBindings);

        LongBuffer pDescriptorSetLayout = memAllocLong(1);
        _CHECK_(vkCreateDescriptorSetLayout(lveDevice.getDevice(), descriptorSetLayoutInfo, null, pDescriptorSetLayout), "Failed to create descriptor set layout: ");
        descriptorSetLayout = pDescriptorSetLayout.get(0);
        memFree(pDescriptorSetLayout);
        memFree(setLayoutBindings);
//        setLayoutBindings.free();
        descriptorSetLayoutInfo.free();
    }

    public void onDestroy() {
        vkDestroyDescriptorSetLayout(lveDevice.getDevice(), descriptorSetLayout, null);
    }

    public long getDescriptorSetLayout() { return descriptorSetLayout; }
}
