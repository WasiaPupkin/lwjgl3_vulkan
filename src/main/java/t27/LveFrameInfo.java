package t27;

import org.lwjgl.vulkan.VkCommandBuffer;

import java.util.Map;

public record LveFrameInfo (
        int frameIndex,
        float frameTime,
        VkCommandBuffer commandBuffer,
        LveCamera camera,
        long globalDescriptorSet,
        Map<Integer, LveGameObject> gameObjects
){}
