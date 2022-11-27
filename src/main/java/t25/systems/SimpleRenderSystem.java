package t25.systems;

import org.joml.Matrix4f;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPushConstantRange;
import t25.LveDevice;
import t25.LveFrameInfo;
import t25.LveGameObject;
import t25.LvePipeLine;

import java.io.IOException;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS;
import static org.lwjgl.vulkan.VK10.vkCmdBindDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkDestroyPipelineLayout;
import static org.lwjgl.vulkan.VK12.VK_SHADER_STAGE_FRAGMENT_BIT;
import static org.lwjgl.vulkan.VK12.VK_SHADER_STAGE_VERTEX_BIT;
import static org.lwjgl.vulkan.VK12.vkCmdPushConstants;
import static org.lwjgl.vulkan.VK12.vkCreatePipelineLayout;
import static util.VKUtil._CHECK_;

public class SimpleRenderSystem {
    public class SimplePushConstantData {
        public Matrix4f modelMatrix = new Matrix4f();
        public Matrix4f normalMatrix = new Matrix4f();
    }

    public static String vertFilePath = "vulkan/shaders/t25/simple_shader.vert";
    public static String fragFilePath = "vulkan/shaders/t25/simple_shader.frag";
    private final LveDevice lveDevice;
    private LvePipeLine lvePipeLine;
    private long pipelineLayout;

    public SimpleRenderSystem(LveDevice lveDevice, long renderPass, long globalSetLayout) {
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

    public synchronized void renderGameObjects(LveFrameInfo frameInfo) {
        // bind pipline
        lvePipeLine.bind(frameInfo.commandBuffer());

        LongBuffer globalDescriptorSet = memAllocLong(1).put(0, frameInfo.globalDescriptorSet());
        vkCmdBindDescriptorSets(frameInfo.commandBuffer(), VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0, globalDescriptorSet, null);
        memFree(globalDescriptorSet);

        for(LveGameObject obj : frameInfo.gameObjects().values()) {
            if (obj.model == null) continue;
            SimplePushConstantData push = new SimplePushConstantData();
            push.modelMatrix  = obj.transform.mat4();
            push.normalMatrix = new Matrix4f(obj.transform.normalMatrix());

            vkCmdPushConstants(frameInfo.commandBuffer(), pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT, 0, pushDataToFloatArr(push));
            obj.model.bind(frameInfo.commandBuffer());
            obj.model.draw(frameInfo.commandBuffer());
        }
    }

//    private FloatBuffer pushDataToFloatBuffer(SimplePushConstantData push) {
//        float[] transformFl = {push.transform.m00(), push.transform.m01(), push.transform.m02(), push.transform.m03(),
//                push.transform.m10(),push.transform.m11(),push.transform.m12(),push.transform.m13(),
//                push.transform.m20(),push.transform.m21(),push.transform.m22(),push.transform.m23(),
//                push.transform.m30(),push.transform.m31(),push.transform.m32(),push.transform.m33()};
//        float[] modelMatrixFl = {push.normalMatrix.m00(), push.normalMatrix.m01(), push.normalMatrix.m02(), push.normalMatrix.m03(),
//                push.normalMatrix.m10(),push.normalMatrix.m11(),push.normalMatrix.m12(),push.normalMatrix.m13(),
//                push.normalMatrix.m20(),push.normalMatrix.m21(),push.normalMatrix.m22(),push.normalMatrix.m23(),
//                push.normalMatrix.m30(),push.normalMatrix.m31(),push.normalMatrix.m32(),push.normalMatrix.m33()};
//
//        return FloatBuffer.allocate(transformFl.length + modelMatrixFl.length).put(transformFl).put(modelMatrixFl);
//    }

    private float[] pushDataToFloatArr(SimplePushConstantData push) {
        return new float[] {push.modelMatrix.m00(), push.modelMatrix.m01(), push.modelMatrix.m02(), push.modelMatrix.m03(),
                push.modelMatrix.m10(),push.modelMatrix.m11(),push.modelMatrix.m12(),push.modelMatrix.m13(),
                push.modelMatrix.m20(),push.modelMatrix.m21(),push.modelMatrix.m22(),push.modelMatrix.m23(),
                push.modelMatrix.m30(),push.modelMatrix.m31(),push.modelMatrix.m32(),push.modelMatrix.m33(),
                push.normalMatrix.m00(), push.normalMatrix.m01(), push.normalMatrix.m02(), push.normalMatrix.m03(),
                push.normalMatrix.m10(),push.normalMatrix.m11(),push.normalMatrix.m12(),push.normalMatrix.m13(),
                push.normalMatrix.m20(),push.normalMatrix.m21(),push.normalMatrix.m22(),push.normalMatrix.m23(),
                push.normalMatrix.m30(),push.normalMatrix.m31(),push.normalMatrix.m32(),push.normalMatrix.m33()};
    }

    /**
     * Create the pipeline layout that is used to generate the rendering pipelines that
     * are based on this descriptor set layout
     */
    private void createPipelineLayout(long globalSetLayout) {

        VkPushConstantRange.Buffer pushConstantRange = VkPushConstantRange.calloc(1)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT)
                .offset(0)
                .size(128) // fixme - should calculate - transformFb.capacity()+pushColorFb.capacity() * 4
                ;

        LongBuffer pDescriptorSetLayout = memAllocLong(1).put(0, globalSetLayout);
        VkPipelineLayoutCreateInfo pipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.calloc()
                .sType$Default()
                .pSetLayouts(pDescriptorSetLayout)
                .pPushConstantRanges(pushConstantRange)
                ;

        LongBuffer pPipelineLayout = memAllocLong(1);
        _CHECK_(vkCreatePipelineLayout(lveDevice.getDevice(), pipelineLayoutCreateInfo, null, pPipelineLayout), "Failed to create pipeline layout: ");

        pipelineLayout = pPipelineLayout.get(0);

        memFree(pPipelineLayout);
        memFree(pDescriptorSetLayout);
        pipelineLayoutCreateInfo.free();
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
