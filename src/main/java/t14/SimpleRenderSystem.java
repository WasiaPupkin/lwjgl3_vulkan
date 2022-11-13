package t14;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPushConstantRange;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.Vector;

import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.VK12.VK_SHADER_STAGE_FRAGMENT_BIT;
import static org.lwjgl.vulkan.VK12.VK_SHADER_STAGE_VERTEX_BIT;
import static org.lwjgl.vulkan.VK12.VK_SUCCESS;
import static org.lwjgl.vulkan.VK12.vkCmdPushConstants;
import static org.lwjgl.vulkan.VK12.vkCreatePipelineLayout;
import static org.lwjgl.vulkan.VK12.vkDestroyPipeline;
import static util.VKUtil.modulo;
import static util.VKUtil.translateVulkanResult;

public class SimpleRenderSystem {
    public static String vertFilePath = "vulkan/shaders/t14/simple_shader.vert";
    public static String fragFilePath = "vulkan/shaders/t14/simple_shader.frag";
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

    public void renderGameObjects(VkCommandBuffer renderCommandBuffer, Vector<LveGameObject> gameObjects, LveCamera camera){

        int i = 0;
        for(LveGameObject obj : gameObjects) {
            obj.transform.rotation.y = modulo(obj.transform.rotation.y + 0.001f, (float) (2*Math.PI));
            obj.transform.rotation.x = modulo(obj.transform.rotation.x + 0.0001f, (float) (2*Math.PI));
        }

        // bind pipline
        lvePipeLine.bind(renderCommandBuffer);

        var projectionView = camera.getProjection().mul(camera.getView(), new Matrix4f());

        for(LveGameObject obj : gameObjects) {

//            Vector2f pushOffset = obj.transform.translation;
            Vector3f pushColor = obj.color;
            Matrix4f transform = projectionView.mul(obj.transform.mat4(), new Matrix4f());

//            float[] pushOffsetFl = {.0f, .0f, pushOffset.x(), pushOffset.y()}; // fixme this is pushOffsetFb + 2 first 0-s for memory alignment
//            FloatBuffer common = FloatBuffer.allocate(11).put(transformFb.array()).put(pushOffsetFl).put(pushColorFb.array());
            float[] transformFl = {transform.m00(), transform.m01(), transform.m02(), transform.m03(),
                    transform.m10(),transform.m11(),transform.m12(),transform.m13(),
                    transform.m20(),transform.m21(),transform.m22(),transform.m23(),
                    transform.m30(),transform.m31(),transform.m32(),transform.m33()};
            float[] pushColorFl = {pushColor.x(), pushColor.y(), pushColor.z()};

//            ByteBuffer vertexBuffer = memAlloc((transformFl.length + pushColorFl.length) * 4);
//            vertexBuffer.alignmentOffset(1, 8);
//            FloatBuffer hz = vertexBuffer.asFloatBuffer();
//            hz.put(transformFl).put(pushColorFl);

            FloatBuffer common = FloatBuffer.allocate(transformFl.length + pushColorFl.length).put(transformFl).put(pushColorFl);
//            memByteBuffer(address, capacity * 4).asFloatBuffer()
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
                .size(76) // fixme - should calculate - transformFb.capacity()+pushColorFb.capacity() * 4
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
