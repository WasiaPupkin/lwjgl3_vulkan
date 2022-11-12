package t11;

import org.joml.Matrix2f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPushConstantRange;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.Vector;

import static util.VKUtil.modulo;
import static util.VKUtil.translateVulkanResult;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkCmdPushConstants;
import static org.lwjgl.vulkan.VK10.vkCreatePipelineLayout;
import static org.lwjgl.vulkan.VK10.vkDestroyPipeline;

public class SimpleRenderSystem {
    public static String vertFilePath = "vulkan/shaders/simple_shader_transform.vert";
    public static String fragFilePath = "vulkan/shaders/simple_shader_transform.frag";
    private final LveDevice lveDevice;
    private LvePipeLine lvePipeLine;
    private long pipelineLayout;

    public SimpleRenderSystem(LveDevice lveDevice, long renderPass) {
        this.lveDevice = lveDevice;

        createPipelineLayout();
        try {
            createPipeline(renderPass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//    fixme
    public void onDestroy() {
        vkDestroyPipeline(lveDevice.getDevice(), lvePipeLine.graphicsPipeline, null);
    }

    public void renderGameObjects(VkCommandBuffer renderCommandBuffer, Vector<LveGameObject> gameObjects){

        int i = 0;
        for(LveGameObject obj : gameObjects) {
            obj.transform2d.rotation = modulo(obj.transform2d.rotation - 0.001f * ++i, (float) (2*Math.PI));
        }

        // bind pipline
        lvePipeLine.bind(renderCommandBuffer);

        for(LveGameObject obj : gameObjects) {

            Vector2f pushOffset = obj.transform2d.translation;
            Vector3f pushColor = obj.color;
            Matrix2f transform = obj.transform2d.mat2();

            FloatBuffer pushOffsetFb = FloatBuffer.allocate(2).put(pushOffset.x()).put(pushOffset.y());
            FloatBuffer pushColorFb = FloatBuffer.allocate(3).put(pushColor.x()).put(pushColor.y()).put(pushColor.z());
            FloatBuffer transformFb = FloatBuffer.allocate(4).put(transform.m00()).put(transform.m01()).put(transform.m10()).put(transform.m11());
            float[] pushOffsetFl = {.0f, .0f, pushOffset.x(), pushOffset.y()}; // fixme this is pushOffsetFb + 2 first 0-s for memory alignment
            FloatBuffer common = FloatBuffer.allocate(11).put(transformFb.array()).put(pushOffsetFl).put(pushColorFb.array());
            vkCmdPushConstants(renderCommandBuffer, pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT, 0, common.array());
            obj.model.bind(renderCommandBuffer);
            obj.model.draw(renderCommandBuffer);

        }
    }

    /**
     * Create the pipeline layout that is used to generate the rendering pipelines that
     * are based on this descriptor set layout
     */
    private void createPipelineLayout() {

        VkPushConstantRange.Buffer pushConstantRange = VkPushConstantRange.calloc(1)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT)
                .offset(0)
                .size(44) // fixme - should calculate
                ;

//        LongBuffer pDescriptorSetLayout = memAllocLong(1).put(0, lveDevice.getDescriptorSetLayout());
        VkPipelineLayoutCreateInfo pipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.calloc()
                .sType$Default()
//                .pSetLayouts(pDescriptorSetLayout)
                .pPushConstantRanges(pushConstantRange)
                ;

        LongBuffer pPipelineLayout = memAllocLong(1);
        int err = vkCreatePipelineLayout(lveDevice.getDevice(), pipelineLayoutCreateInfo, null, pPipelineLayout);

        pipelineLayout = pPipelineLayout.get(0);

        memFree(pPipelineLayout);
        pipelineLayoutCreateInfo.free();
//        memFree(pDescriptorSetLayout);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create pipeline layout: " + translateVulkanResult(err));
        }

        pushConstantRange.free();
    }

    void createPipeline(long renderPass) throws IOException {
        assert renderPass != 0;
        assert pipelineLayout != 0;

//        update pipeline config
        LvePipeLine.PipelineConfigInfo pipelineConfig = new LvePipeLine.PipelineConfigInfo();
        LvePipeLine.defaultPipelineConfigInfo(pipelineConfig);
        pipelineConfig.renderPass = renderPass;
        pipelineConfig.pipelineLayout = pipelineLayout;

        lvePipeLine = new LvePipeLine(lveDevice, vertFilePath, fragFilePath, pipelineConfig);

    }
}
