package t24.systems;

import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import t24.LveDevice;
import t24.LveFrameInfo;
import t24.LvePipeLine;

import java.io.IOException;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS;
import static org.lwjgl.vulkan.VK10.vkCmdBindDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkCmdDraw;
import static org.lwjgl.vulkan.VK10.vkDestroyPipelineLayout;
import static org.lwjgl.vulkan.VK12.vkCreatePipelineLayout;
import static util.VKUtil._CHECK_;

public class PointLightSystem {

    public static String vertFilePath = "vulkan/shaders/t24/point_light.vert";
    public static String fragFilePath = "vulkan/shaders/t24/point_light.frag";
    private final LveDevice lveDevice;
    private LvePipeLine lvePipeLine;
    private long pipelineLayout;

    public PointLightSystem(LveDevice lveDevice, long renderPass, long globalSetLayout) {
        this.lveDevice = lveDevice;

        createPipelineLayout(globalSetLayout);
        try {
            createPipeline(renderPass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//    fixme
    public void onDestroy() {
//        vkDestroyPipeline(lveDevice.getDevice(), lvePipeLine.graphicsPipeline, null);
        vkDestroyPipelineLayout(lveDevice.getDevice(), pipelineLayout, null);
        lvePipeLine.onDestroy();
    }

    public synchronized void render(LveFrameInfo frameInfo) {
        // bind pipline
        lvePipeLine.bind(frameInfo.commandBuffer());

//        fixme try to reuse memory
        LongBuffer globalDescriptorSet = memAllocLong(1).put(0, frameInfo.globalDescriptorSet());
        vkCmdBindDescriptorSets(frameInfo.commandBuffer(), VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0, globalDescriptorSet, null);
        memFree(globalDescriptorSet);

        vkCmdDraw(frameInfo.commandBuffer(), 6, 1, 0, 0);
    }

    /**
     * Create the pipeline layout that is used to generate the rendering pipelines that
     * are based on this descriptor set layout
     */
    private void createPipelineLayout(long globalSetLayout) {

//        VkPushConstantRange.Buffer pushConstantRange = VkPushConstantRange.calloc(1)
//                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT)
//                .offset(0)
//                .size(128) // fixme - should calculate - transformFb.capacity()+pushColorFb.capacity() * 4
//                ;

        LongBuffer pDescriptorSetLayout = memAllocLong(1).put(0, globalSetLayout);
        VkPipelineLayoutCreateInfo pipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.calloc()
                .sType$Default()
                .pSetLayouts(pDescriptorSetLayout)
//                .pPushConstantRanges(pushConstantRange)
                ;

        LongBuffer pPipelineLayout = memAllocLong(1);
        _CHECK_(vkCreatePipelineLayout(lveDevice.getDevice(), pipelineLayoutCreateInfo, null, pPipelineLayout), "Failed to create pipeline layout: ");

        pipelineLayout = pPipelineLayout.get(0);

        memFree(pPipelineLayout);
        memFree(pDescriptorSetLayout);
        pipelineLayoutCreateInfo.free();
//        pushConstantRange.free();
    }

    void createPipeline(long renderPass) throws IOException {
        assert renderPass != 0;
        assert pipelineLayout != 0;

//        update pipeline config
        LvePipeLine.PipelineConfigInfo pipelineConfig = new LvePipeLine.PipelineConfigInfo();
        LvePipeLine.defaultPipelineConfigInfo(pipelineConfig);
        pipelineConfig.attributeDescriptions.clear();
        pipelineConfig.bindingDescriptions.clear();
        pipelineConfig.renderPass = renderPass;
        pipelineConfig.pipelineLayout = pipelineLayout;

        lvePipeLine = new LvePipeLine(lveDevice, vertFilePath, fragFilePath, pipelineConfig);

    }
}
