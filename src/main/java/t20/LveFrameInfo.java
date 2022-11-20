package t20;

import org.lwjgl.vulkan.VkCommandBuffer;

record LveFrameInfo (
        int frameIndex,
        float frameTime,
        VkCommandBuffer commandBuffer,
        LveCamera camera,
        long globalDescriptorSet
){}
